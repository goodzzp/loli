package org.loli.service

import org.loli.base.IRpcIntercept

/**
 * 用于服务的配置
 */
open class RpcConf(
    /**
     * 服务名
     */
    var service: String = "noname",
    /**
     * 服务描述
     */
    var description: String = "no description",
    /**
     * 监听host
     */
    var host: String = "0.0.0.0",
    /**
     * 监听端口
     */
    var port: Int = 8900,
    /**
     * 对外host
     */
    var outHost: String = "127.0.0.1"
){
    /**
     * 提供service的url路径
     */
    var servicePath = "/"
    /**
     * 提供解释的url路径
     */
    var explainPath = "/explain"
    /**
     * 提供信息的url路径
     */
    var infoPath = "/info"
    /**
     * 版本
     */
    var version: Int = 0
    /**
     * cpu能力（仅代表服务的处理能力，可能影响任务分发）
     */
    var cpu: Int = 1
    /**
     * 最大同时运行任务
     */
    var maxTaskQueue = 1000
    /**
     * 计算最近平均值用的任务数
     */
    var recentTaskNum = 100
    /**
     * 服务是否可用（设置为false可关闭对外服务）
     */
    var serviceAvailable = true
    /**
     * 接受的post最大字节数（默认为1M）
     */
    var maxPostBytes = 1024_000L
    /**
     * 拦截器（http和请求）
     */
    val intercept: MutableList<IRpcIntercept> = ArrayList()
}