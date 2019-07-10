package org.loli.dispatch

import com.google.gson.JsonObject

/**
 * 调用请求
 */
data class CallReq(
    /**
     * 调用服务方法
     */
    var call: String,
    /**
     * 上下文
     */
    var context: JsonObject? = null,
    /**
     * 参数
     */
    var params: Map<String, Any>? = null
)