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

import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ArchivedItemsChildrenGet extends AbstractArchivedItemsRetrievalWebScript
{

    private static final String TEMPLATE_STORE_PROTOCOL = "storeProtocol";

    private static final String TEMPLATE_STORE_IDENTIFIER = "storeIdentifier";

    private static final String TEMPLATE_UUID = "uuid";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache)
    {
        final Match serviceMatch = req.getServiceMatch();
        final Map<String, String> templateVars = serviceMatch.getTemplateVars();
        final String storeProtocol = templateVars.get(TEMPLATE_STORE_PROTOCOL);
        final String storeIdentifier = templateVars.get(TEMPLATE_STORE_IDENTIFIER);
        final String uuid = templateVars.get(TEMPLATE_UUID);

        final StoreRef store = new StoreRef(storeProtocol, storeIdentifier);
        final NodeRef node = new NodeRef(store, uuid);

        this.validateArchivedItem(node);

        final Map<String, Object> model = this.executeImpl(req, node, status, cache);
        return model;
    }

    protected void validateArchivedItem(final NodeRef node)
    {
        NodeRef currentNode = node;
        while (currentNode != null && !this.nodeService.hasAspect(currentNode, ContentModel.ASPECT_ARCHIVED))
        {
            final ChildAssociationRef primaryParent = this.nodeService.getPrimaryParent(currentNode);
            currentNode = primaryParent != null ? primaryParent.getParentRef() : null;
        }

        if (currentNode == null)
        {
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "Archived node " + node + " does not exist");
        }
    }

    @Override
    protected SearchParameters prepareSearchParameters(final WebScriptRequest req, final NodeRef archiveContextNode, final int pageSize,
            final int startIndex)
    {
        final SearchParameters sp = new SearchParameters();
        sp.addStore(archiveContextNode.getStoreRef());
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        sp.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

        final StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("PARENT:\"").append(archiveContextNode).append("\"");
        sp.setQuery(queryBuilder.toString());

        sp.setSkipCount(startIndex);
        sp.setLimit(pageSize);
        sp.setMaxItems(pageSize);
        return sp;
    }
}
