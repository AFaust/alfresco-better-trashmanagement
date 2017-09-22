/*
 * Copyright 2017 Axel Faust
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ArchivedItemsQueryGet extends DeclarativeWebScript implements InitializingBean
{

    protected NamespaceService namespaceService;

    protected SearchService searchService;

    protected NodeService nodeService;

    protected NodeArchiveService nodeArchiveService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "searchService", this.searchService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
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
     * @param searchService
     *            the searchService to set
     */
    public void setSearchService(final SearchService searchService)
    {
        this.searchService = searchService;
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
        final Map<String, Object> model = new HashMap<>();

        final String baseStoreParam = req.getParameter("baseStore");
        final String archivedByUserParam = req.getParameter("archivedByUser");
        final String topLevelParam = req.getParameter("topLevel");

        final String startIndexParam = req.getParameter("startIndex");
        final String pageParam = req.getParameter("page");
        final String pageSizeParam = req.getParameter("pageSize");

        final StoreRef baseStore = baseStoreParam != null ? new StoreRef(baseStoreParam) : StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        final String archivedByUser = archivedByUserParam != null ? archivedByUserParam : AuthenticationUtil.getRunAsUser();

        final boolean topLevel = topLevelParam != null ? Boolean.parseBoolean(topLevelParam) : true;

        final int pageSize = pageSizeParam != null ? Integer.parseInt(pageSizeParam, 10) : 50;
        int startIndex = startIndexParam != null ? Integer.parseInt(startIndexParam, 10) : -1;
        if (startIndex < 0 && pageParam != null)
        {
            final int page = Integer.parseInt(pageParam, 10);
            startIndex = page * pageSize + 1;
        }

        if (startIndex < 0)
        {
            startIndex = 0;
        }

        final String filterQueryParam = req.getParameter("filterQuery");
        final String nameForSearchParam = req.getParameter("name");

        final NodeRef storeArchiveNode = this.nodeArchiveService.getStoreArchiveNode(baseStore);
        final Map<String, Object> paginationModel = new HashMap<>();
        model.put("pagination", paginationModel);

        paginationModel.put("startIndex", Integer.valueOf(startIndex));
        paginationModel.put("totalRecords", Integer.valueOf(0));
        paginationModel.put("numberFound", Integer.valueOf(0));

        if (storeArchiveNode != null)
        {
            final SearchParameters sp = new SearchParameters();
            sp.addStore(storeArchiveNode.getStoreRef());
            sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
            // TODO Switch to TRANSACTIONAL_IF_POSSIBLE when we can handle paginated topLevel queries for large archived elements counts in
            // an efficient manner
            sp.setQueryConsistency(QueryConsistency.EVENTUAL);

            final StringBuilder queryBuilder = new StringBuilder();
            if (topLevel)
            {
                queryBuilder.append('=').append(ContentModel.PROP_ARCHIVED_BY.toPrefixString(this.namespaceService)).append(":\"")
                        .append(archivedByUser).append("\" AND ASPECT:\"")
                        .append(ContentModel.ASPECT_ARCHIVED.toPrefixString(this.namespaceService)).append('"');
            }
            else
            {
                // TODO Build an ANCESTOR query from archiveUser as origin using sensible exclusions for technical elements (systemFolder et
                // al)
            }
            sp.setQuery(queryBuilder.toString());

            if (nameForSearchParam != null)
            {
                queryBuilder.delete(0, queryBuilder.length());
                queryBuilder.append('=').append(ContentModel.PROP_NAME.toPrefixString(this.namespaceService)).append(":\"")
                        .append(nameForSearchParam).append('"');
                sp.addFilterQuery(queryBuilder.toString());
            }
            if (filterQueryParam != null)
            {
                sp.addFilterQuery(filterQueryParam);
            }

            sp.setSkipCount(startIndex);
            sp.setLimit(pageSize);
            sp.setMaxItems(pageSize);

            final ResultSet resultSet = this.searchService.query(sp);
            try
            {
                model.put("results", resultSet.getNodeRefs());
                paginationModel.put("totalRecords", Integer.valueOf(resultSet.length()));
                paginationModel.put("numberFound", Long.valueOf(resultSet.getNumberFound()));
            }
            finally
            {
                resultSet.close();
            }
        }
        else
        {
            model.put("results", new ArrayList<NodeRef>());
        }

        return model;
    }

}
