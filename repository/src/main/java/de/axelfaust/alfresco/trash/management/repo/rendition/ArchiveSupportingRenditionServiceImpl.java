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
package de.axelfaust.alfresco.trash.management.repo.rendition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.rendition.RenditionServiceImpl;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This variant of a rendition service will work with archived items in that it will not auto-magically filter renditions that have been
 * archived if the original node itself is actually being stored in the same store.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ArchiveSupportingRenditionServiceImpl extends RenditionServiceImpl
{

    // base class (as large parts of Alfresco do) uses Commons Logging - oh the humanity!!
    private static final Logger LOGGER = LoggerFactory.getLogger(RenditionServiceImpl.class);

    protected NodeService nodeService;

    protected ContentService contentService;

    protected NodeArchiveService nodeArchiveService;

    /**
     *
     * {@inheritDoc}
     */
    // base class is super inconsistent - some services injected via Spring, two obtained from ServiceRegistry
    @Override
    public void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        super.setServiceRegistry(serviceRegistry);
        this.nodeService = serviceRegistry.getNodeService();
        this.contentService = serviceRegistry.getContentService();
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
     *
     * {@inheritDoc}
     */
    @Override
    public List<ChildAssociationRef> getRenditions(final NodeRef node, final String mimeTypePrefix)
    {
        final List<ChildAssociationRef> allRenditions = this.getRenditions(node);
        List<ChildAssociationRef> filteredResults = new ArrayList<>();

        for (final ChildAssociationRef chAssRef : allRenditions)
        {
            final NodeRef renditionNode = chAssRef.getChildRef();

            QName contentProperty = ContentModel.PROP_CONTENT;
            final Serializable contentPropertyName = this.nodeService.getProperty(renditionNode, ContentModel.PROP_CONTENT_PROPERTY_NAME);
            if (contentPropertyName != null)
            {
                contentProperty = (QName) contentPropertyName;
            }

            final ContentReader reader = this.contentService.getReader(renditionNode, contentProperty);
            if (reader != null && reader.exists())
            {
                final String readerMimeType = reader.getMimetype();
                if (readerMimeType.startsWith(mimeTypePrefix))
                {
                    filteredResults.add(chAssRef);
                }

            }
        }
        filteredResults = this.removeArchivedRenditionsFrom(node, filteredResults);

        return filteredResults;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ChildAssociationRef getRenditionByName(final NodeRef node, final QName renditionName)
    {
        List<ChildAssociationRef> renditions = Collections.emptyList();

        // Check that the node has the renditioned aspect applied
        if (this.nodeService.hasAspect(node, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name -
            // there should only be 1 (or 0)
            renditions = this.nodeService.getChildAssocs(node, RenditionModel.ASSOC_RENDITION, renditionName);
            renditions = this.removeArchivedRenditionsFrom(node, renditions);
        }
        if (renditions.isEmpty())
        {
            return null;
        }
        else
        {
            if (renditions.size() > 1 && LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Unexpectedly found " + renditions.size() + " renditions of name " + renditionName + " on node " + node);
            }
            return renditions.get(0);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<ChildAssociationRef> getRenditions(final NodeRef node)
    {
        List<ChildAssociationRef> result = Collections.emptyList();

        // Check that the node has the renditioned aspect applied
        if (this.nodeService.hasAspect(node, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name
            result = this.nodeService.getChildAssocs(node, RenditionModel.ASSOC_RENDITION, RegexQNamePattern.MATCH_ALL);

            result = this.removeArchivedRenditionsFrom(node, result);
        }
        return result;
    }

    // this is the patched variant of a private method
    protected List<ChildAssociationRef> removeArchivedRenditionsFrom(final NodeRef sourceNode,
            final List<ChildAssociationRef> renditionAssocs)
    {
        // This is a workaround for a bug in the NodeService (no JIRA number yet) whereby a call to
        // nodeService.getChildAssocs can return all children, including children in the archive store.
        final List<ChildAssociationRef> result = new ArrayList<>();

        // check if there even is an archive for the store of the node
        final NodeRef archivedNode = this.nodeArchiveService.getStoreArchiveNode(sourceNode.getStoreRef());
        final StoreRef archiveStore = archivedNode != null ? archivedNode.getStoreRef() : null;

        for (final ChildAssociationRef chAssRef : renditionAssocs)
        {
            // If the rendition has *not* been deleted, then it should remain in the result list.
            final StoreRef renditionStoreRef = chAssRef.getChildRef().getStoreRef();
            if (!EqualsHelper.nullSafeEquals(archiveStore, renditionStoreRef))
            {
                result.add(chAssRef);
            }
        }

        return result;
    }
}
