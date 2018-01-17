package de.axelfaust.alfresco.trash.management.repo.web.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.archive.AbstractArchivedNodeWebScript;
import org.alfresco.repo.web.scripts.archive.ArchivedNodesDelete;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ArchivedItemsDelete extends AbstractArchivedNodeWebScript 
{
    private static Log log = LogFactory.getLog(ArchivedItemsDelete.class);

	@Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, Object> model = new HashMap<String, Object>();
        
        // Current user
        String userID = AuthenticationUtil.getFullyAuthenticatedUser();
        if (userID == null)
        {
            throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "Web Script ["
                        + req.getServiceMatch().getWebScript().getDescription()
                        + "] requires user authentication.");
        }
        
        JSONParser parser = new JSONParser();
        JSONArray jsonList;
		try 
		{
			jsonList = (JSONArray)parser.parse(req.getContent().getContent());
		} 
		catch (ParseException | IOException e) 
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not parse the JSON body of the request");
		}
		if (jsonList == null || jsonList.isEmpty())
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Empty or no node references provided");
		}
        
        List<NodeRef> nodesToBePurged = new ArrayList<NodeRef>();
        for(Object listElement : jsonList)
        {
        	if (listElement != null && listElement instanceof JSONObject)
			{
        		JSONObject jsonObject = (JSONObject)listElement;
        		String nodeRefString = (String)jsonObject.get("nodeRef");
        		if(nodeRefString != null && NodeRef.isNodeRef(nodeRefString))
        		{
        			NodeRef nodeRef = new NodeRef(nodeRefString);
            		if (nodeRef != null)
                    {
                        // check if the current user has the permission to purge the node
                        validatePermission(nodeRef, userID);
                        
                        // If there is a specific NodeRef, then that is the only Node that should be purged.
                        // In this case, the NodeRef points to the actual node to be purged i.e. the node in
                        // the archive store.
                        nodesToBePurged.add(nodeRef);
                    }
        		}
			}
        }
        
        
        if (log.isDebugEnabled())
        {
            log.debug("Purging " + nodesToBePurged.size() + " nodes");
        }
        
        // Now having identified the nodes to be purged, we simply have to do it.
        nodeArchiveService.purgeArchivedNodes(nodesToBePurged);
	
        model.put(ArchivedNodesDelete.PURGED_NODES, nodesToBePurged);
        
        return model;
    }	
}
