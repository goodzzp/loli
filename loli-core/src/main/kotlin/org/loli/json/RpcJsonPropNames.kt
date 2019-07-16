package org.loli.json

/**
 * 用于rpc的标准属性名称
 */
object RpcJsonPropNames {
    /**
     * 请求 方法
     */
    const val METHOD = "method"
    /**
     * 参数列表
     */
    const val PARAMS = "params"
    /**
     * 返回结果
     */
    const val RETURN = "return"
    /**
     * 返回状态码，0表示正常，其他非正常
     */
    const val STATUS = "status"
    /**
     * 返回状态码对应的信息
     */
    const val STATUS_INFO = "status_info"
    /**
     * 上下文
     */
    const val CONTEXT = "context"
    /**
     * 来源
     */
    const val SOURCE = "source"
    /**
     * 孩子
     */
    const val CHILDREN = "children"
    /**
     * 地址
     */
    const val ADDRESS = "address"
    /**
     * 类型
     */
    const val TYPE = "type"
    /**
     * 序列号
     */
    const val SEQUENCE = "sequence"
    /**
     * 版本
     */
    const val VERSION = "version"
    /**
     * 当前跳数
     */
    const val HOPS = "hops"
    /**
     * 是否在线调试
     */
    const val DEBUG = "debug"
    /**
     * 调试信息
     */
    const val DEBUG_INFO = "debug_info"
    /**
     * 令牌信息
     */
    const val TOKEN = "token"
}