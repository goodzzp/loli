package org.loli.dispatch

import com.google.gson.JsonObject
import io.ktor.http.Headers

/**
 * 调用结果
 */
data class CallResp<T>(
    /**
     * 数据
     */
    var data: T? = null,
    /**
     * header头
     */
    var headers: Headers? = null,
    /**
     * 完整文本数据
     */
    var text: String? = null,
    /**
     * 执行错误
     */
    var error: Throwable? = null,
    /**
     * 返回状态
     */
    var retStatus: Int? = null,
    /**
     * 返回状态信息
     */
    var retStatusInfo: String? = null,
    /**
     * 上下文信息
     */
    var context: JsonObject? = null
)