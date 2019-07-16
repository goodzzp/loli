package org.loli.service

import com.github.salomonbrys.kotson.forEach
import com.google.gson.*
import io.ktor.application.ApplicationCall
import org.loli.auth.AuthClient
import org.loli.base.RpcCall
import org.loli.base.RpcCallUtil
import org.loli.base.RpcException
import org.loli.base.RpcReturnStatus
import org.loli.json.RpcJsonPropNames
import org.loli.util.KotlinReflectUtil
import org.loli.util.toString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import kotlin.collections.set
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.javaType

/**
 * 根据json文本来调用方法，返回同样是json文本
 */
object RemoteCallJson {
    /**
     * 日志
     */
    private val log: Logger = LoggerFactory.getLogger(RemoteCallJson::class.java)

    /**
     * 根据json文本来调用方法
     * @param call http请求
     * @param json json文本
     * @param conf 配置
     * @return json表示的结果
     */
    suspend fun call(call: ApplicationCall, json: String, conf: RpcConf): String? {
        var retStatus = 0
        var retStatusInfo: String? = null
        var request: JsonObject? = null
        var response: JsonObject? = null
        try {
            request = JsonParser().parse(json) as JsonObject

            // 添加调用链
            val type =  if(request.has(RpcJsonPropNames.METHOD)) request[RpcJsonPropNames.METHOD].asString else conf.service
            RpcCallUtil.addCallLink(request, "${conf.outHost}:${conf.port}", type, conf.version)

            // 拦截器处理
            conf.intercept.forEach {
                val userResp = it.startCall(call, json, request)
                if(! userResp.first){
                    return userResp.second?.toString()
                }
                request = userResp.second ?: request
            }

            // 方法调用
            val (jsonResult, rpcService) = call(request!!)
            response = RpcCallUtil.createReturn(request!!, rpcService, jsonResult)
        }catch (e: InvocationTargetException) {
            retStatus = RpcReturnStatus.ERROR.first
            retStatusInfo = toString(e.targetException)
        } catch (e: JsonParseException) {
            retStatus = RpcReturnStatus.ERROR.first
            retStatusInfo = toString(e)
        }catch (e: RpcException.AuthError){
            retStatus = AuthClient.AUTH_ERROR.first
            retStatusInfo = AuthClient.AUTH_ERROR.second
        }catch (e: RpcException) {
            retStatus = RpcReturnStatus.ERROR.first
            retStatusInfo = e.message
        }
        catch (e: Throwable) {
            retStatus = RpcReturnStatus.ERROR.first
            retStatusInfo = toString(e)
        }

        // 如果出错，则建立一个新的返回对象
        response = response ?: RpcCallUtil.createReturn(request, retStatus, retStatusInfo!!)
        try {
            // 拦截器处理
            var realResp: JsonObject? = response
            conf.intercept.forEach {
                val userResp = it.endCall(call, json, request, realResp)
                if(! userResp.first){
                    return userResp.second?.toString()
                }
                realResp = userResp.second ?: realResp
            }

            return realResp.toString()
        }
        catch (e: Throwable) {
            log.error(toString(e))
        }
        return response.toString()
    }

    /**
     * 根据json来调用方法
     * @param request json对象
     * @return (json对象, 调用对象)
     */
    private suspend fun call(request: JsonObject): Pair<JsonElement, AbsRpc> {
        val (className, methodName) = getCaller(request)
        val methodInfo = RpcCall.getMethod(className, methodName)
        val clsDef = methodInfo.clsInfo
        val method = methodInfo.method
        val paramValues = HashMap<KParameter, Any?>()

        val params = getParams(request)
        val context: JsonObject? = request.get(RpcJsonPropNames.CONTEXT)?.asJsonObject
        val debug = context?.get(RpcJsonPropNames.DEBUG)?.asBoolean ?: false
        val token = context?.get(RpcJsonPropNames.TOKEN)?.asString ?: ""

        val obj = clsDef.createInstance()
        if(obj is AbsRpc){
            // 设置context
            KotlinReflectUtil.setProperty(obj::context, obj, context)

            // 设置是否debug模式
            if(debug){
                KotlinReflectUtil.setProperty(obj::debug, obj, true)
            }

            // 设置token
            if(methodInfo.needAuth){
                val auth = AuthClient.auth(token) ?: throw RpcException.AuthError("auth failed for $token")
                KotlinReflectUtil.setProperty(obj::tokenInfo, obj, auth)
            }
        }
        else {
            throw RpcException("$clsDef not extends ${AbsRpc::class}")
        }

        // 第一个参数：必须为实例
        paramValues[method.parameters[0]] = obj
        // 其他参数
        params.forEach { name, jsonElement ->
            for (funParam in method.parameters) {
                if (funParam.name == name) {
                    if(jsonElement is JsonNull){
                        paramValues[funParam] = null
                    }
                    else {
                        try {
                            paramValues[funParam] = Gson().fromJson(jsonElement, funParam.type.javaType)
                        }catch (e: JsonParseException){
                            throw RpcException("${funParam.name}格式错误: ${e.message}")
                        }
                    }
                }
            }
        }

        // 调用方法
        checkParams(method, paramValues)
        val retObject: Any? = if(method.isSuspend) method.callSuspendBy(paramValues) else method.callBy(paramValues)
        return Pair(Gson().toJsonTree(retObject), obj)
    }

    /**
     * 检查方法的参数是否足够（不足够则抛出异常）
     * @param method 方法
     * @param params 参数
     */
    private fun checkParams(method: KCallable<*>, params: Map<KParameter, Any?>){
        for(param in method.parameters){
            if(param.name == null){
                continue
            }
            if(! params.containsKey(param)){
                if(! param.isOptional){
                    throw RpcException("param '${param.name}' of method '${method.name}' is missing")
                }
            }
        }
    }

    /**
     * 得到参数
     */
    private fun getParams(request: JsonObject): JsonObject {
        // 请求参数"params":xxx
        if (!request.has(RpcJsonPropNames.PARAMS)) {
            throw RpcException("property '${RpcJsonPropNames.PARAMS}' not found")
        }
        return request[RpcJsonPropNames.PARAMS].asJsonObject
    }

    /**
     * 得到请求call
     */
    private fun getCaller(request: JsonObject): Pair<String, String> {
        val className: String
        val methodName: String

        // 请求方法"call":"service:class:method"
        if (!request.has(RpcJsonPropNames.METHOD)) {
            throw RpcException("property '${RpcJsonPropNames.METHOD}' not found")
        }

        val call = request[RpcJsonPropNames.METHOD].asString
        val ks = call.split(':')
        if (ks.size == 3) {
            className = ks[1]
            methodName = ks[2]
        } else {
            throw RpcException("'${RpcJsonPropNames.METHOD}' is invalid, should be: service:class:method")
        }
        return Pair(className, methodName)
    }
}