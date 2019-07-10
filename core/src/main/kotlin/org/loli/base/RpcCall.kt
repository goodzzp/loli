package org.loli.base

import com.google.gson.JsonElement
import org.loli.service.AbsRpc
import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubclassOf

/**
 * 根据名称来调用方法
 */
object RpcCall {
    /**
     * 日志
     */
    private val log: Logger = LoggerFactory.getLogger(RpcCall::class.java)
    /**
     * 类名，方法名 => 方法定义
     */
    private val nameToMethod = HashMap<Pair<String,String>, MethodInfo>()
    /**
     * 存在的类名称
     */
    private val existClass = HashSet<String>()

    /**
     * 检查类型是否满足约束：
     *
     * 基础类型：Double, Float, Long, Int, Short, Byte, Char, Boolean, String
     *
     * 集合类型：Map<String,Any>, List<Any>, Set<Any>
     *
     * JsonElement类型：com.google.gson.JsonElement
     *
     * 自定义：data class 类型（其内部属性类型也遵守本定义）, enum类型
     */
    private fun checkType(name: String, type: KType, checked: MutableSet<KType>){
        // 避免多次check同一个类型
        checked.add(type)

        var isOk = false
        val clsDef = type.classifier as KClass<*>
        when(clsDef){
            Unit::class -> isOk = true
            Double::class, Float::class, Long::class, Int::class -> isOk = true
            Short::class, Byte::class, Char::class, Boolean::class, String::class  -> isOk = true
            List::class, Set::class -> {
                val subtype = type.arguments[0].type!!
                if(! checked.contains(subtype)) {
                    checkType(name, subtype, checked)
                }
                isOk = true
            }
            Map::class -> {
                when(type.arguments[0].type!!.classifier){
                    Int::class, Long::class, String::class -> {}
                    else -> throw RpcException("$name type '${clsDef.qualifiedName}' is invalid, Map key type should by Int/Long/String")
                }
                val subtype = type.arguments[1].type!!
                if(! checked.contains(subtype)) {
                    checkType(name, subtype, checked)
                }
                isOk = true
            }
            JsonElement::class -> isOk = true
        }
        if(clsDef.java.isEnum){
            isOk = true
        }else if(clsDef.isData){
            clsDef.constructors.forEach { construct ->
                construct.parameters.forEach{
                    val subtype = it.type
                    if(! checked.contains(subtype)) {
                        checkType("'$clsDef' property '${it.name}'", subtype, checked)
                    }
                }
            }
            isOk = true
        }

        if(! isOk){
            throw RpcException("$name type '${clsDef.qualifiedName}' is invalid")
        }
    }

    /**
     * 检查函数参数的类型是否满足约束
     */
    private fun checkParamType(clsDef: KClass<*>, function: KFunction<*>){
        // 参数类型
        for(param in function.parameters){
            if(param.name == null){
                continue
            }
            checkType(
                "${clsDef.qualifiedName!!} method '${function.name}' param '${param.name!!}'",
                param.type,
                HashSet()
            )
        }
        // 返回类型
        checkType(
            "${clsDef.qualifiedName!!} method '${function.name}' return",
            function.returnType,
            HashSet()
        )
    }

    /**
     * 检查参数是否有@RpcDesc
     */
    private fun checkParams(clsDef: KClass<*>, function: KFunction<*>){
        for(param in function.parameters){
            if(param.name == null){
                continue
            }
            val num = param.annotations.filter { it is RpcDesc }.size
            if(num <= 0){
                throw RpcException(
                    "${clsDef.qualifiedName} method '${function.name}' " +
                            "param '${param.name}' has no @RpcDesc"
                )
            }
        }
    }

    /**
     * 注册包里面的所有类定义
     * @param pkgName 包名称
     */
    fun register(pkgName: String){
        val reflections = Reflections(pkgName)
        val subTypes = reflections.getSubTypesOf(AbsRpc::class.java)
        for(sub in subTypes){
            register(sub.kotlin)
        }
    }

