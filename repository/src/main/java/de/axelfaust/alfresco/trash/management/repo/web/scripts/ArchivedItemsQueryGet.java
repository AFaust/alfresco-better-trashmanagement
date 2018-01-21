/*
 * Copyright 2017, 2018 Axel Faust
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.Operator;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.PropertyCheck;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ArchivedItemsQueryGet extends AbstractArchivedItemsRetrievalWebScript
{

    private static final String PARAM_BASE_STORE = "baseStore";

    private static final String PARAM_ARCHIVED_BY_USER = "archivedByUser";

    private static final String PARAM_TOP_LEVEL = "topLevel";

    private static final String PARAM_NAME_FILTER = "name";

    private static final String PARAM_FILTER_QUERY = "filterQuery";

    private static final String PARAM_FILTER_QUERY_DEFAULT_OPERATOR = "defaultOperator";

    private static final String PARAM_FILTER_QUERY_DEFAULT_TEMPLATE = "defaultQueryTemplate";

    private static final String PARAM_FILTER_ARCHIVE_DATE_FROM = "archiveDateFrom";

    private static final String PARAM_FILTER_ARCHIVE_DATE_TO = "archiveDateTo";

    protected NamespaceService namespaceService;

    protected NodeArchiveService nodeArchiveService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "nodeArchiveService", this.nodeArchiveService);
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param nodeArchiveService
     *            the nodeArchiveService to set
     */
    public void setNodeArchiveService(final NodeArchiveService nodeArchiveService)
    {
        this.nodeArchiveService = nodeArchiveService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache)
    {
        final String baseStoreParam = this.getParameter(req, PARAM_BASE_STORE);
        final StoreRef baseStore = baseStoreParam != null && !baseStoreParam.isEmpty() ? new StoreRef(baseStoreParam)
                : StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;

        final NodeRef storeArchiveNode = this.nodeArchiveService.getStoreArchiveNode(baseStore);
        final Map<String, Object> model = this.executeImpl(req, storeArchiveNode, status, cache);
        return model;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected SearchParameters prepareSearchParameters(final WebScriptRequest req, final NodeRef archiveContextNode, final int pageSize,
            final int startIndex)
    {
        final String archivedByUserParam = this.getParameter(req, PARAM_ARCHIVED_BY_USER);
        final String topLevelParam = this.getParameter(req, PARAM_TOP_LEVEL);
        final boolean topLevel = topLevelParam != null && !topLevelParam.isEmpty() ? Boolean.parseBoolean(topLevelParam) : true;

        final String filterNameParam = this.getParameter(req, PARAM_NAME_FILTER);

        final String filterQueryParam = this.getParameter(req, PARAM_FILTER_QUERY);
        final String defaultOperator = this.getParameter(req, PARAM_FILTER_QUERY_DEFAULT_OPERATOR);
        final String defaultQueryTemplate = this.getParameter(req, PARAM_FILTER_QUERY_DEFAULT_TEMPLATE);

        final String filterArchiveDateFromParam = this.getParameter(req, PARAM_FILTER_ARCHIVE_DATE_FROM);
        final String filterArchiveDateToParam = this.getParameter(req, PARAM_FILTER_ARCHIVE_DATE_TO);

        final SearchParameters sp = new SearchParameters();
        sp.addStore(archiveContextNode.getStoreRef());
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        // TODO Switch to TRANSACTIONAL_IF_POSSIBLE when we can handle paginated topLevel queries for large archived elements counts in
        // an efficient manner
        sp.setQueryConsistency(QueryConsistency.EVENTUAL);

        final StringBuilder queryBuilder = new StringBuilder();
        if (topLevel)
        {
            if (archivedByUserParam != null)
            {
                queryBuilder.append('=').append(ContentModel.PROP_ARCHIVED_BY.toPrefixString(this.namespaceService)).append(":\"")
                        .append(archivedByUserParam).append('"').append(" AND ");
            }
            queryBuilder.append("ASPECT:\"").append(ContentModel.ASPECT_ARCHIVED.toPrefixString(this.namespaceService)).append('"');
        }
        else if (archivedByUserParam != null)
        {
            final NodeRef archiveUserNode = AuthenticationUtil.runAsSystem(() -> {
                final List<ChildAssociationRef> archiveUserAssocs = this.nodeService.getChildrenByName(archiveContextNode,
                        ContentModel.ASSOC_ARCHIVE_USER_LINK, Collections.singletonList(archivedByUserParam));
                final NodeRef archiveUser = archiveUserAssocs.isEmpty() ? archiveUserAssocs.get(0).getChildRef() : null;
                return archiveUser;
            });
            queryBuilder.append("ANCESTOR:\"").append(archiveUserNode).append('"');
        }
        else
        {
            queryBuilder.append("NOT TYPE:\"").append(ContentModel.TYPE_ARCHIVE_USER.toPrefixString(this.namespaceService)).append('"');
        }
        sp.setQuery(queryBuilder.toString());

        if (filterNameParam != null && !filterNameParam.isEmpty())
        {
            queryBuilder.delete(0, queryBuilder.length());
            queryBuilder.append('=').append(ContentModel.PROP_NAME.toPrefixString(this.namespaceService)).append(":\"");
            if (!filterNameParam.startsWith("*"))
            {
                queryBuilder.append('*');
            }
            queryBuilder.append(filterNameParam.replace("\\", "\\\\").replace("\"", "\\\""));
            if (!filterNameParam.endsWith("*"))
            {
                queryBuilder.append('*');
            }
            queryBuilder.append('"');
            sp.addFilterQuery(queryBuilder.toString());
        }

        if (filterQueryParam != null && !filterQueryParam.isEmpty())
        {
            sp.addFilterQuery(filterQueryParam);
        }

        if (defaultOperator != null && !defaultOperator.isEmpty())
        {
            sp.setDefaultOperator(Operator.valueOf(defaultOperator.toUpperCase(Locale.ENGLISH)));
        }

        sp.setDefaultFieldName("keywords");
        if (defaultQueryTemplate != null && !defaultQueryTemplate.isEmpty())
        {
            sp.addQueryTemplate("keywords", defaultQueryTemplate);
        }
        else
        {
            sp.addQueryTemplate("keywords",
                    "%(cm:name cm:title cm:description ia:whatEvent ia:descriptionEvent lnk:title lnk:description TEXT TAG)");
        }

        if ((filterArchiveDateFromParam != null && !filterArchiveDateFromParam.isEmpty())
                || (filterArchiveDateToParam != null && !filterArchiveDateToParam.isEmpty()))
        {
            // TODO Add custom (virtual?) property to handle archive date filtering on any level, not just the root archived items
            final StringBuilder archiveDateFilterBuilder = new StringBuilder();
            archiveDateFilterBuilder.append(ContentModel.PROP_ARCHIVED_DATE.toPrefixString(this.namespaceService)).append(":[");
            archiveDateFilterBuilder.append(
                    filterArchiveDateFromParam != null && !filterArchiveDateFromParam.isEmpty() ? filterArchiveDateFromParam : "MIN");
            archiveDateFilterBuilder.append(" TO ");
            archiveDateFilterBuilder
                    .append(filterArchiveDateToParam != null && !filterArchiveDateToParam.isEmpty() ? filterArchiveDateToParam : "NOW");
            archiveDateFilterBuilder.append(']');
            sp.addFilterQuery(archiveDateFilterBuilder.toString());
        }

        sp.setSkipCount(startIndex);
        sp.setLimit(pageSize);
        sp.setMaxItems(pageSize);
        return sp;
    }
}
