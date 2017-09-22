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
package de.axelfaust.alfresco.trash.management.repo.behaviour;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnRestoreNodePolicy;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.TransactionalResourceHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.axelfaust.alfresco.trash.management.repo.BetterTrashManagementModel;

/**
 * This behaviour ensures that default {@link PermissionService#READ read privileges} are set for the archiving user so that the elements
 * can be queried, e.g. via SOLR even if there are no explicitly set permission on the node itself.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class UserTrashContainer implements InitializingBean, OnRestoreNodePolicy, OnDeleteNodePolicy
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTrashContainer.class);

    protected PolicyComponent policyComponent;

    protected NodeService nodeService;

    protected NodeArchiveService nodeArchiveService;

    protected PermissionService permissionService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "policyComponent", this.policyComponent);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "nodeArchiveService", this.nodeArchiveService);
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);

        this.policyComponent.bindClassBehaviour(OnDeleteNodePolicy.QNAME, this,
                new JavaBehaviour(this, "onDeleteNode", NotificationFrequency.EVERY_EVENT));
        this.policyComponent.bindClassBehaviour(OnRestoreNodePolicy.QNAME, this,
                new JavaBehaviour(this, "onRestoreNode", NotificationFrequency.EVERY_EVENT));
    }

    /**
     * @param policyComponent
     *            the policyComponent to set
     */
    public void setPolicyComponent(final PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
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
     * @param permissionService
     *            the permissionService to set
     */
    public void setPermissionService(final PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeleteNode(final ChildAssociationRef childAssocRef, final boolean isNodeArchived)
    {
        final NodeRef deletedChildRef = childAssocRef.getChildRef();
        if (isNodeArchived)
        {
            // TODO Report bug: policy parameter isNodeArchive is incorrect in various cases, e.g. set to true if node is moved due to
            // restore, though in that case the policy will usually be ignored due to the "storesToIgnorePolicies" list

            final NodeRef archiveRootNode = this.nodeArchiveService.getStoreArchiveNode(deletedChildRef.getStoreRef());
            if (archiveRootNode != null)
            {
                final NodeRef archivedNode = this.nodeArchiveService.getArchivedNode(deletedChildRef);
                if (this.nodeService.exists(archivedNode))
                {
                    final Map<QName, Serializable> properties = this.nodeService.getProperties(archivedNode);
                    final String archivedBy = DefaultTypeConverter.INSTANCE.convert(String.class,
                            properties.get(ContentModel.PROP_ARCHIVED_BY));

                    // only in this case is it an actual archive-move of a root element (we don't need to handle cascade moves)
                    if (archivedBy != null)
                    {
                        // ensure the archiving user can always query top-level elements
                        // explicit permissions may be only set to a group from which the user might be removed later on
                        final Set<AccessPermission> allSetPermissions = this.permissionService.getAllSetPermissions(archivedNode);
                        final boolean userReadAccessSet = allSetPermissions.stream().anyMatch(setPermission -> {
                            final boolean readAccessSet = setPermission.getAccessStatus() == AccessStatus.ALLOWED
                                    && PermissionService.READ.equals(setPermission.getPermission())
                                    && archivedBy.equals(setPermission.getAuthority());
                            return readAccessSet;
                        });

                        if (!userReadAccessSet)
                        {
                            LOGGER.debug("Adding explicit read permission to archived node {} for {} due to inherit=false", archivedNode,
                                    archivedBy);
                            this.permissionService.setPermission(archivedNode, archivedBy, PermissionService.READ, true);
                            this.nodeService.addAspect(archivedNode, BetterTrashManagementModel.ASPECT_USER_READ_ACCESS_GRANTED,
                                    Collections.singletonMap(BetterTrashManagementModel.PROP_READ_ACCESS_GRANTED_TO, archivedBy));
                        }
                    }
                    else
                    {
                        LOGGER.debug("Not handling deletion of {} as it is not an archive operation", deletedChildRef);
                    }
                }
                else
                {
                    LOGGER.warn("Node {} know to have been archived in current txn does not exist", archivedNode);
                }
            }
            else
            {
                LOGGER.debug("Not handling deletion of {} as it is not an archive operation", deletedChildRef);
            }
        }
        else
        {
            LOGGER.debug("Not handling deletion of {} as it is not an archive operation", deletedChildRef);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestoreNode(final ChildAssociationRef childAssocRef)
    {
        final NodeRef restoredNode = childAssocRef.getChildRef();
        final NodeRef archiveRootNode = this.nodeArchiveService.getStoreArchiveNode(restoredNode.getStoreRef());
        if (archiveRootNode != null)
        {
            final NodeRef parentRef = childAssocRef.getParentRef();
            final NodeRef potentiallyRestoredParentNode = this.nodeArchiveService.getArchivedNode(parentRef);
            final Set<Object> nodesRestoredInTxn = TransactionalResourceHelper
                    .getSet(UserTrashContainer.class.getName() + "-restoredNodes");

            if (!nodesRestoredInTxn.contains(potentiallyRestoredParentNode))
            {
                if (this.nodeService.hasAspect(restoredNode, BetterTrashManagementModel.ASPECT_USER_READ_ACCESS_GRANTED))
                {
                    final Map<QName, Serializable> properties = this.nodeService.getProperties(restoredNode);
                    final String readAccessGrantedTo = DefaultTypeConverter.INSTANCE.convert(String.class,
                            properties.get(BetterTrashManagementModel.PROP_READ_ACCESS_GRANTED_TO));
                    LOGGER.debug(
                            "Removing explicit read permission from restored node {} for {} which was granted as part of trash management",
                            restoredNode, readAccessGrantedTo);
                    this.permissionService.deletePermission(restoredNode, readAccessGrantedTo, PermissionService.READ);
                    this.nodeService.removeAspect(restoredNode, BetterTrashManagementModel.ASPECT_USER_READ_ACCESS_GRANTED);
                }
                nodesRestoredInTxn.add(restoredNode);
            }
            else
            {
                LOGGER.debug("Not handling restoration of node {} as it is a cascade-restoration operation", restoredNode);
            }
        }
        else
        {
            LOGGER.debug("Not handling restoration of {} as it is not an regular restore-from-archive operation", restoredNode);
        }
    }

}
