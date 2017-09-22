<#compress><#escape x as jsonUtils.encodeJSONString(x)>
{
    "startIndex" : ${pagination.startIndex?c},
    "totalRecords" : ${pagination.totalRecords?c},
    "numberFound" : ${pagination.numberFound?c},
    "items": [<#list results as item>
        {
            <#assign version = "1.0">
            <#if item.hasAspect("cm:versionable")><#assign version = item.properties["cm:versionLabel"]!""></#if>
            
            "nodeRef": "${item.nodeRef}",
            "name": "${item.name}",
            "nodeType": "${item.type}",
            "nodeTypeShort": "${shortQName(item.type)}",
            <#-- simple type - UI in some parts does hard-coded value comparisons -->
            "type": "${item.isContainer?string('folder', item.isDocument?string('document', 'node'))}",
            "version": "${version}",
            <#if item.isDocument>
                "mimetype": "${item.mimetype!""}",
                "size": ${item.size?c},
                "contentUrl": "api/node/content/${item.storeType}/${item.storeId}/${item.id}/${item.name?url}",
            </#if>
            "properties": {
                <#assign lastRendered = false />
                <#list item.properties?keys as key>
                    <#if item.properties[key]??><#if lastRendered == true>,</#if>
                        "${shortQName(key)}" : <@renderProperty key item.properties[key] />
                    <#assign lastRendered = true />
                <#else>
                    <#assign lastRendered = false />
                </#if>
           </#list>
            }
        }<#if item_has_next>,</#if>
    </#list>]
}
</#escape></#compress>

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