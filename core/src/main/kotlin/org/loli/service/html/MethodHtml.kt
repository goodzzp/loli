package org.loli.service.html

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.GsonBuilder
import org.loli.base.RpcCall
import org.loli.base.RpcException
import org.loli.html.CssUtil
import org.loli.html.HtmlUtil
import org.loli.json.RpcJsonPropNames
import org.loli.service.RpcConf
import org.loli.util.RpcAnnotationUtil
import org.loli.util.RpcMethodDescription
import kotlin.reflect.KClass

/**
 * 方法的信息
 */
internal object MethodHtml {
    /**
     * 根据方法名称，得到html信息
     * @param serviceName 服务名
     * @param className 类名
     * @param methodName 方法名
     * @param conf 配置
     * @return html信息
     */
    fun getHtml(serviceName: String, className: String, methodName: String, conf: RpcConf): String {
        try {
            val methodInfo = RpcCall.getMethod(className, methodName)
            val method = methodInfo.method
            val clsDef = methodInfo.clsInfo
            val methodDesc = RpcAnnotationUtil.getMethodDesc(method)
            val methodDescHtml = HtmlUtil.formatMethodDesc(methodDesc)

            val buf = StringBuilder()

            // highlight.js 用来做代码标红和格式化
            buf.append(
                """
                <head>
                ${CssUtil.CSS_TABLE}
                <title>$serviceName:$className:$methodName 方法信息</title>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.15.6/styles/default.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.15.6/highlight.min.js"></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/languages/kotlin.min.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>

                <script>
                    function send(txt_area,txt_result){
                        var xhr = new XMLHttpRequest();
                        var start = new Date().getTime();
                        xhr.onreadystatechange = function () {
                          if(xhr.readyState === XMLHttpRequest.DONE){
                              if(xhr.status === 200) {
                                // 对json进行漂亮化处理
                                txt_result.textContent = "耗时: " + (new Date().getTime() - start) + " ms\n" + JSON.stringify(JSON.parse(xhr.responseText), null, 4)
                                hljs.highlightBlock(txt_result);
                              }else{
                                txt_result.innerHTML = "读取数据错误，返回状态码：" + xhr.status;
                              }
                          }
                        }
                        var path = document.getElementById("path_text").value
                        xhr.open("POST",path,true);
                        xhr.setRequestHeader("Content-type","application/json; charset=UTF-8");
                        xhr.send(txt_area.value);
                    }
                </script>

                </head>
                <body>
                <center>
            """.trimIndent()
            )

            ServiceHtml.appendServiceHtml(buf, serviceName, conf.description, conf.version)
            appendClassHtml(buf, serviceName, className, clsDef)
            appendMethodHtml(buf, methodName, methodInfo, methodDescHtml)
            buf.append("</center>")

            appendHowToCall(methodDesc, serviceName, className, methodName, conf, buf)

            return buf.toString()
        } catch (e: RpcException) {
            return e.message!!
        }
    }

    /**
     * 添加如何调用的html
     */
    private fun appendHowToCall(
        methodDesc: RpcMethodDescription,
        serviceName: String,
        className: String,
        methodName: String,
        conf: RpcConf,
        buf: StringBuilder
    ) {
        val req = getRequestJson(methodDesc)
        val reqReal = getReqRealJson(methodDesc, "$serviceName:$className:$methodName")
        val resp = getResponseJson(methodDesc)
        val (requestJson, responseJson) = alignLines(req, resp)
        buf.append(
            """
    <div style='width: 90%; margin: 0 auto; clear:both;'>
        <div style='width:50%; float:left;'>
            <h2>请求Json</h2>
            <pre><code class="json" style='width:95%'>
$requestJson
            </code></pre>
        </div>
        <div style='width:50%; float:left;'>
            <h2>返回Json</h2>
            <pre><code class="json" style='width:95%'>
$responseJson
            </code></pre>
        </div>
    </div>
    <p>
    <div style='width: 90%; margin: 0 auto; clear:both;'>
        <div style='width:50%; float:left;margin-top:20px'>
            请求url: <input type="text" id="path_text" value="${conf.servicePath}" style='width:60%;margin:5px'>
            <button onclick='send(document.getElementById("txt_json"),document.getElementById("result_json"))'
            style="width: 120px; height: 40px; font-size: 20px">测试Json</button>
            <textarea id="txt_json" style='width:96%;height:auto;margin-top:15px' rows=${requestJson.split('\n').size}>
$reqReal
            </textarea>
        </div>
        <div style='width:50%; float:left;'>
            <h2>测试结果</h2>
            <pre><code class="json" id="result_json" style='width:95%'>
            </code></pre>
        </div>
    </div>
                """.trimIndent()
        )
    }

