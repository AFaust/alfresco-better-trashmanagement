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

                QUERY_ARCHIVED_ITEMS : 'BETTER_TRASH_MANAGEMENT_QUERY_ARCHIVED_ITEMS',

                registerSubscriptions : function betterTrashManagement_service_TrashManagementService__registerSubscriptions()
                {
                    this.alfSubscribe(this.QUERY_ARCHIVED_ITEMS, lang.hitch(this, this.onQueryArchivedItems));
                },

                onQueryArchivedItems : function betterTrashManagement_service_TrashManagementService__onQueryArchivedItems(payload)
                {
                    var url, config, processedFilters;

                    url = Constants.PROXY_URI + 'api/better-trash-management/archivedItems';

                    if (payload.pageSize)
                    {
                        url = urlUtils.addQueryParameter(url, 'pageSize', payload.pageSize);
                    }

                    if (payload.page)
                    {
                        url = urlUtils.addQueryParameter(url, 'page', payload.page);
                    }

                    if (payload.page && payload.pageSize)
                    {
                        var startIndex = (payload.page - 1) * payload.pageSize;
                        url = urlUtils.addQueryParameter(url, 'startIndex', startIndex);
                    }

                    processedFilters = {};
                    if (payload.dataFilters)
                    {
                        // support our specific filters (only)
                        array.forEach(payload.dataFilters, function(filter)
                        {
                            switch (filter.name)
                            {
                                case 'name':
                                case 'archivedByUser':
                                case 'baseStore':
                                case 'filterQuery':
                                    urlUtils.addQueryParameter(url, filter.name, filter.value);
                                    processedFilters[filter.name] = true;
                                    break;
                            }
                        });
                    }
                    
                    if (!processedFilters.hasOwnProperty('baseStore'))
                    {
                        urlUtils.addQueryParameter(url, 'baseStore', 'workspace://SpacesStore');
                    }

                    config = {
                        preventCache : true,
                        url : url,
                        requestScope : payload.alfResponseScope,
                        alfTopic : payload.alfResponseTopic || null,
                        method : 'GET'
                    };

                    this.serviceXhr(config);
                }
            });
        });
