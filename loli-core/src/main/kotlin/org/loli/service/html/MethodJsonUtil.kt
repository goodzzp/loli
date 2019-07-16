package org.loli.service.html

import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import org.loli.base.RpcDesc
import org.loli.html.HtmlUtil
import org.loli.util.RpcAnnotationUtil
import org.loli.util.RpcMethodDescription
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/**
 * 方法转json的常用逻辑
 */
object MethodJsonUtil {
    /**
     * 用json描述参数列表
     * @param params 参数列表
     * @param ret 返回的json
     */
    fun toJsonDescription(params: List<RpcMethodDescription.ParamInfo>, ret: JsonElement) {
        params.forEach {
            val clsDef = it.type.classifier as KClass<*>
            val name = clsDef.qualifiedName!!
            if(clsDef == List::class || clsDef == Set::class){
                ret[it.name] = jsonArray(
                    toJsonDescription(it.type.arguments[0].type!!)
                )
            } else if(clsDef == Map::class){
                ret[it.name] =  jsonObject(
                    RpcAnnotationUtil.simplifyTypeName(it.type.arguments[0].type.toString()) to toJsonDescription(
                        it.type.arguments[1].type!!
                    )
                )
            } else if (name.startsWith("kotlin.") || name.startsWith("java.")
                || name == "com.google.gson.JsonElement") {
                ret[it.name] = HtmlUtil.escape("${RpcAnnotationUtil.simplifyTypeName(it.type.toString())}, ${it.desc}")
            } else if (clsDef.java.isEnum) {
                // 枚举类型
                val buf = StringBuilder()
                clsDef.java.declaredFields.forEach { field ->
                    var desc: String? = null
                    var outName: String? = field.name
                    field.annotations.forEach { annotation ->
                        if (annotation is RpcDesc) {
                            desc = annotation.desc
                        }else if(annotation is SerializedName){
                            outName = annotation.value
                        }
                    }
                    if (desc != null) {
                        buf.append("$outName($desc)、")
                    }
                }
                if (buf.isNotEmpty()) {
                    buf.deleteCharAt(buf.length - 1)
                }
                val enumDesc = RpcAnnotationUtil.getClassDesc(clsDef)
                ret[it.name] = HtmlUtil.escape("枚举类型, ${enumDesc?.desc}：$buf")
            } else if (clsDef.isData) {
                // 数据类型，读取构造函数
                val construct = clsDef.primaryConstructor!!
                ret[it.name] = jsonObject()
                toJsonDescription(
                    RpcAnnotationUtil.getMethodDesc(construct).params,
                    ret[it.name]
                )
            }
        }
    }

    /**
     * 把ktype变成json描述
     */
    fun toJsonDescription(params: KType): JsonElement {
        val clsDef = params.classifier as KClass<*>
        val name = clsDef.qualifiedName!!
        if(clsDef == List::class || clsDef == Set::class){
            return jsonArray(
                toJsonDescription(params.arguments[0].type!!)
            )
        } else if(clsDef == Map::class){
           return jsonObject(
             RpcAnnotationUtil.simplifyTypeName(params.arguments[0].type.toString()) to toJsonDescription(
                 params.arguments[1].type!!
             )
           )
        } else if (name.startsWith("kotlin.") || name.startsWith("java.")
            || name == "com.google.gson.JsonElement"
        ) {
            // 普通类型
            return JsonPrimitive(HtmlUtil.escape(RpcAnnotationUtil.simplifyTypeName(params.toString())))
        } else if (clsDef.java.isEnum) {
            // 枚举类型
            val buf = StringBuilder()
            clsDef.java.declaredFields.forEach { field ->
                var desc: String? = null
                var outName: String? = field.name
                field.annotations.forEach { annotation ->
                    if (annotation is RpcDesc) {
                        desc = annotation.desc
                    }else if(annotation is SerializedName){
                        outName = annotation.value
                    }
                }
                if (desc != null) {
                    buf.append("$outName($desc)、")
                }
            }
            if (buf.isNotEmpty()) {
                buf.deleteCharAt(buf.length - 1)
            }
            val enumDesc = RpcAnnotationUtil.getClassDesc(clsDef)
            return JsonPrimitive(HtmlUtil.escape("枚举类型, ${enumDesc?.desc}：$buf"))
        } else if (clsDef.isData) {
            // 数据类型，读取构造函数
            val construct = clsDef.primaryConstructor!!
            val ret = jsonObject()
            toJsonDescription(
                RpcAnnotationUtil.getMethodDesc(construct).params,
                ret
            )
            return ret
        }
        throw IllegalArgumentException("type '$params' not supported")
    }

