/* exported augmentServices */
function augmentServices(services)
{
    var idx, requiredServices, service;

    // track services we need to add (currently none of the services need specific configuration options)
    requiredServices = {
        'better-trash-management/services/TrashManagementService' : true,
        'alfresco/services/NodePreviewService' : true,
        'alfresco/services/LightboxService' : true,
        'alfresco/services/DialogService' : true,
        // needed for some lazy loading of data (some service expect data in weird/complex structures that we don't provide
        // we would have the data, but there is no way to configure services / widgets to use our data, and transforming is out of the
        // question
        // example: alfresco/renderers/Thumbnail doing a hard-coded ALF_RETRIEVE_SINGLE_DOCUMENT_REQUEST if currentItem does not provide
        // type/mimetype via hard-coded object paths
        'alfresco/services/DocumentService' : true
    };

    for (idx = 0; idx < services.length; idx++)
    {
        if (typeof services[idx].name === 'string' && requiredServices.hasOwnProperty(services[idx].name))
        {
            requiredServices[services[idx].name] = false;
        }
        else if (typeof services[idx] === 'string' && requiredServices.hasOwnProperty(services[idx]))
        {
            requiredServices[services[idx]] = false;
        }
    }

    for (service in requiredServices)
    {
        if (requiredServices.hasOwnProperty(service))
        {
            if (requiredServices[service] === true)
            {
                services.push({
                    name : service,
                    config : {}
                });
            }
        }
    }
}

