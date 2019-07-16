package org.loli.service

/**
 * 统计结果的属性名
 */
object RpcJsonStatPropName {
    /**
     * 服务名
     */
    const val SERVICE = "service"
    /**
     * 启动时间
     */
    const val START = "start"
    /**
     * 运行时间
     */
    const val RUNNING = "running"
    /**
     * 处理任务数
     */
    const val TASK = "task"
    /**
     * 今日任务数
     */
    const val TASK_TODAY = "task_today"
    /**
     * 当前任务数
     */
    const val CUR_TASK = "cur_task"
    /**
     * 平均耗时
     */
    const val AVG_TIME = "avg_time"
    /**
     * 今日平均耗时
     */
    const val AVG_TIME_TODAY = "avg_time_today"
    /**
     * 最近N个请求平均耗时
     */
    const val AVG_TIME_RECENT = "avg_time_recent"
    /**
     * 跳过任务数
     */
    const val SKIP_TASK = "skip_task"
    /**
     * 今日跳过任务数
     */
    const val SKIP_TASK_TODAY = "skip_task_today"
    /**
     * 当前线程数
     */
    const val THREAD_COUNT = "thread_count"
}