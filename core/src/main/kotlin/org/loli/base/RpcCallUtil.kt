package org.loli.base

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.loli.json.RpcJsonPropNames
import org.loli.service.AbsRpc

object RpcCallUtil {

    /**
     * 得到最后一个孩子节点
     */
    private fun getLast(json: JsonObject): Pair<Int, JsonObject>{
        var step = 0
        var ret = json
        while(ret.has(RpcJsonPropNames.CHILDREN)){
            val cs = ret[RpcJsonPropNames.CHILDREN].asJsonArray
            ret = cs[cs.size() - 1].asJsonObject
            step ++
        }
        return Pair(step, ret)
    }

    /**
     * 添加调用链
     * @param request 请求
     * @param address 本服务器地址
     * @param type 本服务类型
     * @param version 版本
     */
    fun addCallLink(request: JsonObject, address: String, type: String, version: Int){
        if(! request.has(RpcJsonPropNames.CONTEXT)){
            request[RpcJsonPropNames.CONTEXT] = jsonObject(
                RpcJsonPropNames.SOURCE to jsonObject()
            )
        }
        val context = request[RpcJsonPropNames.CONTEXT].asJsonObject
        if(! context.has(RpcJsonPropNames.SOURCE)){
            context[RpcJsonPropNames.SOURCE] = jsonObject()
        }

        // 添加当前模块到调用链
        val source = context[RpcJsonPropNames.SOURCE].asJsonObject
        val (_, last) = getLast(source)
        val array = jsonArray()
        array.add(jsonObject(
            RpcJsonPropNames.ADDRESS to address,
            RpcJsonPropNames.TYPE to type,
            RpcJsonPropNames.VERSION to version
        ))
        last[RpcJsonPropNames.CHILDREN] = array
    }

    /**
     * 建立返回数据
     * @param request 请求
     * @param rpc 本地服务类
     * @param jsonResult 实际返回
     */
    fun createReturn(request: JsonObject?, rpc: AbsRpc?, jsonResult: Any): JsonObject{
        val ret = jsonObject(
            RpcJsonPropNames.STATUS to (rpc?.retStatus ?: RpcReturnStatus.OK.first),
            RpcJsonPropNames.STATUS_INFO to (rpc?.retStatusInfo ?: "")
        )

        val context = jsonObject()
        var curSource : JsonObject? = null
        request?.get(RpcJsonPropNames.CONTEXT)?.asJsonObject?.let{
            // 只保留sequence和source
            if(it.has(RpcJsonPropNames.SEQUENCE)){
                context[RpcJsonPropNames.SEQUENCE] = it[RpcJsonPropNames.SEQUENCE]
            }
            if(it.has(RpcJsonPropNames.SOURCE)){
                context[RpcJsonPropNames.SOURCE] = it[RpcJsonPropNames.SOURCE]
                val (step, last) = getLast(context[RpcJsonPropNames.SOURCE].asJsonObject)
                curSource = last
                rpc?.combineSubCallLink(step)?.let { sub ->
                    if(sub.size() > 0){
                        curSource!![RpcJsonPropNames.CHILDREN] = sub
                    }
                }
            }
        }
        ret[RpcJsonPropNames.CONTEXT] = context

        // 添加debug信息
        rpc?.let {
            if(it.debug && it.debugInfo.isNotEmpty() && curSource != null){
                curSource!![RpcJsonPropNames.DEBUG_INFO] = Gson().toJsonTree(rpc.debugInfo)
            }
        }

        // 返回结果
        ret[RpcJsonPropNames.RETURN] = Gson().toJsonTree(jsonResult)

        return ret
    }

    /**
     * 建立返回数据
     * @param request 请求
     * @param retStatus 返回码
     * @param retStatusInfo 返回信息
     */
    fun createReturn(request: JsonObject?, retStatus: Int, retStatusInfo: String): JsonObject{
        val ret = jsonObject(
            RpcJsonPropNames.STATUS to retStatus,
            RpcJsonPropNames.STATUS_INFO to retStatusInfo
        )

        val context = jsonObject()
        request?.get(RpcJsonPropNames.CONTEXT)?.asJsonObject?.let{
            // 只保留sequence和source
            if(it.has(RpcJsonPropNames.SEQUENCE)){
                context[RpcJsonPropNames.SEQUENCE] = it[RpcJsonPropNames.SEQUENCE]
            }
            if(it.has(RpcJsonPropNames.SOURCE)){
                context[RpcJsonPropNames.SOURCE] = it[RpcJsonPropNames.SOURCE]
            }
        }
        ret[RpcJsonPropNames.CONTEXT] = context

        return ret
    }
}