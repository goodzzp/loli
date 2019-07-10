package org.loli.dispatch

/**
 * 节点地址
 */
data class NodeEndPoint(
    /**
     * host地址
     */
    val host: String,
    /**
     * 端口
     */
    val port: Int,
    /**
     * 提供service的url路径
     */
    val servicePath: String = "/",
    /**
     * 提供解释的url路径
     */
    val explainPath: String = "/explain",
    /**
     * 提供信息的url路径
     */
    val infoPath: String = "/info",
    /**
     * cpu个数
     */
    val cpu: Int = 1,
    /**
     * 最大队列长度
     */
    val maxQueue: Int = 1000,
    /**
     * 版本
     */
    val version: Int = 0
)