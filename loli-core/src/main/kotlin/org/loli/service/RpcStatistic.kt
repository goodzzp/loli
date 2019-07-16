package org.loli.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * 服务统计
 */
class RpcStatistic(
    /**
     * 最近计算个数
     */
    val recentNum: Int = 100
) {
    /**
     * 开始时间
     */
    var startTime = System.currentTimeMillis()
        private set
    /**
     * 任务总数
     */
    var taskNum = 0L
        private set
    /**
     * 今天任务总数
     */
    var taskNumToday = 0L
        private set
    /**
     * 当前任务数
     */
    var curTaskNum = 0L
        private set
    /**
     * 最近任务时间消耗
     */
    private val recentTaskTimes = LinkedList<Long>()
    /**
     * 总耗时
     */
    var totalTaskTime = 0L
        private set
    /**
     * 今日总耗时
     */
    var totalTaskTimeToday = 0L
        private set
    /**
     * 跳过任务数
     */
    var skipTaskNum = 0L
        private set
    /**
     * 今日跳过任务数
     */
    var skipTaskNumToday = 0L
        private set
    /**
     * 同步
     */
    private val mutex = Mutex()

    /**
     * 启动一个任务
     */
    suspend fun startOneTask(){
        mutex.withLock {
            taskNum ++
            taskNumToday ++
            curTaskNum ++
        }
    }

    /**
     * 完成一个任务
     * @param timeUsed 时间消耗
     */
    suspend fun finishOneTask(timeUsed: Long){
        mutex.withLock {
            curTaskNum --
            recentTaskTimes.add(timeUsed)
            if(recentTaskTimes.size > recentNum)
                recentTaskTimes.removeFirst()
            totalTaskTime += timeUsed
            totalTaskTimeToday += timeUsed
        }
    }

    /**
     * 跳过一个任务
     */
    suspend fun skipOneTask(){
        mutex.withLock {
            skipTaskNum ++
            skipTaskNumToday ++
        }
    }

    /**
     * 新的一天
     */
    suspend fun newDay(){
        mutex.withLock {
            taskNumToday = 0
            totalTaskTimeToday = 0
            skipTaskNumToday = 0
        }
    }

    /**
     * 得到最近请求的平均耗时
     */
    suspend fun getRecentAvgTime(): Long{
        mutex.withLock {
            if(recentTaskTimes.isEmpty())
                return 0
            return recentTaskTimes.sum() / recentTaskTimes.size
        }
    }
}