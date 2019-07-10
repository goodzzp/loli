package org.loli.service.html

import org.loli.base.RpcCall
import org.loli.html.CssUtil
import org.loli.html.HtmlUtil
import org.loli.json.RpcJsonPropNames
import org.loli.service.RpcConf
import org.loli.util.RpcAnnotationUtil

/**
 * 服务的html信息
 */
internal object ServiceHtml {
    /**
     * 根据服务名称，得到html信息
     * @param serviceName 服务名
     * @param conf 配置
     * @return html信息
     */
    fun getHtml(serviceName: String, conf: RpcConf): String {
        if(serviceName != conf.service){
            return "服务名不匹配，要求$serviceName，实际${conf.service}"
        }

        val buf = StringBuilder()
        buf.append(
            """
            <head>
            ${CssUtil.CSS_TABLE}
            <title>$serviceName 服务信息</title>
            </head>
                """.trimIndent()
        )

        appendServiceHtml(buf, serviceName, conf.description, conf.version)

        buf.append(
            """
            <p>
            <table class="customers">
                <tr>
                    <th>类名</th>
                    <th>描述</th>
                    <th>定义</th>
                </tr>
                """.trimIndent()
        )

        // 服务列表
        val services = RpcCall.getAllClass()
        for ((className, clsDef) in services) {
            buf.append(
                """
                <tr>
                    <td width="15%"><a target='blank' href='?${RpcJsonPropNames.METHOD}=$serviceName:$className'>$className</a></td>
                    <td width="35%">${HtmlUtil.escape(RpcAnnotationUtil.getDesc(clsDef))}</td>
                    <td>${clsDef.qualifiedName}</td>
                </tr>
        """.trimIndent()
            )
        }
        buf.append("</table></center></body>")

        return buf.toString()
    }

    /**
     * 添加服务对应的html
     */
    fun appendServiceHtml(buf: StringBuilder, serviceName: String, description: String, version: Int){
        buf.append(
            """
            <table class="customers">
                <tr>
                    <th>服务名</th>
                    <th>描述</th>
                    <th>版本</th>
                </tr>
                <tr>
                    <td width="15%"><a target='_blank' href='?${RpcJsonPropNames.METHOD}=$serviceName'>$serviceName</a></td>
                    <td width="35%">${HtmlUtil.escape(description)}</td>
                    <td>$version</td>
                </tr>
            </table>
            <p>
        """.trimIndent()
        )
    }
}