function buildListResultWidgets(idPrefix)
{
    var widgets = [ {
        id : idPrefix + '_VIEW',
        name : 'alfresco/lists/views/AlfListView',
        config : {
            additionalCssClasses : 'bordered',
            widgetsForHeader : [],
            widgets : [ {
                id : idPrefix + '_VIEW_ROW',
                name : 'alfresco/lists/views/layouts/Row',
                config : {
                    // TODO Report bug - property zebraStriping without effect
                    additionalCssClasses : 'zebra-striping',
                    // TODO Report enhancement - simple CellProperty widget
                    // TODO Report enhancement - support of expanded (alternative) value as tooltip / mouseover
                    widgets : [ {
                        id : idPrefix + '_SELECTOR_CELL',
                        name : 'alfresco/lists/views/layouts/Cell',
                        config : {
                            additionalCssClasses : 'smallpad',
                            // we know Selector is 20px + 2x4px for smallpad
                            width : '30px',
                            widgets : [ {
                                name : 'alfresco/renderers/Selector',
                                itemKey : 'nodeRef',
                                publishGlobal : false,
                                publishToParent : false
                            } ]
                        }
                    }, {
                        id : idPrefix + '_THUMBNAIL_CELL',
                        name : 'alfresco/lists/views/layouts/Cell',
                        config : {
                            additionalCssClasses : 'smallpad',
                            // we know Thumbnail is 100px + 2x4px for smallpad
                            width : '110px',
                            widgets : [
                            // need to use separate configs for folder vs document/node
                            // if usePreviewService is set the widget will not handle publication
                            {
                                id : idPrefix + '_THUMBNAIL_LINK',
                                name : 'alfresco/renderers/Thumbnail',
                                config : {
                                    itemKey : 'nodeRef',
                                    imageTitleProperty : 'name',
                                    publishTopic : 'BETTER_TRASH_MANAGEMENT_PUBLISH_CHAIN',
                                    publishGlobal : true,
                                    publishPayloadType : 'PROCESS',
                                    publishPayloadModifiers : [ 'processCurrentItemTokens' ],
                                    useCurrentItemAsPayload : false,
                                    publishPayload : {
                                        publications : [ {
                                            publishTopic : [ 'TRASH-BROWSER-DISABLE-TAB', 'TRASH-BROWSER-SELECT-TAB' ],
                                            publishGlobal : true,
                                            publishPayload : {
                                                value : false,
                                                id : 'BTTM_TREE'
                                            }
                                        }, {
                                            publishTopic : 'TRASH_TREE_LIST/BETTER_TRASH_MANAGEMENT_SET_PARENT',
                                            publishGlobal : true,
                                            publishPayload : {
                                                name : 'nodeRef',
                                                value : '{nodeRef}'
                                            }
                                        } ]
                                    },
                                    renderFilter : [ {
                                        property : 'type',
                                        values : [ 'folder' ]
                                    } ]
                                }
                            }, {
                                id : idPrefix + '_THUMBNAIL',
                                name : 'alfresco/renderers/Thumbnail',
                                config : {
                                    itemKey : 'nodeRef',
                                    imageTitleProperty : 'name',
                                    lastThumbnailModificationProperty : 'properties.cm:lastThumbnailModification',
                                    renditionName : 'doclib',
                                    showDocumentPreview : true,
                                    usePreviewService : true,
                                    renderFilter : [ {
                                        property : 'type',
                                        values : [ 'folder' ],
                                        negate : true
                                    } ]
                                }
                            } ]
                        }
                    }, {
                        id : idPrefix + '_DETAILS_CELL',
                        name : 'alfresco/lists/views/layouts/Cell',
                        config : {
                            // TODO Expand cell into a document library-like details view
                            // TODO Consider how people might be able to customise view for different types
                            // of archived items
                            additionalCssClasses : 'smallpad',
                            widgets : [ {
                                id : idPrefix + '_DETAILS_COMPOSITE',
                                name : 'alfresco/core/ProcessWidgets',
                                config : {
                                    widgets : [ {
                                        id : idPrefix + '_DETAILS_NAME_LINK',
                                        name : 'alfresco/renderers/PropertyLink',
                                        config : {
                                            propertyToRender : 'properties.cm:name',
                                            renderSize : 'large',
                                            publishTopic : 'BETTER_TRASH_MANAGEMENT_PUBLISH_CHAIN',
                                            publishGlobal : true,
                                            publishPayloadType : 'PROCESS',
                                            publishPayloadModifiers : [ 'processCurrentItemTokens' ],
                                            useCurrentItemAsPayload : false,
                                            publishPayload : {
                                                publications : [ {
                                                    publishTopic : [ 'TRASH-BROWSER-DISABLE-TAB', 'TRASH-BROWSER-SELECT-TAB' ],
                                                    publishGlobal : true,
                                                    publishPayload : {
                                                        value : false,
                                                        id : 'BTTM_TREE'
                                                    }
                                                }, {
                                                    publishTopic : 'TRASH_TREE_LIST/BETTER_TRASH_MANAGEMENT_SET_PARENT',
                                                    publishGlobal : true,
                                                    publishPayload : {
                                                        name : 'nodeRef',
                                                        value : '{nodeRef}'
                                                    }
                                                } ]
                                            },
                                            renderFilter : [ {
                                                property : 'type',
                                                values : [ 'folder' ]
                                            } ]
                                        }
                                    }, {
                                        id : idPrefix + '_DETAILS_NAME',
                                        name : 'alfresco/renderers/Property',
                                        config : {
                                            propertyToRender : 'properties.cm:name',
                                            renderSize : 'large',
                                            renderFilter : [ {
                                                property : 'type',
                                                values : [ 'folder' ],
                                                negate : true
                                            } ]
                                        }
                                    }, {
                                        id : idPrefix + '_DETAILS_TITLE',
                                        name : 'alfresco/renderers/Property',
                                        config : {
                                            // TODO Define (dynamic) CSS class
                                            style : 'margin-left: 0.5ex;',
                                            propertyToRender : 'properties.cm:title',
                                            renderSize : 'large',
                                            renderedValuePrefix : '(',
                                            renderedValueSuffix : ')',
                                            renderFilter : [ {
                                                property : 'properties.cm:title',
                                                renderOnAbsentProperty : true,
                                                values : [ '' ],
                                                negate : true
                                            } ]
                                        }
                                    }, {
                                        id : idPrefix + '_DETAILS_MODIFIED_DATE',
                                        name : 'alfresco/renderers/Date',
                                        config : {
                                            renderOnNewLine : true,
                                            modifiedDateProperty : 'modified',
                                            modifiedByProperty : 'modifierDisplayName'
                                        }
                                    }, {
                                        id : idPrefix + '_DETAILS_ARCHIVED_DATE',
                                        name : 'alfresco/renderers/Date',
                                        config : {
                                            renderOnNewLine : true,
                                            modifiedDateProperty : 'archived',
                                            modifiedByProperty : 'archiverDisplayName',
                                            modifiedByMessage : 'trash-browser.archived-by'
                                        }
                                    }, {
                                        id : idPrefix + '_DETAILS_ORIGINAL_PATH',
                                        name : 'alfresco/renderers/Property',
                                        config : {
                                            renderOnNewLine : true,
                                            propertyToRender : 'displayPath',
                                            renderSize : 'medium',
                                            label : 'trash-browser.deleted-from'
                                        }
                                    } ]
                                }
                            } ]
                        }
                    }, {
                        id : idPrefix + '_ACTIONS_CELL',
                        name : 'alfresco/lists/views/layouts/Cell',
                        config : {
                            // we know Actions should be ~105px + 2x4px for smallpad
                            // due to block-nature, cell/column will expand if Actions is actually larger due to i18n
                            width : '115px',
                            widgets : [ {
                                id : idPrefix + '_ACTIONS',
                                name : 'alfresco/renderers/Actions',
                                config : {
                                    // TODO Report enhancement - make size of Actions configurable (it is frigging huge)
                                    // TODO Report enhancement - Actions should allow providing custom actions like the pre-defined
                                    // action widgets (currently customActions are mapped into AlfMenuItem dropping some options and
                                    // adding some non-suppressable ones)
                                    customActions : [ {
                                        id : idPrefix + '_ACTION_DELETE',
                                        label : 'trash-browser.action.delete.label',
                                        icon : 'document-delete',
                                        publishTopic : 'BETTER_TRASH_MANAGEMENT_DELETE_ARCHIVED_ITEMS',
                                        publishGlobal : true,
                                        publishPayloadType : 'PROCESS',
                                        publishPayloadModifiers : [ 'processCurrentItemTokens' ],
                                        publishPayload : {
                                            nodes : [ '{nodeRef}' ],
                                            successTopic : 'RELOAD_TRASH_ITEMS'
                                        },
                                    } ]
                                }
                            } ]
                        }
                    } ]
                }
            } ]
        }
    } ];

    return widgets;
}

