package org.loli.base

import com.google.gson.JsonObject
import io.ktor.application.ApplicationCall

/**
 * rpc请求的拦截器
 */
interface IRpcIntercept {
    /**
     * 开始http请求
     * @param call http请求
     * @return 是否继续运行
     */
    suspend fun startHttp(call: ApplicationCall): Boolean {
        return true
    }

    /**
     * 调用call之前，当post数据为二进制时，用户可以在这里把它转化为string
     * @param call http请求
     * @return (是否继续运行，返回的字符串)
     */
    suspend fun beforeCall(call: ApplicationCall): Pair<Boolean, String?>{
        return Pair(true, null)
    }

    /**
     * 开始调用call
     * @param call http请求
     * @param txt 请求的json文本
     * @param req 请求的json对象
     * @return (是否继续运行，返回的json数据)
     */
    suspend fun startCall(call: ApplicationCall, txt: String?, req: JsonObject?): Pair<Boolean, JsonObject?>{
        return Pair(true, null)
    }

    /**
     * 结束调用call
     * @param call http请求
     * @param txt 请求的json文本
     * @param req 请求的json，为null表示未能解析出json
     * @param resp 返回的json
     * @return (是否继续运行，返回的json数据)
     */
    suspend fun endCall(call: ApplicationCall, txt: String?, req: JsonObject?, resp: JsonObject?): Pair<Boolean, JsonObject?>{
        return Pair(true, null)
    }
}