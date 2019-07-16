package org.loli.dispatch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.loli.base.RpcException

/**
 * 单一类型节点调度器
 */
class NodeDispatch {
    /**
     * 服务对应的服务节点信息
     */
    private val endPoints: LinkedHashMap<String, EndPointInfo> = LinkedHashMap()
    /**
     * 同步器
     */
    private val mutex = Mutex()

    /**
     * 更新节点
     */
    suspend fun update(endPoints: List<NodeEndPoint>) {
        val keyToEp = HashMap<String, EndPointInfo>()
        for (ep in endPoints) {
            keyToEp["${ep.host}:${ep.port}"] = EndPointInfo(ep)
        }

        mutex.withLock {
            // 删除不再存在的节点
            val iter = this.endPoints.iterator()
            while (iter.hasNext()) {
                val ep = iter.next().value.endPoint
                if (!keyToEp.containsKey("${ep.host}:${ep.port}")) {
                    iter.remove()
                }
            }

            // 添加新的
            for ((key, value) in keyToEp) {
                if (!this.endPoints.containsKey(key)) {
                    this.endPoints[key] = value
                }
            }
        }
    }

    /**
     * 选择一个节点
     * TODO: 使用更先进的策略（暂时用的轮转）
     * @param name 名称
     */
    suspend fun select(name: String): NodeEndPoint {
        mutex.withLock {
            if (endPoints.isEmpty()) {
                throw RpcException("no endpoint for '$name'")
            }
            val data = endPoints.iterator().next()

            // 放到最后面
            endPoints.remove(data.key)
            endPoints[data.key] = data.value

            return data.value.endPoint
        }
    }

    /**
     * 得到所有节点
     * @return 节点地址列表
     */
    suspend fun getAll(): List<Pair<String, Int>> {
        val ret = ArrayList<Pair<String, Int>>()
        mutex.withLock {
            for((_,value) in endPoints){
                ret.add(Pair(value.endPoint.host, value.endPoint.port))
            }
        }
        return ret
    }

    /**
     * 更新节点状态
     * @param endPoint 节点
     * @param curTask 当前任务数，默认不修改
     * @param available 是否可用，默认不修改
     */
    suspend fun updateStatus(
        endPoint: Pair<String, Int>,
        curTask: Int? = null,
        available: Boolean? = null
    ) {
        mutex.withLock {
            endPoints["${endPoint.first}:${endPoint.second}"]?.let {
                it.curTask = curTask ?: it.curTask
                it.available = available ?: it.available
            }
        }
    }

    /**
     * 节点信息
     */
    class EndPointInfo(
        /**
         * 节点
         */
        val endPoint: NodeEndPoint
    ) {
        /**
         * 当前任务数
         */
        var curTask = 0
        /**
         * 是否可用
         */
        var available = true
    }
}