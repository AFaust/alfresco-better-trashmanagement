/**
 * This service handles pubSub events to deal with archived elements in an Alfresco Repository.
 * 
 * @module better-trash-management/services/TrashManagementService
 * @extends module:alfresco/services/BaseService
 * @author Axel Faust
 */
define(
        [ 'dojo/_base/declare', 'alfresco/services/BaseService', 'alfresco/core/CoreXhr', 'service/constants/Default',
                'alfresco/core/topics', 'dojo/_base/lang', 'dojo/_base/array', 'alfresco/core/ObjectTypeUtils', 'alfresco/util/urlUtils' ],
        function(declare, BaseService, CoreXhr, Constants, topics, lang, array, ObjectTypeUtils, urlUtils)
        {
            return declare(
                    [ BaseService, CoreXhr ],
                    {

                        pubChainTopic : 'BETTER_TRASH_MANAGEMENT_PUBLISH_CHAIN',

                        browseArchivedItemsTopic : 'BETTER_TRASH_MANAGEMENT_BROWSE_ARCHIVED_ITEMS',

                        queryArchivedItemsTopic : 'BETTER_TRASH_MANAGEMENT_QUERY_ARCHIVED_ITEMS',

                        deleteArchivedItemsTopic : 'BETTER_TRASH_MANAGEMENT_DELETE_ARCHIVED_ITEMS',

                        registerSubscriptions : function betterTrashManagement_service_TrashManagementService__registerSubscriptions()
                        {
                            this.alfSubscribe(this.pubChainTopic, lang.hitch(this, this.onPublishChain));
                            this.alfSubscribe(this.browseArchivedItemsTopic, lang.hitch(this, this.onBrowseArchivedItems));
                            this.alfSubscribe(this.queryArchivedItemsTopic, lang.hitch(this, this.onQueryArchivedItems));
                            this.alfSubscribe(this.deleteArchivedItemsTopic, lang.hitch(this, this.onDeleteArchivedItems));
                        },

                        // TODO There should be a standard way to have a publication composed of multiple topics + payloads
                        onPublishChain : function betterTrashManagement_service_TrashManagementService__onPublishChain(payload)
                        {
                            if (Array.isArray(payload.publications))
                            {
                                array.forEach(payload.publications,
                                        function betterTrashManagement_service_TrashManagementService__onPublishChain_forEach(publication)
                                        {
                                            var publishPayload = {};
                                            if (publication.publishTopic)
                                            {
                                                if (publication.publishPayload)
                                                {
                                                    publishPayload = lang.clone(publication.publishPayload);
                                                    publishPayload.responseScope = payload.alfResponseScope;
                                                }
                                                this.alfPublish(publication.publishTopic, publishPayload, true, false,
                                                        publication.publishScope);
                                            }
                                        }, this);
                            }
                        },

                        onBrowseArchivedItems : function betterTrashManagement_service_TrashManagementService__onBrowseArchivedItems(
                                payload)
                        {
                            var url, config, uuid;

                            if (payload.nodeRef)
                            {

                                url = Constants.PROXY_URI + 'api/better-trash-management/archivedItems/';
                                url += encodeURI(payload.nodeRef.replace(/:?\/+/g, '/'));
                                url += '/children';

                                if (payload.pageSize)
                                {
                                    url = urlUtils.addQueryParameter(url, 'pageSize', payload.pageSize, true);
                                }

                                if (payload.page)
                                {
                                    url = urlUtils.addQueryParameter(url, 'page', payload.page, true);
                                }

                                if (payload.page && payload.pageSize)
                                {
                                    var startIndex = (payload.page - 1) * payload.pageSize;
                                    url = urlUtils.addQueryParameter(url, 'startIndex', startIndex, true);
                                }

                                config = {
                                    preventCache : true,
                                    url : url,
                                    method : 'GET'
                                };

                                uuid = this.generateUuid();
                                this.alfSubscribe(uuid + '_SUCCESS', lang.hitch(this, this._successTranslator, payload, uuid,
                                        this.browseArchivedItemsTopic));
                                this.alfSubscribe(uuid + '_FAILURE', lang.hitch(this, this._successTranslator, payload, uuid,
                                        this.browseArchivedItemsTopic));
                                config.alfTopic = uuid;

                                this.serviceXhr(config);
                            }
                            else
                            {
                                this.alfPublish((payload.alfResponseTopic || this.browseArchivedItemsTopic) + '_FAILURE', {}, false, false,
                                        payload.alfResponseScope);
                            }
                        },

                        onQueryArchivedItems : function betterTrashManagement_service_TrashManagementService__onQueryArchivedItems(payload)
                        {
                            var url, config, processedFilters, uuid;

                            url = Constants.PROXY_URI + 'api/better-trash-management/archivedItems';

                            if (payload.pageSize)
                            {
                                url = urlUtils.addQueryParameter(url, 'pageSize', payload.pageSize, true);
                            }

                            if (payload.page)
                            {
                                url = urlUtils.addQueryParameter(url, 'page', payload.page, true);
                            }

                            if (payload.page && payload.pageSize)
                            {
                                var startIndex = (payload.page - 1) * payload.pageSize;
                                url = urlUtils.addQueryParameter(url, 'startIndex', startIndex, true);
                            }

                            if (payload.defaultOperator)
                            {
                                url = urlUtils.addQueryParameter(url, 'defaultOperator', payload.defaultOperator, true);
                            }

                            if (payload.defaultQueryTemplate)
                            {
                                url = urlUtils.addQueryParameter(url, 'defaultQueryTemplate', payload.defaultQueryTemplate, true);
                            }

                            processedFilters = {};
                            if (payload.dataFilters && lang.isArray(payload.dataFilters))
                            {
                                // support our specific filters (only)
                                array.forEach(payload.dataFilters, function(filter)
                                {
                                    var dateFragments;

                                    switch (filter.name)
                                    {
                                        case 'name':
                                        case 'archivedByUser':
                                        case 'baseStore':
                                        case 'filterQuery':
                                        case 'topLevel':
                                            url = urlUtils.addQueryParameter(url, filter.name, filter.value, true);
                                            processedFilters[filter.name] = true;
                                            break;
                                        case 'archiveDate':
                                            dateFragments = filter.value.split('|');
                                            if (dateFragments[0] !== undefined && dateFragments[0] !== null && dateFragments[0] !== '')
                                            {
                                                url = urlUtils.addQueryParameter(url, 'archiveDateFrom', dateFragments[0], true);
                                            }
                                            if (dateFragments[1] !== undefined && dateFragments[1] !== null && dateFragments[1] !== '')
                                            {
                                                url = urlUtils.addQueryParameter(url, 'archiveDateTo', dateFragments[1], true);
                                            }
                                            processedFilters[filter.name] = true;
                                            break;
                                    }
                                });
                            }

                            if (!processedFilters.hasOwnProperty('baseStore'))
                            {
                                url = urlUtils.addQueryParameter(url, 'baseStore', 'workspace://SpacesStore', true);
                            }

                            config = {
                                preventCache : true,
                                url : url,
                                method : 'GET'
                            };

                            uuid = this.generateUuid();
                            this.alfSubscribe(uuid + '_SUCCESS', lang.hitch(this, this._successTranslator, payload, uuid,
                                    this.queryArchivedItemsTopic));
                            this.alfSubscribe(uuid + '_FAILURE', lang.hitch(this, this._successTranslator, payload, uuid,
                                    this.queryArchivedItemsTopic));
                            config.alfTopic = uuid;

                            this.serviceXhr(config);
                        },

                        onDeleteArchivedItems : function betterTrashManagement_service_TrashManagementService__onDeleteArchivedItems(
                                payload)
                        {
                            var iterFn, nodes, url, config, uuid;

                            iterFn = function betterTrashManagement_service_TrashManagementService__onDeleteArchivedItems_iterFn(
                                    selectedItem)
                            {
                                if (typeof selectedItem === 'string' && /^[^:]+:\/\/[^\/]+\/.+$/.test(selectedItem))
                                {
                                    nodes.push(selectedItem);
                                }
                                else if (selectedItem.nodeRef && typeof selectedItem.nodeRef === 'string')
                                {
                                    nodes.push(selectedItem.nodeRef);
                                }
                            };

                            if (payload.selectedItems && lang.isArray(payload.selectedItems))
                            {
                                nodes = [];
                                array.forEach(payload.selectedItems, iterFn, this);
                            }
                            else if (payload.nodes && lang.isArray(payload.nodes))
                            {
                                nodes = [];
                                array.forEach(payload.nodes, iterFn, this);
                            }

                            if (nodes)
                            {
                                url = Constants.PROXY_URI + 'api/better-trash-management/archivedItems/bulkDelete';

                                config = {
                                    preventCache : true,
                                    url : url,
                                    data : {
                                        nodes : nodes
                                    },
                                    method : 'POST'
                                };

                                uuid = this.generateUuid();
                                this.alfSubscribe(uuid + '_SUCCESS', lang.hitch(this, this._successTranslator, payload, uuid,
                                        this.deleteArchivedItemsTopic));
                                this.alfSubscribe(uuid + '_FAILURE', lang.hitch(this, this._successTranslator, payload, uuid,
                                        this.deleteArchivedItemsTopic));
                                config.alfTopic = uuid;

                                this.serviceXhr(config);
                            }
                        },

                        _failureTranslator : function betterTrashManagement_service_TrashManagementService__failureTranslator(
                                requestPayload, uuid, defaultTopic, failurePayload)
                        {
                            var responseTopic, responseScope;

                            this._unsubscribeXhrHandles(uuid);

                            // explicit failure topic specified by the original publisher
                            if (requestPayload.failureTopic)
                            {
                                responseTopic = requestPayload.failureTopic;
                            }
                            // generic, implicit failure topic provided by Core#alfPublish
                            // alfFailureTopic was introduced in an update, so we fall back to the generic alfResponseTopic
                            else
                            {
                                responseTopic = requestPayload.alfFailureTopic
                                        || ((requestPayload.alfResponseTopic || defaultTopic) + '_FAILURE');
                            }

                            responseScope = null;
                            if (responseTopic.indexOf(requestPayload.alfResponseScope) !== 0)
                            {
                                responseScope = requestPayload.alfResponseScope;
                            }

                            this.alfPublish(responseTopic, failurePayload, responseScope === null, false, responseScope);
                        },

                        _successTranslator : function betterTrashManagement_service_TrashManagementService__successTranslator(
                                requestPayload, uuid, defaultTopic, successPayload)
                        {
                            var responseTopic, responseScope;

                            this._unsubscribeXhrHandles(uuid);

                            // explicit success topic specified by the original publisher
                            if (requestPayload.successTopic)
                            {
                                responseTopic = requestPayload.successTopic;
                            }
                            // generic, implicit failure topic provided by Core#alfPublish
                            // alfSuccessTopic was introduced in an update, so we fall back to the generic alfResponseTopic
                            else
                            {
                                responseTopic = requestPayload.alfSucessTopic
                                        || ((requestPayload.alfResponseTopic || defaultTopic) + '_SUCCESS');
                            }

                            responseScope = null;
                            if (responseTopic.indexOf(requestPayload.alfResponseScope) !== 0)
                            {
                                responseScope = requestPayload.alfResponseScope;
                            }

                            this.alfPublish(responseTopic, successPayload, responseScope === null, false, responseScope);
                        },

                        _unsubscribeXhrHandles : function betterTrashManagement_service_TrashManagementService__unsubscribeXhrHandles(uuid)
                        {
                            array
                                    .forEach(
                                            this.alfSubscriptions,
                                            function betterTrashManagement_service_TrashManagementService__unsubscribeXhrHandles_checkAndUnsubscribe(
                                                    handle)
                                            {
                                                if (handle.scopedTopic.indexOf(uuid) === 0)
                                                {
                                                    this.alfUnsubscribe(handle);
                                                }
                                            }, this);
                        }
                    });
        });
