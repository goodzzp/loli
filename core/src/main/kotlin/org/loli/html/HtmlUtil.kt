package org.loli.html

import org.loli.util.RpcAnnotationUtil
import org.loli.util.RpcMethodDescription

/**
 * html常用方法
 */
internal object HtmlUtil {
    /**
     * 字符到转义字符串
     */
    private val charToEscape = HashMap<Char, String>()

    init {
        charToEscape['"'] = "&quot;"
        charToEscape['&'] = "&amp;"
        charToEscape['<'] = "&lt;"
        charToEscape['>'] = "&gt;"
        charToEscape[' '] = "&nbsp;"
        charToEscape['\n'] = "<br>"
    }

    /**
     * 把文本根据html要求进行转义
     * @param txt 文本
     * @return html文件
     */
    fun escape(txt: String): String{
        val buf = StringBuilder()
        txt.forEach { c ->
            if(charToEscape.containsKey(c)){
                buf.append(charToEscape[c])
            }else{
                buf.append(c)
            }
        }
        return buf.toString()
    }

    /**
     * 根据方法描述，计算一个更可读的格式
     *
     * 参考：
     *```
     * add(a,b,c)
     *     a: Int, 第一个参数
     *     返回: Int, 所有参数的和
     *```
     * @param method 方法定义
     * @return 文本
     */
    fun formatMethodDesc(method: RpcMethodDescription): String{
        val buf = StringBuilder()

        // 方法体
//        buf.append(method.name).append('(')
//        for(param in method.params){
//            buf.append(param.name).append(',')
//        }
//        if(method.params.size > 0){
//            buf.deleteCharAt(buf.length - 1)
//        }
//        buf.append(")\n")

        buf.append("<table class='tbl_method_desc'>")

        // params
        method.params.forEach {
            val typeName = RpcAnnotationUtil.simplifyTypeName(it.type.toString())
            buf.append("<tr><td class='td_name'>${it.name}</td><td>${escape(typeName)}, ${escape(
                it.desc
            )}</td></tr>")
        }

        // return
        if(method.returnType.classifier != Unit::class) {
            val typeName = RpcAnnotationUtil.simplifyTypeName(method.returnType.toString())
            buf.append("<tr><td class='td_name'>返回</td><td>${escape(typeName)}, ${escape(
                method.returnDesc
            )}</td></tr>")
        }

        buf.append("</table>")

        return buf.toString()
    }
}