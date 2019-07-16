package org.loli.util

import org.loli.base.RpcDesc
import org.loli.base.RpcExport
import org.loli.base.RpcReturn
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * 和注释有关的常用方法
 */
object RpcAnnotationUtil {

    /**
     * 得到暴露出去的描述
     * @param clsDef 类定义
     */
    fun getDesc(clsDef: KClass<*>): String{
        var ret = ""
        for(annotation in clsDef.annotations){
            if(annotation is RpcExport){
                ret = annotation.desc
                break
            }
        }
        return ret
    }

    /**
     * 得到暴露出去的描述
     * @param method 方法定义
     */
    fun getDesc(method: KCallable<*>): String{
        var ret = ""
        for(annotation in method.annotations){
            if(annotation is RpcExport){
                ret = annotation.desc
                break
            }
        }
        return ret
    }

    /**
     * 根据类定义得到详细描述，这里类最好是data class
     */
    fun getClassDesc(clsDef: KClass<*>) : RpcClassDescription?{
        // 描述
        var desc = ""
        for(annotation in clsDef.annotations) {
            if (annotation is RpcDesc) {
                desc = annotation.desc
            }
        }
//        if(desc.isEmpty()) {
//            throw RpcException.ClassDefineError("class $clsDef should be marked by @RpcDesc")
//        }

        val ret = RpcClassDescription(clsDef.qualifiedName!!, desc)

        if(clsDef.isData) {
            // 主构造函数
            val construct = clsDef.primaryConstructor!!
            construct.parameters.filter { it.name != null }.forEach { param ->
                ret.propNames.add(param.name!!)
                ret.propTypes.add(simplifyTypeName(param.type.toString()))

                var paramDesc = ""
                param.annotations.filterIsInstance<RpcDesc>().forEach {
                    paramDesc = it.desc
                }
                ret.propDescs.add(paramDesc)
                ret.props.add(param)
            }
        }

        return ret
    }

    /**
     * 根据方法定义得到详细描述
     */
    fun getMethodDesc(method: KCallable<*>): RpcMethodDescription{
        // 方法和返回描述
        var desc = ""
        var returnDesc = ""
        for(annotation in method.annotations){
            if(annotation is RpcExport){
                desc = annotation.desc
            }
            else if(annotation is RpcReturn){
                returnDesc = annotation.desc
            }
        }

        // 返回类型
//        val returnType = simplifyTypeName(method.returnType.toString())

        // 返回
        val ret = RpcMethodDescription(method.name, desc, method.returnType, returnDesc, method.returnType)

        // params
        method.parameters.filter { it.name != null }.forEach {param ->
            var paramDesc = ""
            param.annotations.filterIsInstance<RpcDesc>().forEach { annotation ->
                paramDesc = annotation.desc
            }
            ret.params.add(RpcMethodDescription.ParamInfo(param.type, paramDesc, param.name!!))
        }
        return ret
    }

    /**
     * 简化类型名称，如kotlin.collections.List<kotlin.Int> => List<Int>
     * @param txt 类型
     */
    fun simplifyTypeName(txt: String) : String{
        // kotlin.collections.List<kotlin.Int> 切分为 kotlin.collections.List < kotlin.Int >
        val buf = StringBuilder()
        var prePos = -1
        txt.forEachIndexed { index, c ->
            if(c == '<' || c == '>' || c == ',' || c == ' '){
                if(prePos >= 0){
                    val segment = txt.substring(prePos, index)
                    buf.append(simplifyName(segment))
                }
                buf.append(c)
                prePos = -1
            }else{
                if(prePos < 0){
                    prePos = index
                }
            }
        }
        if(prePos >= 0){
            val segment = txt.substring(prePos, txt.length)
            buf.append(simplifyName(segment))
        }
        return buf.toString()
    }

    /**
     * 简化名称，如 kotlin.Int => Int, com.google.gson.JsonElement -> JsonElement
     */
    private fun simplifyName(name: String): String{
        var ret = name
        when(name){
            "com.google.gson.JsonElement" -> ret = "JsonElement"
            else ->{
                val ks = name.split('.')
                if(ks.size == 2){
                    if("kotlin" == ks[0]){
                        ret = ks[1]
                    }
                }
                else if(ks.size == 3){
                    if("kotlin" == ks[0]){
                        if("collections" == ks[1]){
                            ret = ks[2]
                        }
                    }else if("java" == ks[0]){
                        if("lang" == ks[1]){
                            ret = ks[2]
                        }
                    }
                }
            }
        }

        return ret
    }
}