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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.archive.AbstractArchivedNodeWebScript;
import org.alfresco.repo.web.scripts.archive.ArchivedNodesDelete;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 *
 * @author Jonas van Malders
 * @author Ana Gouveia
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ArchivedItemsDelete extends AbstractArchivedNodeWebScript
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivedItemsDelete.class);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache)
    {
        final Map<String, Object> model = new HashMap<>();

        final Object parsedContent = req.parseContent();
        if (!(parsedContent instanceof JSONObject))
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No or invalid request data provided - only JSON data is supported");
        }

        final List<NodeRef> nodesToBePurged = new ArrayList<>();
        try
        {
            final JSONObject rq = (JSONObject) parsedContent;
            final JSONArray nodes = rq.getJSONArray("nodes");

            for (int slot = 0; slot < nodes.length(); slot++)
            {
                final String nodeRefStr = nodes.getString(slot);
                if (nodeRefStr != null && NodeRef.isNodeRef(nodeRefStr))
                {
                    final NodeRef nodeRef = new NodeRef(nodeRefStr);

                    // check if the current user has the permission to purge the node
                    this.validatePermission(nodeRef, AuthenticationUtil.getRunAsUser());

                    // If there is a specific NodeRef, then that is the only Node that should be purged.
                    // In this case, the NodeRef points to the actual node to be purged i.e. the node in
                    // the archive store.
                    nodesToBePurged.add(nodeRef);
                }
            }
        }
        catch (final JSONException jsonEx)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request JSON data", jsonEx);
        }

        LOGGER.debug("Purging {} nodes", nodesToBePurged.size());
        LOGGER.trace("Purging nodes {}", nodesToBePurged);

        // Now having identified the nodes to be purged, we simply have to do it.
        this.nodeArchiveService.purgeArchivedNodes(nodesToBePurged);

        model.put(ArchivedNodesDelete.PURGED_NODES, nodesToBePurged);

        return model;
    }
}
