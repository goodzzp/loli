package org.loli.base

/**
 * 返回状态码
 */
object RpcReturnStatus {
    /**
     * 成功
     */
    val  OK = Pair(0, "")
    /**
     * 失败
     */
    val ERROR = Pair(-1, "")
}