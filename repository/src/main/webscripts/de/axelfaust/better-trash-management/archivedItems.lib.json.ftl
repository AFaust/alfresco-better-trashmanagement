<#macro renderArchivedItemsList results pagination><#compress><#escape x as jsonUtils.encodeJSONString(x)>
{
    "startIndex" : ${pagination.startIndex?c},
    "totalRecords" : ${pagination.totalRecords?c},
    "numberFound" : ${pagination.numberFound?c},
    "items": [<#list results as resultItem>
        {
            <#assign node = resultItem.node />
            <#assign version = "1.0">
            <#if node.hasAspect("cm:versionable")><#assign version = node.properties["cm:versionLabel"]!""></#if>
            
            "nodeRef": "${node.nodeRef}",
            "name": "${node.name}",
            "nodeType": "${node.type}",
            "nodeTypeShort": "${shortQName(node.type)}",
            <#-- simple type - UI in some parts does hard-coded value comparisons -->
            "type": "${node.isContainer?string('folder', node.isDocument?string('document', 'node'))}",
            "version": "${version}",
            <#if node.isDocument>
                "mimetype": "${node.mimetype!""}",
                "size": ${node.size?c},
                "contentUrl": "api/node/content/${node.storeType}/${node.storeId}/${node.id}/${node.name?url}",
            </#if>
            "modified": "${xmldate(node.properties.modified)}",
            "modifier": "${node.properties.modifier}",
            "modifierDisplayName": "${resultItem.modifier.displayName!""}",
            "archived": "${xmldate(resultItem.archivedOn)}",
            "archiver": "${resultItem.archiver.userName}",
            "archiverDisplayName": "${resultItem.archiver.displayName!""}",
            "displayPath": "${resultItem.displayPath!""}",
            "properties": {
                <#assign lastRendered = false />
                <#list node.properties?keys as key>
                    <#if node.properties[key]??>
                        <#if lastRendered == true>,</#if>"${shortQName(key)}" : <@renderProperty key node.properties[key] />
                    <#assign lastRendered = true />
                <#else>
                    <#assign lastRendered = false />
                </#if>
           </#list>
            }
        }<#if resultItem_has_next>,</#if>
    </#list>]
    <#nested />
}
</#escape></#compress></#macro>

<#macro renderProperty key value><#compress><#escape x as jsonUtils.encodeJSONString(x)>
        <#if value?is_sequence>
            [
                <#list value as element>
                <@renderProperty key element /><#if element_has_next>,</#if>
                </#list>
            ]
        <#-- rather elaborate check to ensure we really have a TemplateNode -->
        <#elseif value?is_hash && value.nodeRef?? && value.storeType??>
            <#-- TODO Deal with tags / categories -->
            "${value.nodeRef}"
        <#elseif value?is_hash && value.downloadUrl??>
            {
                "mimetype": "${value.mimetype}",
                "displayMimetype": "${value.displayMimetype!(value.mimetype)}",
                "size": ${value.size?c},
                "encoding": "${value.encoding}",
                "downloadUrl": "${value.downloadUrl}"
            }
        <#elseif value?is_date>
            "${xmldate(value)}"
        <#elseif value?is_string>
            <#if value?starts_with('{') && value?ends_with('}')>
            <#noescape>${value}</#noescape><#-- Trust that this is a valid JSON -->
            <#elseif value?matches('^\\{http://[^}]+\\}[^$]+$')>
            "${shortQName(value)}"
            <#else>
            "${value}"
            </#if>
        <#elseif value?is_number>
            ${value?c}
        <#elseif value?is_boolean>
            ${value?string}
        <#elseif value?is_hash>
            {
                <#local firstElement = true />
                <#list value?keys as subKey>
                <#if value[subKey]??><#if firstElement == false>,</#if>
                "${subKey}": <@renderProperty key value[subKey] />
                <#local firstElement = false />
                </#if>
                </#list>
            }
        <#else>
            ""
        </#if>
</#escape></#compress></#macro>