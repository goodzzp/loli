package org.loli.service.html

import org.loli.base.RpcCall
import org.loli.html.CssUtil
import org.loli.html.HtmlUtil
import org.loli.json.RpcJsonPropNames
import org.loli.service.RpcConf
import org.loli.util.RpcAnnotationUtil
import kotlin.reflect.KClass

/**
 * 类的信息
 */
internal object ClassHtml {
    /**
     * 根据方法名称，得到html信息
     * @param serviceName 服务名
     * @param className 类名
     * @param conf 配置
     * @return html信息
     */
    fun getHtml(serviceName: String, className: String, conf: RpcConf): String {
        val methods = RpcCall.getMethod(className)

        val buf = StringBuilder()
        buf.append(
            """
            <head>
            ${CssUtil.CSS_TABLE}
            <title>$serviceName:$className 类信息</title>
            </head>
            <body>
            <center>
        """.trimIndent()
        )

        ServiceHtml.appendServiceHtml(buf, serviceName, conf.description, conf.version)
        appendClassHtml(buf, className, methods[0].clsInfo)

        buf.append(
            """
            <table class="customers">
            <tr>
                <th>方法名</th>
                <th>描述</th>
                <th>定义</th>
            </tr>
        """.trimIndent()
        )

        val sortedMethods = methods.sortedWith(Comparator { obj1, obj2 ->
            obj1.method.name.compareTo(obj2.method.name)
        })

        sortedMethods.forEachIndexed { index, method ->
            val methodDesc = RpcAnnotationUtil.getMethodDesc(method.method)
            val methodDescHtml = HtmlUtil.formatMethodDesc(methodDesc)
            var methodExportDesc = RpcAnnotationUtil.getDesc(method.method)
            if(method.needAuth){
                methodExportDesc = "(需要鉴权) $methodExportDesc"
            }
            methodExportDesc = HtmlUtil.escape(methodExportDesc)
            buf.append(
                """
                <tr ${if((index%2)==1) "class=high_light" else ""}>
                    <td width="15%"><a target='_blank' href='?${RpcJsonPropNames.METHOD}=$serviceName:$className:${method.methodOutName}'>
                        ${method.methodOutName}</a>
                    </td>
                    <td width="35%">$methodExportDesc</td>
                    <td>$methodDescHtml</td>
                </tr>
            """.trimIndent()
            )
        }
        buf.append("</table></center></body>")

        return buf.toString()
    }

    /**
     * 添加class对应的html
     */
    fun appendClassHtml(buf: StringBuilder, className: String, classDef: KClass<*>){
        buf.append(
            """
            <table class="customers">
                <tr>
                    <th>类名</th>
                    <th>描述</th>
                    <th>定义</th>
                </tr>
                <tr>
                    <td width="15%">$className</td>
                    <td width="35%">${RpcAnnotationUtil.getDesc(classDef)}</td>
                    <td>${classDef.qualifiedName}</td>
                </tr>
            </table>
            <p>
        """.trimIndent()
        )
    }
}