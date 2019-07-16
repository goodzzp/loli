package org.loli.service

import com.github.salomonbrys.kotson.jsonArray
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.loli.json.RpcJsonPropNames
import java.util.*

/**
 * rpc的顶层父类
 */
abstract class AbsRpc {
    /**
     * 上下文信息
     */
    val context: JsonObject? = null
    /**
     * 请求包含的令牌信息
     */
    val tokenInfo: Any? = null
    /**
     * 是否debug模式
     */
    val debug: Boolean = false
    /**
     * 返回的debug信息
     */
    val debugInfo = LinkedList<String>()
    /**
     * 返回状态code（可不设置）
     */
    var retStatus: Int? = null
    /**
     * 返回状态信息（可不设置）
     */
    var retStatusInfo: String? = null
    /**
     * 返回的调用链
     */
    private val subCallLink = LinkedList<JsonObject?>()

    /**
     * 内部使用
     */
    fun addSubCallLink(source: JsonObject?){
        synchronized(subCallLink) {
            subCallLink.add(source)
        }
    }

    /**
     *  合并调用链
     *  @param step 跳过的路径长度
     */
    fun combineSubCallLink(step: Int): JsonArray {
        val jsonArray = jsonArray()
        subCallLink.filter { it != null }.forEach {
            var json = it !!
            var curStep = step
            while(curStep > 0){
                if(json.has(RpcJsonPropNames.CHILDREN)){
                    val cs = json[RpcJsonPropNames.CHILDREN].asJsonArray
                    json = cs[cs.size() - 1].asJsonObject
                    curStep --
                }else{
                    break
                }
            }
            if(curStep == 0 && json.has(RpcJsonPropNames.CHILDREN)){
                jsonArray.addAll(json[RpcJsonPropNames.CHILDREN].asJsonArray)
            }
        }
        return jsonArray
    }
}