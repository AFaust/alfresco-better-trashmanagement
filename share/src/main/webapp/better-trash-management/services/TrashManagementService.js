/**
 * This service handles pubSub events to deal with archived elements in an Alfresco Repository.
 * 
 * @module better-trash-management/services/TrashManagementService
 * @extends module:alfresco/services/BaseService
 * @author Axel Faust
 */
define([ 'dojo/_base/declare', 'alfresco/services/BaseService', 'alfresco/core/CoreXhr', 'service/constants/Default',
        'alfresco/core/topics', 'dojo/_base/lang', 'dojo/_base/array', 'alfresco/core/ObjectTypeUtils', 'alfresco/util/urlUtils' ],
        function(declare, BaseService, CoreXhr, Constants, topics, lang, array, ObjectTypeUtils, urlUtils)
        {
            return declare([ BaseService, CoreXhr ], {

                pubChainTopic : 'BETTER_TRASH_MANAGEMENT_PUBLISH_CHAIN',

                browseArchivedItemsTopic : 'BETTER_TRASH_MANAGEMENT_BROWSE_ARCHIVED_ITEMS',

                queryArchivedItemsTopic : 'BETTER_TRASH_MANAGEMENT_QUERY_ARCHIVED_ITEMS',

                registerSubscriptions : function betterTrashManagement_service_TrashManagementService__registerSubscriptions()
                {
                    this.alfSubscribe(this.pubChainTopic, lang.hitch(this, this.onPublishChain));
                    this.alfSubscribe(this.browseArchivedItemsTopic, lang.hitch(this, this.onBrowseArchivedItems));
                    this.alfSubscribe(this.queryArchivedItemsTopic, lang.hitch(this, this.onQueryArchivedItems));
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
                                        this.alfPublish(publication.publishTopic, publishPayload, true, false, publication.publishScope);
                                    }
                                }, this);
                    }
                },

                onBrowseArchivedItems : function betterTrashManagement_service_TrashManagementService__onBrowseArchivedItems(payload)
                {
                    var url, config;

                    if (payload.nodeRef)
                    {

                        url = Constants.PROXY_URI + 'api/better-trash-management/';
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
                            requestScope : payload.alfResponseScope,
                            alfTopic : payload.alfResponseTopic || this.browseArchivedItemsTopic,
                            method : 'GET'
                        };

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
                    var url, config, processedFilters;

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
                    if (payload.dataFilters)
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
                        requestScope : payload.alfResponseScope,
                        alfTopic : payload.alfResponseTopic || this.queryArchivedItemsTopic,
                        method : 'GET'
                    };

                    this.serviceXhr(config);
                }
            });
        });
