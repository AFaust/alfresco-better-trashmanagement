<#include "/org/alfresco/share/page-templates/full-page-template.ftl" />
<@templateHeader />

<@templateBody>
    <div id="content">
        <#assign regionId = page.properties.webscriptURI?replace("/", "-")/>
        <@autoComponentRegion uri="${page.properties.webscriptURI}"/>
    </div>
</@>
