<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-header.lib.js">
<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-footer.lib.js">
<import resource="classpath:/alfresco/site-webscripts/de/axelfaust/better-trash-management/pages/trash-browser.lib.js">

function main()
{
    var services, widgets;
    
    services = getHeaderServices();
    widgets = getHeaderModel(msg.get('better-trash-management.trash-browser.page.title'));

    // TODO Enhance model with services + widgets we need (call into *.lib.js)
    
    model.jsonModel = getFooterModel(services, widgets);
    model.jsonModel.groupMemberships = user.properties.alfUserGroups;
}

main();