    /**
     * 添加class对应的html
     */
    fun appendClassHtml(buf: StringBuilder, serviceName: String, className: String, classDef: KClass<*>){
        buf.append(
            """
            <table class="customers">
                <tr>
                    <th>类名</th>
                    <th>描述</th>
                    <th>定义</th>
                </tr>
                <tr>
                    <td width="15%"><a target='_blank' href='?${RpcJsonPropNames.METHOD}=$serviceName:$className'>$className</a></td>
                    <td width="35%">${HtmlUtil.escape(RpcAnnotationUtil.getDesc(classDef))}</td>
                    <td>${classDef.qualifiedName}</td>
                </tr>
            </table>
            <p>
        """.trimIndent()
        )
    }

    /**
     * 添加method对应的html
     */
    private fun appendMethodHtml(buf: StringBuilder, methodName: String, method: RpcCall.MethodInfo, methodDescHtml: String) {
        var methodExportDesc = RpcAnnotationUtil.getDesc(method.method)
        if(method.needAuth){
            methodExportDesc = "(需要鉴权) $methodExportDesc"
        }
        methodExportDesc = HtmlUtil.escape(methodExportDesc)

        buf.append(
            """
                <table class="customers">
                    <tr>
                        <th>方法名</th>
                        <th>描述</th>
                        <th>定义</th>
                    </tr>
                    <tr>
                        <td width="15%">$methodName</td>
                        <td width="35%">$methodExportDesc</td>
                        <td>$methodDescHtml</td>
                    </tr>
                </table>
            <p>
        """.trimIndent()
        )
    }

    /**
     * 对齐行数
     */
    private fun alignLines(txtA: String, txtB: String): Pair<String, String> {
        val aLineNum = txtA.split('\n').size
        val bLineNum = txtB.split('\n').size
        val maxNum = Math.max(aLineNum, bLineNum)
        val bufA = StringBuilder(txtA)
        repeat(maxNum - aLineNum) {
            bufA.append('\n')
        }
        val bufB = StringBuilder(txtB)
        repeat(maxNum - bLineNum) {
            bufB.append('\n')
        }
        return Pair(bufA.toString(), bufB.toString())
    }

    /**
     * 生成请求的json
     */
    private fun getRequestJson(methodDesc: RpcMethodDescription): String {
        val params = jsonObject()
        MethodJsonUtil.toJsonDescription(methodDesc.params, params)
        val json = jsonObject(
            RpcJsonPropNames.METHOD to "服务:类:方法",
            RpcJsonPropNames.CONTEXT to jsonObject(
                RpcJsonPropNames.DEBUG to "是否debug模式，true/false，可省略",
                RpcJsonPropNames.SEQUENCE to "Long，序列号，被调用者原样返回，可省略",
                RpcJsonPropNames.TOKEN to "String，令牌信息，可省略",
                RpcJsonPropNames.VERSION to "String，版本，可省略",
                RpcJsonPropNames.SOURCE to jsonObject(
                    RpcJsonPropNames.TYPE to "String, 调用方类型，如小程序、app，可省略"
                )
            ),
            RpcJsonPropNames.PARAMS to params
        )
        return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json)
    }

    /**
     * 生成实际调用的json
     */
    private fun getReqRealJson(methodDesc: RpcMethodDescription, method: String): String {
        val params = jsonObject()
        MethodJsonUtil.toJsonCall(methodDesc.params, params)
        val json = jsonObject(
            RpcJsonPropNames.METHOD to method,
            RpcJsonPropNames.CONTEXT to jsonObject(
                RpcJsonPropNames.DEBUG to false,
                RpcJsonPropNames.TOKEN to "",
                RpcJsonPropNames.SOURCE to jsonObject(
                    RpcJsonPropNames.TYPE to "web"
                )
            ),
            RpcJsonPropNames.PARAMS to params
        )
        return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json)
    }

    /**
     * 生成返回的json
     */
    private fun getResponseJson(method: RpcMethodDescription): String {
        val json = jsonObject(
            RpcJsonPropNames.STATUS to "Int，状态，0表示成功，其他表示出错",
            RpcJsonPropNames.STATUS_INFO to "String, 状态信息",
            RpcJsonPropNames.CONTEXT to jsonObject(
                RpcJsonPropNames.SOURCE to jsonObject(
                    RpcJsonPropNames.TYPE to "来源，原样返回",
                    RpcJsonPropNames.CHILDREN to "List，调用链"
                )
            )
        )

        MethodJsonUtil.toReturnJsonDescription(method.ret, method.returnDesc, json)
        return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json)
    }
}