    /**
     * 注册类定义
     * @param clsDef 类定义
     */
    fun register(clsDef: KClass<*>){
        if(! clsDef.isSubclassOf(AbsRpc::class)) {
            throw RpcException("$clsDef should extends ${AbsRpc::class}")
        }

        // class 对外名称
        var clsOutName : String? = null
        var clsAuth = true
        for(annotation in clsDef.annotations){
            when(annotation){
                is RpcExport -> clsOutName = if(annotation.name.isEmpty()) clsDef.qualifiedName!! else annotation.name
                is RpcAuth -> clsAuth = annotation.need
            }
        }
        if(clsOutName == null) {
            throw RpcException("$clsDef has no @RpcExport")
        }

        if(existClass.contains(clsOutName)){
//            throw RpcException("duplicate name '$clsOutName': ${clsDef.qualifiedName}")
            log.warn("duplicate class name '$clsOutName': ${clsDef.qualifiedName}")
        }else {
            existClass.add(clsOutName)
        }

        log.info("register rpc class: ${clsDef.qualifiedName} as $clsOutName")

        // 遍历方法
        val exists = HashSet<String>()
        clsDef.declaredMemberFunctions.forEach { member ->
//            var hasRpcReturn = false
            var methodOutName: String? = null
            var methodAuth = clsAuth

            for(annotation in member.annotations){
                when(annotation)
                {
                    is RpcExport -> {
                        val methodName = if (annotation.name.isEmpty()) member.name else annotation.name
                        if (exists.contains(methodName)) {
                            throw RpcException("duplicate method '$methodName' in $clsDef")
                        }
                        methodOutName = methodName
                        exists.add(methodName)

                        // 检查参数是否有@RpcDesc定义
                        checkParams(clsDef, member)
                    }
//                    is RpcReturn -> hasRpcReturn = true
                    is RpcAuth -> methodAuth = annotation.need
                }
            }
            methodOutName?.let {
                val key = Pair(clsOutName, it)
                if(nameToMethod.containsKey(key)){
                    throw RpcException("duplicate method '$it' in $clsDef")
                }
                nameToMethod[key] =
                    MethodInfo(clsDef, clsOutName, member, methodOutName, methodAuth)

//                // 有返回类型的方法都必须写上 @RpcReturn
//                if(member.returnType.classifier != Unit::class && ! hasRpcReturn){
//                    throw RpcException("${clsDef.qualifiedName} method '${member.name}' has no @RpcReturn")
//                }

                // 检测参数和返回的类型是否符合约束
                checkParamType(clsDef, member)
            }
        }

        if(nameToMethod.isEmpty()){
            throw RpcException("$clsDef has no export method")
        }
    }

    /**
     * 方法是否存在
     * @param className 类名
     * @param methodName 方法名
     */
    fun existMethod(className: String, methodName: String): Boolean{
        val key = Pair(className, methodName)
        return nameToMethod.containsKey(key)
    }

    /**
     * 得到方法定义
     * @param className 类名
     * @param methodName 方法名
     * @return 类定义 和 方法的定义
     */
    fun getMethod(className: String, methodName: String): MethodInfo {
        val key = Pair(className, methodName)
        return nameToMethod[key] ?: throw RpcException("service '$className' method '$methodName' not exist")
    }

    /**
     * 得到类下的所有方法
     * @param className 类名
     * @return 方法的定义
     */
    fun getMethod(className: String): List<MethodInfo> {
        val methods = ArrayList<MethodInfo>()
        for((key, value) in nameToMethod){
            if(className == key.first){
                methods.add(value)
            }
        }

        return methods
    }

    /**
     * 得到所有类
     * @return 所有类列表
     */
    fun getAllClass() : Map<String, KClass<*>>{
        val ret = TreeMap<String, KClass<*>>()
        for((key, value) in nameToMethod){
            ret[key.first] = value.clsInfo
        }
        return ret
    }

    /**
     * 方法信息
     */
    data class MethodInfo(
        /**
         * 类定义
         */
        val clsInfo: KClass<*>,
        /**
         * 类的对外名称
         */
        val clsOutName: String,
        /**
         * 方法定义
         */
        val method: KCallable<*>,
        /**
         * 方法的对外名称
         */
        val methodOutName: String,
        /**
         * 是否需要鉴权，默认为true
         */
        val needAuth: Boolean = true
    )
}