    /**
     * 把ktype变成json描述
     */
    fun toJsonCallDescription(params: KType): JsonElement {
        val clsDef = params.classifier as KClass<*>
        val name = clsDef.qualifiedName!!
        if(clsDef == List::class || clsDef == Set::class){
            return jsonArray(
                toJsonCallDescription(params.arguments[0].type!!)
            )
        } else if(clsDef == Map::class){
            return jsonObject(
                toJsonCallDescription(params.arguments[0].type!!).asString  to toJsonCallDescription(
                    params.arguments[1].type!!
                )
            )
        } else if (name.startsWith("kotlin.") || name.startsWith("java.")
            || name == "com.google.gson.JsonElement"
        ) {
            return when (clsDef) {
                Double::class, Float::class, Long::class, Int::class -> JsonPrimitive(0)
                Short::class, Byte::class -> JsonPrimitive(0)
                Char::class -> JsonPrimitive("0")
                Boolean::class -> JsonPrimitive("false")
                String::class -> JsonPrimitive("")
                JsonElement::class -> jsonObject()
                else -> throw IllegalArgumentException("type '$params' not supported")
            }
        } else if (clsDef.java.isEnum) {
            // 枚举类型
            return JsonPrimitive(clsDef.java.enumConstants[0].toString())
        } else if (clsDef.isData) {
            // 数据类型，读取构造函数
            val construct = clsDef.primaryConstructor!!
            val ret = jsonObject()
            toJsonCall(RpcAnnotationUtil.getMethodDesc(construct).params, ret)
            return ret
        }
        throw IllegalArgumentException("type '$params' not supported")
    }

    /**
     * 把方法的return变成json描述
     * @param returnType 返回类型
     * @param returnDesc 返回描述
     * @param retJson json
     */
    fun toReturnJsonDescription(returnType: KType, returnDesc: String, retJson: JsonElement) {
        val clsDef = returnType.classifier as KClass<*>
        val name = clsDef.qualifiedName!!
        if(clsDef == List::class || clsDef == Set::class){
            retJson["return"] = jsonArray(
                toJsonDescription(returnType.arguments[0].type!!)
            )
        } else if(clsDef == Map::class){
            retJson["return"] =  jsonObject(
                RpcAnnotationUtil.simplifyTypeName(returnType.arguments[0].type.toString())  to toJsonDescription(
                    returnType.arguments[1].type!!
                )
            )
        } else if (name.startsWith("kotlin.") || name.startsWith("java.")
            || name == "com.google.gson.JsonElement") {
            retJson["return"] =
                HtmlUtil.escape("${RpcAnnotationUtil.simplifyTypeName(returnType.toString())}, $returnDesc")
        } else if (clsDef.java.isEnum) {
            // 枚举类型
            val buf = StringBuilder()
            clsDef.java.declaredFields.forEach { field ->
                var desc: String? = null
                var outName: String? = field.name
                field.annotations.forEach { annotation ->
                    if (annotation is RpcDesc) {
                        desc = annotation.desc
                    }else if(annotation is SerializedName){
                        outName = annotation.value
                    }
                }
                if (desc != null) {
                    buf.append("$outName($desc)、")
                }
            }
            if (buf.isNotEmpty()) {
                buf.deleteCharAt(buf.length - 1)
            }
            val enumDesc = RpcAnnotationUtil.getClassDesc(clsDef)
            retJson["return"] = HtmlUtil.escape("枚举类型, ${enumDesc?.desc}：$buf")
        } else if (clsDef.isData) {
            // 数据类型，读取构造函数
            val construct = clsDef.primaryConstructor!!
            retJson["return"] = jsonObject()
            toJsonDescription(
                RpcAnnotationUtil.getMethodDesc(construct).params,
                retJson["return"]
            )
        }
    }

    /**
     * 把方法用json描述，填充默认值
     * @param params 参数
     * @param ret json
     */
    fun toJsonCall(params: List<RpcMethodDescription.ParamInfo>, ret: JsonElement) {
        params.forEach {
            val clsDef = it.type.classifier as KClass<*>
            val name = clsDef.qualifiedName!!
            if(clsDef == List::class || clsDef == Set::class){
                ret[it.name] = jsonArray(
                    toJsonCallDescription(it.type.arguments[0].type!!)
                )
            } else if(clsDef == Map::class){
                ret[it.name] =  jsonObject(
                    toJsonCallDescription(it.type.arguments[0].type!!).asString to toJsonCallDescription(
                        it.type.arguments[1].type!!
                    )
                )
            } else if (name.startsWith("kotlin.") || name.startsWith("java.")
                ||  name == "com.google.gson.JsonElement") {
                ret[it.name] = when (clsDef) {
                    Double::class, Float::class, Long::class, Int::class -> 0
                    Short::class, Byte::class -> 0
                    Char::class -> 'a'
                    Boolean::class -> "false"
                    String::class -> ""
                    JsonElement::class -> jsonObject()
                    else -> throw IllegalArgumentException("type '$params' not supported")
                }
            } else if (clsDef.java.isEnum) {
                // 自定义的第一个值
                var value: String? = null
                clsDef.java.declaredFields[0].let { field ->
                    field.annotations.forEach { annotation ->
                        if(annotation is SerializedName){
                            value = annotation.value
                        }
                    }
                }
                ret[it.name] = value ?: clsDef.java.enumConstants[0].toString()
            } else if (clsDef.isData) {
                // 数据类型，读取构造函数
                val construct = clsDef.primaryConstructor!!
                ret[it.name] = jsonObject()
                toJsonCall(
                    RpcAnnotationUtil.getMethodDesc(construct).params,
                    ret[it.name]
                )
            }
        }
    }
}