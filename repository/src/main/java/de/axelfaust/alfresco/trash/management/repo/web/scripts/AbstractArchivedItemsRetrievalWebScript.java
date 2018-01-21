/*
 * Copyright 2018 Axel Faust
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.axelfaust.alfresco.trash.management.repo.web.scripts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public abstract class AbstractArchivedItemsRetrievalWebScript extends DeclarativeWebScript implements InitializingBean
{

    // include potential tenant name suffix
    private static final String PATTERN_SYSTEM_USER_NAME = "^" + AuthenticationUtil.getSystemUserName() + "(@.+)?$";

    private static final String PARAM_PAGE_SIZE = "pageSize";

    private static final String PARAM_PAGE = "page";

    private static final String PARAM_START_INDEX = "startIndex";

    private static final String RESPONSE_PAGINATION = "pagination";

    private static final String RESPONSE_RESULTS = "results";

    private static final String RESPONSE_TOTAL_RECORDS = "totalRecords";

    private static final String RESPONSE_NUMBER_FOUND = "numberFound";

    private static final String RESPONSE_USER_NAME = "userName";

    private static final String RESPONSE_USER_DISPLAY_NAME = "displayName";

    private static final String RESPONSE_USER_FIRST_NAME = "firstName";

    private static final String RESPONSE_USER_LAST_NAME = "lastName";

    protected NodeService nodeService;

    protected PersonService personService;

    protected PermissionService permissionService;

    protected SearchService searchService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "personService", this.personService);
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);
        PropertyCheck.mandatory(this, "searchService", this.searchService);
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param personService
     *            the personService to set
     */
    public void setPersonService(final PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * @param permissionService
     *            the permissionService to set
     */
    public void setPermissionService(final PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    /**
     * @param searchService
     *            the searchService to set
     */
    public void setSearchService(final SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * {@inheritDoc}
     */
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final NodeRef storeArchiveNode, final Status status,
            final Cache cache)
    {
        final Map<String, Object> model = new HashMap<>();

        final String startIndexParam = this.getParameter(req, PARAM_START_INDEX);
        final String pageParam = this.getParameter(req, PARAM_PAGE);
        final String pageSizeParam = this.getParameter(req, PARAM_PAGE_SIZE);

        final int pageSize = pageSizeParam != null && !pageSizeParam.isEmpty() ? Integer.parseInt(pageSizeParam, 10) : 50;
        int startIndex = startIndexParam != null && !startIndexParam.isEmpty() ? Integer.parseInt(startIndexParam, 10) : -1;
        if (startIndex < 0 && pageParam != null)
        {
            final int page = Integer.parseInt(pageParam, 10);
            startIndex = page * pageSize + 1;
        }

        if (startIndex < 0)
        {
            startIndex = 0;
        }

        final Map<String, Object> paginationModel = new HashMap<>();
        model.put(RESPONSE_PAGINATION, paginationModel);

        paginationModel.put(PARAM_START_INDEX, Integer.valueOf(startIndex));
        paginationModel.put(RESPONSE_TOTAL_RECORDS, Integer.valueOf(0));
        paginationModel.put(RESPONSE_NUMBER_FOUND, Integer.valueOf(0));

        if (storeArchiveNode != null)
        {
            final SearchParameters sp = this.prepareSearchParameters(req, storeArchiveNode, pageSize, startIndex);

            final ResultSet resultSet = this.searchService.query(sp);
            try
            {
                final List<Map<String, Object>> results = this.processResults(resultSet);
                model.put(RESPONSE_RESULTS, results);
                paginationModel.put(RESPONSE_TOTAL_RECORDS, Integer.valueOf(resultSet.length()));
                paginationModel.put(RESPONSE_NUMBER_FOUND, Long.valueOf(resultSet.getNumberFound()));
            }
            finally
            {
                resultSet.close();
            }
        }
        else
        {
            model.put("results", new ArrayList<Map<String, Object>>());
        }

        return model;
    }

    abstract protected SearchParameters prepareSearchParameters(final WebScriptRequest req, final NodeRef archiveContextNode,
            final int pageSize, final int startIndex);

    protected List<Map<String, Object>> processResults(final ResultSet resultSet)
    {
        final List<Map<String, Object>> results = new ArrayList<>();
        final Map<String, Map<String, Object>> userObjByUserName = new HashMap<>();

        resultSet.getNodeRefs().forEach((result) -> {
            final Map<String, Object> itemObj = this.buildResultEntry(result, userObjByUserName);
            results.add(itemObj);
        });
        return results;
    }

    protected Map<String, Object> buildResultEntry(final NodeRef result, final Map<String, Map<String, Object>> userObjByUserName)
    {
        final Map<QName, Serializable> resultProperties = this.nodeService.getProperties(result);

        final String modifier = DefaultTypeConverter.INSTANCE.convert(String.class, resultProperties.get(ContentModel.PROP_MODIFIER));
        String archiver = DefaultTypeConverter.INSTANCE.convert(String.class, resultProperties.get(ContentModel.PROP_ARCHIVED_BY));
        Date archivedOn = DefaultTypeConverter.INSTANCE.convert(Date.class, resultProperties.get(ContentModel.PROP_ARCHIVED_DATE));
        ChildAssociationRef originalParentAssoc = DefaultTypeConverter.INSTANCE.convert(ChildAssociationRef.class,
                resultProperties.get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC));

        final StringBuilder displayPathBuilder = new StringBuilder(1024);

        NodeRef previousNode = result;
        while (archiver == null && previousNode != null)
        {
            final ChildAssociationRef primaryParent = this.nodeService.getPrimaryParent(previousNode);
            final NodeRef parentRef;
            if (primaryParent != null)
            {
                parentRef = primaryParent.getParentRef();
                final AccessStatus parentReadAccess = this.permissionService.hasPermission(parentRef, PermissionService.READ);

                final Map<QName, Serializable> archivedItemProperties;
                final String parentName;

                if (parentReadAccess == AccessStatus.ALLOWED)
                {
                    archivedItemProperties = this.nodeService.getProperties(parentRef);
                    parentName = DefaultTypeConverter.INSTANCE.convert(String.class, archivedItemProperties.get(ContentModel.PROP_NAME));
                }
                else
                {
                    archivedItemProperties = AuthenticationUtil.runAsSystem(() -> {
                        final Map<QName, Serializable> properties;
                        if (this.nodeService.hasAspect(parentRef, ContentModel.ASPECT_ARCHIVED))
                        {
                            properties = this.nodeService.getProperties(parentRef);
                        }
                        else
                        {
                            properties = Collections.emptyMap();
                        }
                        return properties;
                    });

                    parentName = primaryParent.getQName().getLocalName();
                }

                archiver = DefaultTypeConverter.INSTANCE.convert(String.class, archivedItemProperties.get(ContentModel.PROP_ARCHIVED_BY));
                archivedOn = DefaultTypeConverter.INSTANCE.convert(Date.class, archivedItemProperties.get(ContentModel.PROP_ARCHIVED_DATE));
                originalParentAssoc = DefaultTypeConverter.INSTANCE.convert(ChildAssociationRef.class,
                        archivedItemProperties.get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC));

                displayPathBuilder.insert(0, parentName);
                displayPathBuilder.insert(0, '/');
            }
            else
            {
                parentRef = null;
            }
            previousNode = parentRef;
        }

        Map<String, Object> modifierObj = userObjByUserName.get(modifier);
        if (modifierObj == null)
        {
            modifierObj = this.buildUserObject(modifier);
            userObjByUserName.put(modifier, modifierObj);
        }

        Map<String, Object> archiverObj = userObjByUserName.get(archiver);
        if (archiverObj == null)
        {
            archiverObj = this.buildUserObject(modifier);
            userObjByUserName.put(archiver, archiverObj);
        }

        final NodeRef originalParent = originalParentAssoc != null ? originalParentAssoc.getParentRef() : null;
        if (originalParent != null && this.nodeService.exists(originalParent))
        {
            final Map<QName, Serializable> parentProperties = this.nodeService.getProperties(originalParent);
            final String parentName = DefaultTypeConverter.INSTANCE.convert(String.class, parentProperties.get(ContentModel.PROP_NAME));
            displayPathBuilder.insert(0, parentName);
            displayPathBuilder.insert(0, '/');

            final String parentPath = this.nodeService.getPath(originalParent).toDisplayPath(this.nodeService, this.permissionService);
            displayPathBuilder.insert(0, parentPath);
        }
        else
        {
            // parent no longer exists, so we include marker to avoid misunderstanding relative path with full path
            displayPathBuilder.insert(0, "?");
        }

        final Map<String, Object> itemObj = new HashMap<>();
        itemObj.put("modifier", modifierObj);
        itemObj.put("archiver", archiverObj);
        itemObj.put("archivedOn", archivedOn);
        itemObj.put("node", result);
        itemObj.put("displayPath", displayPathBuilder.toString());
        return itemObj;
    }

    protected Map<String, Object> buildUserObject(final String user)
    {
        Map<String, Object> modifierObj;
        modifierObj = new HashMap<>();
        modifierObj.put(RESPONSE_USER_NAME, user);

        final NodeRef person = this.personService.getPerson(user, false);

        if (person != null)
        {
            final Map<QName, Serializable> personProperties = this.nodeService.getProperties(person);
            final String firstName = DefaultTypeConverter.INSTANCE.convert(String.class, personProperties.get(ContentModel.PROP_FIRSTNAME));
            final String lastName = DefaultTypeConverter.INSTANCE.convert(String.class, personProperties.get(ContentModel.PROP_LASTNAME));

            modifierObj.put(RESPONSE_USER_FIRST_NAME, firstName != null ? firstName.trim() : "");
            modifierObj.put(RESPONSE_USER_LAST_NAME, lastName != null ? lastName.trim() : "");
            final StringBuilder displayNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty())
            {
                displayNameBuilder.append(firstName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty())
            {
                if (firstName != null && !firstName.trim().isEmpty())
                {
                    displayNameBuilder.append(' ');
                }
                displayNameBuilder.append(lastName.trim());
            }
            if (displayNameBuilder.length() == 0)
            {
                displayNameBuilder.append(user);
            }
            modifierObj.put(RESPONSE_USER_DISPLAY_NAME, displayNameBuilder.toString());
        }
        else if (user.matches(PATTERN_SYSTEM_USER_NAME))
        {
            modifierObj.put(RESPONSE_USER_FIRST_NAME, "System");
            modifierObj.put(RESPONSE_USER_LAST_NAME, "User");
            modifierObj.put(RESPONSE_USER_DISPLAY_NAME, "System User");
        }
        else
        {
            modifierObj.put(RESPONSE_USER_DISPLAY_NAME, user);
        }
        return modifierObj;
    }

    protected String getParameter(final WebScriptRequest req, final String parameterName)
    {
        String value = req.getParameter(parameterName);
        value = value != null ? value.trim() : null;
        return value;
    }
}
