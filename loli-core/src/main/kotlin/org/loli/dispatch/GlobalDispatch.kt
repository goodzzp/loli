package org.loli.dispatch

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.loli.base.HttpClientByApache
import org.loli.base.RpcException
import org.loli.json.RpcJsonPropNames

/**
 * 服务调度器
 */
object GlobalDispatch {
    /**
     * 节点调度器
     */
    private val nodeDispatchs: MutableMap<String, NodeDispatch> = HashMap()
    /**
     * 同步器
     */
    private val mutex = Mutex()
    /**
     * api前缀
     */
    private const val API_PREFIX = "api."
    /**
     * service前缀
     */
    private const val SRV_PREFIX = "srv."

    /**
     * 更新节点
     * @param name 名称
     * @param endPoints 节点列表
     */
    private suspend fun update(name: String, endPoints: List<NodeEndPoint>) {
        var nodeDispatch: NodeDispatch? = null
        mutex.withLock {
            if(! nodeDispatchs.containsKey(name)){
                nodeDispatchs[name] = NodeDispatch()
            }
            nodeDispatch = nodeDispatchs[name]
        }
        nodeDispatch?.update(endPoints)
    }

    /**
     * 更新api节点
     * @param name 名称
     * @param endPoints 节点列表
     */
    suspend fun updateApi(name: String, endPoints: List<NodeEndPoint>) {
        update(API_PREFIX + name, endPoints)
    }

    /**
     * 更新service节点
     * @param name 名称
     * @param endPoints 节点列表
     */
    suspend fun updateSrv(name: String, endPoints: List<NodeEndPoint>) {
        update(SRV_PREFIX + name, endPoints)
    }

    /**
     * 选择一个节点
     * @param name 名称
     */
    private suspend fun select(name: String): NodeEndPoint {
        var nodeDispatch: NodeDispatch? = null
        mutex.withLock {
            if(! nodeDispatchs.containsKey(name)){
                throw RpcException("no endpoint for '$name'")
            }
            nodeDispatch = nodeDispatchs[name]
        }
        return nodeDispatch?.select(name)!!
    }

    /**
     * 选择一个api节点
     * @param name 名称
     */
    suspend fun selectApi(name: String): NodeEndPoint {
        return select(API_PREFIX + name)
    }

    /**
     * 选择一个service节点
     * @param name 名称
     */
    suspend fun selectSrv(name: String): NodeEndPoint {
        return select(SRV_PREFIX + name)
    }

    /**
     * 得到所有节点
     * @param name 名称
     * @return 节点地址列表
     */
    private suspend fun getAll(name: String): List<Pair<String, Int>> {
        var nodeDispatch: NodeDispatch? = null
        mutex.withLock {
            if(nodeDispatchs.containsKey(name)){
                nodeDispatch = nodeDispatchs[name]
            }
        }
        val ret = ArrayList<Pair<String, Int>>()
        nodeDispatch?.let { ret.addAll(it.getAll()) }
        return ret
    }

    /**
     * 得到所有api节点
     * @param name 名称
     * @return 节点地址列表
     */
    suspend fun getAllApi(name: String): List<Pair<String, Int>> {
        return getAll(API_PREFIX + name)
    }

    /**
     * 得到所有service节点
     * @param name 名称
     * @return 节点地址列表
     */
    suspend fun getAllSrv(name: String): List<Pair<String, Int>> {
        return getAll(SRV_PREFIX + name)
    }

    /**
     * 调用api层
     * @param T 返回数据类型
     * @param endPoint 目标节点
     * @param call 调用请求
     * @return 返回数据
     */
    suspend inline fun <reified T: Any> call(endPoint: NodeEndPoint, call: CallReq): CallResp<T> {
        val json = jsonObject(
            RpcJsonPropNames.METHOD to call.call,
            RpcJsonPropNames.CONTEXT to (call.context ?: jsonObject()),
            RpcJsonPropNames.PARAMS to (if(call.params == null) jsonObject() else Gson().toJsonTree(call.params))
        )

        val callResult = CallResp<T>()
        try{
            val resp = HttpClientByApache.postJson("http://${endPoint.host}:${endPoint.port}${endPoint.servicePath}",  json.toString())
            callResult.headers = resp.first
            callResult.text = resp.second

            resp.second?.let {
                val retJson = JsonParser().parse(it).asJsonObject
                if(retJson.has(RpcJsonPropNames.STATUS)){
                    callResult.retStatus = retJson[RpcJsonPropNames.STATUS].asInt
                }
                if(retJson.has(RpcJsonPropNames.STATUS_INFO)){
                    callResult.retStatusInfo = retJson[RpcJsonPropNames.STATUS_INFO].asString
                }
                if(retJson.has(RpcJsonPropNames.CONTEXT)){
                    callResult.context = retJson[RpcJsonPropNames.CONTEXT].asJsonObject
                }
                if(retJson.has(RpcJsonPropNames.RETURN)){
                    callResult.data = Gson().fromJson(retJson[RpcJsonPropNames.RETURN])
                }
            }
        }catch (e: Throwable){
            callResult.error = e
        }
        return callResult
    }

    /**
     * 调用api层
     * @param T 返回数据类型
     * @param call 请求
     * @return 返回数据
     */
    suspend inline fun <reified T: Any> callApi(call: CallReq): CallResp<T> {
        val kvs = call.call.split(':')
        if(kvs.size != 3){
            throw IllegalArgumentException("call is invalid: $call")
        }

        return call(selectApi(kvs[0]), call)
    }

    /**
     * 调用service层
     * @param T 返回数据类型
     * @param call 请求
     * @return 返回数据
     */
    suspend inline fun <reified T: Any> callSrv(call: CallReq): CallResp<T> {
        val kvs = call.call.split(':')
        if(kvs.size != 3){
            throw IllegalArgumentException("call is invalid: $call")
        }

        return call(selectSrv(kvs[0]), call)
    }
}