package org.loli.base

/**
 * rpc相关的http头名称
 */
object RpcHeaderName {
    /**
     * 服务状态，0 正常，1 不可用
     */
    const val SERVICE_AVAILABLE = "service_available"
    /**
     * 服务的cpu个数
     */
    const val SERVICE_CPU = "service_cpu"
    /**
     * 当前处理任务数
     */
    const val SERVICE_TASK_NUM = "service_task_num"
    /**
     * 最大任务队列数
     */
    const val SERVICE_MAX_QUEUE = "service_max_queue"
    /**
     * 当前是第几跳
     */
    const val SERVICE_HOP = "service_hop"
}