function buildSearchPanel()
{
    var model;

    model = {
        id : 'BTTM_SEARCH',
        name : 'alfresco/core/ProcessWidgets',
        config : {
            pubSubScope : 'TRASH_SEARCH_LIST/',
            widgets : [
                    {
                        id : 'BTTM_SEARCH_LIST_PAGINATOR',
                        name : 'alfresco/lists/Paginator',
                        config : {
                            documentsPerPage : 20,
                            pageSizes : [ 20, 50, 100 ],
                            style : 'text-align:center;',
                            widgetsBefore : [ {
                                id : 'BTTM_SEARCH_LIST_PAGINATOR_BULK_SELECTOR',
                                name : 'alfresco/documentlibrary/AlfSelectDocumentListItems'
                            } ],
                            widgetsAfter : [ {
                                id : 'BTTM_SEARCH_LIST_PAGINATOR_RELOADER',
                                name : 'alfresco/menus/AlfMenuBarItem',
                                config : {
                                    label : 'trash-browser.paginator.reload.label',
                                    publishTopic : 'RELOAD_TRASH_ITEMS'
                                }
                            }, {
                                id : 'BTTM_SEARCH_LIST_PAGINATOR_SELECTED_ITEMS',
                                name : 'alfresco/documentlibrary/AlfSelectedItemsMenuBarPopup',
                                config : {
                                    // can't believe "selected-items.label" is not a global label
                                    // we prefix it to not mess with any global labels others may have added
                                    // TODO Report bug / enhancement
                                    label : 'trash-browser.paginator.selected-items.label',
                                    passive : false,
                                    itemKeyProperty : 'nodeRef',
                                    processActionPayloads : true,
                                    widgets : [ {
                                        name : 'alfresco/menus/AlfSelectedItemsMenuItem',
                                        config : {
                                            label : 'trash-browser.action.delete.label',
                                            iconClass : 'alf-delete-icon',
                                            publishTopic : 'BETTER_TRASH_MANAGEMENT_DELETE_ARCHIVED_ITEMS',
                                            publishGlobal : true,
                                            publishPayload : {
                                                successTopic : 'RELOAD_TRASH_ITEMS'
                                            }
                                        }
                                    } ]
                                }
                            } ]
                        }
                    },
                    {
                        id : 'BTTM_SEARCH_LIST',
                        name : 'alfresco/lists/AlfFilteredList',
                        config : {
                            reloadDataTopic : 'RELOAD_TRASH_ITEMS',
                            loadDataPublishTopic : 'BETTER_TRASH_MANAGEMENT_QUERY_ARCHIVED_ITEMS',
                            loadDataPublishPayload : {
                                defaultOperator : 'AND',
                                defaultQueryTemplate : '%(cm:name cm:title cm:description ia:whatEvent ia:descriptionEvent lnk:title lnk:description TEXT TAG)'
                            // TODO Fill with whatever the service needs
                            },
                            // TODO Report enhancement - filtering should not require these form topic cludges
                            filteringTopics : [ '_valueChangeOf_ITEM_NAME', '_valueChangeOf_KEYWORDS', '_valueChangeOf_ARCHIVE_DATE',
                                    '_valueChangeOf_TOP_LEVEL' ],
                            widgetsForFilters : [ {
                                id : 'BTTM_SEARCH_LIST_NAME_FILTER_INPUT',
                                name : 'alfresco/forms/controls/TextBox',
                                config : {
                                    // TODO Report enhancement - filter widgets should align properly
                                    // TODO Report enhancement - simple width customisation
                                    style : 'vertical-align:top;',
                                    fieldId : 'ITEM_NAME',
                                    name : 'name',
                                    label : 'trash-browser.filter.itemName.label',
                                    placeHolder : 'trash-browser.filter.itemName.placeHolder'
                                }
                            }, {
                                id : 'BTTM_SEARCH_LIST_KEYWORDS_FILTER_INPUT',
                                name : 'alfresco/forms/controls/TextBox',
                                config : {
                                    // TODO Report enhancement - filter widgets should align properly
                                    // TODO Report enhancement - simple width customisation
                                    style : 'vertical-align:top;',
                                    fieldId : 'KEYWORDS',
                                    name : 'keywords',
                                    label : 'trash-browser.filter.keywords.label',
                                    placeHolder : 'trash-browser.filter.keywords.placeHolder'
                                }
                            }, {
                                id : 'BTTM_SEARCH_LIST_ARCHIVE_DATE_FILTER_INPUT',
                                name : 'better-trash-management/forms/controls/DateRange',
                                config : {
                                    // TODO Report enhancement - filter widgets should align properly
                                    // TODO Report enhancement - allow configuration option in base DateRange widget to allow open-ended
                                    // ranges
                                    // TODO Report bug - change of "from" value does not trigger pubSub in base DateRange widget
                                    style : 'vertical-align:top;',
                                    fieldId : 'ARCHIVE_DATE',
                                    name : 'archiveDate',
                                    label : 'trash-browser.filter.archiveDate.label',
                                    allowOpenEndDateRange : true
                                }
                            }, {
                                id : 'BTTM_SEARCH_LIST_TOP_LEVEL_FILTER_INPUT',
                                name : 'alfresco/forms/controls/CheckBox',
                                config : {
                                    // TODO Report enhancement - filter widgets should align properly
                                    style : 'vertical-align:top;',
                                    fieldId : 'TOP_LEVEL',
                                    name : 'topLevel',
                                    label : 'trash-browser.filter.topLevel.label'
                                }
                            } ],
                            usePagination : true,
                            currentPageSize : 20,
                            itemsProperty : 'items',
                            widgets : buildListResultWidgets('BTTM_SEARCH_LIST'),
                            // TODO Report enhancement - layout of AlfList (and sub-modules) should allow for consistent padding to stop
                            // views clinging to the edge while not forcing i.e. filter form to have an even larger inset than by default
                            style : 'padding: 0 10px;'
                        }
                    } ]
        }
    };

    return model;
}

function buildHierarchyPanel()
{
    var model;

    model = {
        id : 'BTTM_TREE',
        name : 'alfresco/core/ProcessWidgets',
        config : {
            pubSubScope : 'TRASH_TREE_LIST/',
            widgets : [ {
                id : 'BTTM_TREE_LIST_PAGINATOR',
                name : 'alfresco/lists/Paginator',
                config : {
                    documentsPerPage : 20,
                    pageSizes : [ 20, 50, 100 ],
                    style : 'text-align:center;',
                    widgetsBefore : [ {
                        id : 'BTTM_TREE_LIST_PAGINATOR_BULK_SELECTOR',
                        name : 'alfresco/documentlibrary/AlfSelectDocumentListItems'
                    } ],
                    widgetsAfter : [ {
                        id : 'BTTM_TREE_LIST_PAGINATOR_RELOADER',
                        name : 'alfresco/menus/AlfMenuBarItem',
                        config : {
                            label : 'trash-browser.paginator.reload.label',
                            publishTopic : 'RELOAD_TRASH_ITEMS'
                        }
                    }, {
                        id : 'BTTM_TREE_LIST_PAGINATOR_SELECTED_ITEMS',
                        name : 'alfresco/documentlibrary/AlfSelectedItemsMenuBarPopup',
                        config : {
                            // can't believe "selected-items.label" is not a global label
                            // we prefix it to not mess with any global labels others may have added
                            // TODO Report bug / enhancement
                            label : 'trash-browser.paginator.selected-items.label',
                            passive : false,
                            itemKeyProperty : 'nodeRef',
                            widgets : [ {
                                name : 'alfresco/menus/AlfSelectedItemsMenuItem',
                                config : {
                                    label : 'trash-browser.action.delete.label',
                                    iconClass : 'alf-delete-icon',
                                    publishTopic : 'BETTER_TRASH_MANAGEMENT_DELETE_ARCHIVED_ITEMS',
                                    publishGlobal : true,
                                    publishPayload : {
                                        successTopic : 'RELOAD_TRASH_ITEMS'
                                    }
                                }
                            } ]
                        }
                    } ]
                }
            }, {
                id : 'BTTM_TREE_LIST',
                name : 'alfresco/lists/AlfSortablePaginatedList',
                config : {
                    reloadDataTopic : 'RELOAD_TRASH_ITEMS',
                    loadDataPublishTopic : 'BETTER_TRASH_MANAGEMENT_BROWSE_ARCHIVED_ITEMS',
                    loadDataPublishPayload : {},
                    filteringTopics : [ 'BETTER_TRASH_MANAGEMENT_SET_PARENT' ],
                    usePagination : true,
                    currentPageSize : 20,
                    itemsProperty : 'items',
                    widgets : buildListResultWidgets('BTTM_TREE_LIST'),
                    // TODO Report enhancement - layout of AlfList (and sub-modules) should allow for consistent padding to stop views
                    // clinging to the edge while not forcing i.e. filter form to have an even larger inset than by default
                    style : 'padding: 0 10px;'
                }
            } ]
        }
    };

    return model;
}

/* exported buildPageWidgets */
function buildPageWidgets()
{
    var model, searchPanel, hierarchyPanel;

    searchPanel = buildSearchPanel();
    searchPanel.title = 'trash-browser.tab.search.title';
    searchPanel.selected = true;

    hierarchyPanel = buildHierarchyPanel();
    hierarchyPanel.title = 'trash-browser.tab.hierarchy.title';
    hierarchyPanel.selected = false;
    hierarchyPanel.disabled = true;

    model = {
        id : 'BTTM_TABS',
        name : 'alfresco/layout/AlfTabContainer',
        config : {
            widgets : [ searchPanel, hierarchyPanel ],
            tabSelectionTopic : 'TRASH-BROWSER-SELECT-TAB',
            tabDisablementTopic : 'TRASH-BROWSER-DISABLE-TAB'
        }
    };
    return model;
}
