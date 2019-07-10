package org.loli.service

import com.github.salomonbrys.kotson.jsonObject
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.head
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.loli.base.RpcHeaderName
import org.loli.base.RpcReturnStatus
import org.loli.json.RpcJsonPropNames
import org.loli.service.html.ClassHtml
import org.loli.service.html.MethodHtml
import org.loli.service.html.ServiceHtml
import org.loli.util.receiveTextNew
import org.loli.util.toString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * 启动service层服务
 */
class StartRpcJson {
    companion object{
        /**
         * 日志
         */
        private val log: Logger = LoggerFactory.getLogger(StartRpcJson::class.java)
    }

    /**
     * 执行引擎
     */
    private lateinit var engine: ApplicationEngine
    /**
     * 统计数据
     */
    private lateinit var statistic: RpcStatistic

    /**
     * 启动，会阻塞当前线程
     * @param conf 配置
     */
    fun start(conf: RpcConf) = runBlocking {
        statistic = RpcStatistic(conf.recentTaskNum)

        // 是否到了新的一天
        checkNewDay(statistic)

        // 优雅关闭
        gracefulExit(conf, statistic)

        // 启动server, configure参考：https://ktor.io/servers/configuration.html
        engine = embeddedServer(Netty, conf.port, conf.host) {
            routing {
                // 请求拦截器
                intercept(ApplicationCallPipeline.Features){
                    conf.intercept.forEach {
                        if(! it.startHttp(call)){
                            finish()
                        }
                    }
                }

                // rpc调用
                post(conf.servicePath) {
                    // 服务不可用
                    if(! conf.serviceAvailable){
                        val obj = jsonObject(
                            RpcJsonPropNames.STATUS to RpcReturnStatus.ERROR.first,
                            RpcJsonPropNames.STATUS_INFO to "service paused by user"
                        )
                        call.respondText(obj.toString(), ContentType.Application.Json, HttpStatusCode.NotImplemented)
                    }
                    // 防止雪崩，限制运行任务数
                    else if (statistic.curTaskNum >= conf.maxTaskQueue) {
                        statistic.skipOneTask()

                        val obj = jsonObject(
                            RpcJsonPropNames.STATUS to RpcReturnStatus.ERROR.first,
                            RpcJsonPropNames.STATUS_INFO to "queue is full"
                        )
                        call.respondText(obj.toString(), ContentType.Application.Json, HttpStatusCode.ServiceUnavailable)
                    }
                    // 格式必须是json
//                    else if (!"json".equals(call.request.contentType().contentSubtype, true)) {
//                        call.respond(HttpStatusCode.NotAcceptable)
//                    }
                    // 正常流程
                    else {
                        callRpc(statistic, this, conf)
                    }
                }

                // 对接口的解释：method="service:class:method"
                get(conf.explainPath) {
                    callExplain(conf)
                }

                // 运行状态: 启动时间、运行时间、处理任务数、当前运行任务数、平均耗时、最近100个请求平均耗时、今日处理任务数、今日平均耗时
                get(conf.infoPath) {
                    call.respondText(statisticInfo(statistic, conf), ContentType.Application.Json)
                }

                // 根请求返回ok
                head("/{...}") {
                    call.respond(HttpStatusCode.OK)
                }

                // 其他（必须要，否则拦截器拦截不到这类请求）
                get("/{...}"){
                    call.respond(HttpStatusCode.OK, "query ${call.request.uri} is OK")
                }

                // 其他（必须要，否则拦截器拦截不到这类请求）
                post("/{...}"){
                    call.respond(HttpStatusCode.OK, "query ${call.request.uri} is OK")
                }
            }
        }
        engine.start(wait = true)
    }

    /**
     * 停止，一般仅作为测试用
     */
    fun close(){
        if(this::engine.isInitialized){
            engine.stop(1000, 1000, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * 调用正常rpc
     */
    private suspend fun callRpc(
        statistic: RpcStatistic,
        pipelineContext: PipelineContext<Unit, ApplicationCall>,
        conf: RpcConf
    ) {
        with(pipelineContext) {
            var time = 0L
            try {
                statistic.startOneTask()

                var ret:String? = null
                time = measureTimeMillis {
                    // 调用拦截器
                    if(conf.intercept.isNotEmpty()){
                        var userText : Pair<Boolean, String?>? = null
                        conf.intercept.forEach {
                            userText = it.beforeCall(call)
                        }
                        ret = if(! userText!!.first){
                            userText!!.second
                        } else{
                            val json = userText!!.second ?: call.receiveTextNew(conf.maxPostBytes)
                            RemoteCallJson.call(call, json, conf)
                        }
                    }else{
                        val json = call.receiveTextNew(conf.maxPostBytes)
                        ret = RemoteCallJson.call(call, json, conf)
                    }
                }

                ret?.let { resp ->
                    // 跳数加1
                    val hop = call.request.header(RpcHeaderName.SERVICE_HOP)
                    hop?.let {
                        call.response.header(RpcHeaderName.SERVICE_HOP, "${it.toInt() + 1}")
                    }

                    call.response.header(RpcHeaderName.SERVICE_CPU, "${conf.cpu}")
                    call.response.header(RpcHeaderName.SERVICE_TASK_NUM, "${statistic.curTaskNum - 1}")
                    call.response.header(RpcHeaderName.SERVICE_MAX_QUEUE, "${conf.maxTaskQueue}")
                    call.response.header(RpcHeaderName.SERVICE_AVAILABLE, "${conf.serviceAvailable}")

                    call.respondText(resp, ContentType.Application.Json)
                }
            } catch (e: Exception) {
                val obj = jsonObject(
                    RpcJsonPropNames.STATUS to 1,
                    RpcJsonPropNames.STATUS_INFO to toString(e)
                )
                call.respondText(obj.toString(), ContentType.Application.Json)
                log.error("deal service task failed", e)
            } finally {
                statistic.finishOneTask(time)
            }
        }
    }

    /**
     * 对接口的解释
     */
    private suspend fun PipelineContext<Unit, ApplicationCall>.callExplain(
        conf: RpcConf
    ) {
        val params = call.request.queryParameters
        if (params.contains(RpcJsonPropNames.METHOD)) {
            val ks = params[RpcJsonPropNames.METHOD]?.split(':')!!
            val service = if (ks.isNotEmpty()) ks[0] else ""
            val className = if (ks.size > 1) ks[1] else ""
            val method = if (ks.size > 2) ks[2] else ""
            when {
                className.isEmpty() -> call.respondText(ServiceHtml.getHtml(service, conf), ContentType.Text.Html)
                method.isEmpty() -> call.respondText(ClassHtml.getHtml(service, className, conf), ContentType.Text.Html)
                else -> call.respondText(MethodHtml.getHtml(service, className, method, conf), ContentType.Text.Html)
            }
        } else {
            call.respondText(ServiceHtml.getHtml(conf.service, conf), ContentType.Text.Html)
        }
    }

    /**
     * 返回统计信息
     */
    private suspend fun statisticInfo(statistic: RpcStatistic, conf: RpcConf): String {
        return jsonObject(
            "value" to jsonObject(
                RpcJsonStatPropName.SERVICE to conf.service,
                RpcJsonStatPropName.START to statistic.startTime,
                RpcJsonStatPropName.RUNNING to (System.currentTimeMillis() - statistic.startTime),
                RpcJsonStatPropName.TASK to statistic.taskNum,
                RpcJsonStatPropName.TASK_TODAY to statistic.taskNumToday,
                RpcJsonStatPropName.CUR_TASK to statistic.curTaskNum,
                RpcJsonStatPropName.AVG_TIME to
                        if (statistic.taskNum < 1) 0 else statistic.totalTaskTime / statistic.taskNum,
                RpcJsonStatPropName.AVG_TIME_TODAY to
                        if (statistic.taskNumToday < 1) 0 else statistic.totalTaskTimeToday / statistic.taskNumToday,
                RpcJsonStatPropName.AVG_TIME_RECENT to statistic.getRecentAvgTime(),
                RpcJsonStatPropName.SKIP_TASK to statistic.skipTaskNum,
                RpcJsonStatPropName.SKIP_TASK_TODAY to statistic.skipTaskNumToday,
                RpcJsonStatPropName.THREAD_COUNT to Thread.activeCount()
            ),
            "desc" to jsonObject(
                RpcJsonStatPropName.SERVICE to "服务名",
                RpcJsonStatPropName.START to "启动时间 System.currentTimeMillis()",
                RpcJsonStatPropName.RUNNING to "已经运行时间(ms)",
                RpcJsonStatPropName.TASK to "已经处理任务总数",
                RpcJsonStatPropName.TASK_TODAY to "今天处理任务总数",
                RpcJsonStatPropName.CUR_TASK to "当前正在处理任务数",
                RpcJsonStatPropName.AVG_TIME to "平均耗时(ms)",
                RpcJsonStatPropName.AVG_TIME_TODAY to "今天平均耗时(ms)",
                RpcJsonStatPropName.AVG_TIME_RECENT to "最近${statistic.recentNum}个请求平均耗时(ms)",
                RpcJsonStatPropName.SKIP_TASK to "已经跳过的任务总数(服务器资源不足)",
                RpcJsonStatPropName.SKIP_TASK_TODAY to "今日跳过的任务总数(服务器资源不足)",
                RpcJsonStatPropName.THREAD_COUNT to "线程个数"
            )
        ).toString()
    }

    /**
     * 优雅的退出
     */
    private fun gracefulExit(conf: RpcConf, statistic: RpcStatistic) {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                conf.serviceAvailable = false
                val waitSeconds = 10L

                log.info("start stopping service ${conf.service} in $waitSeconds seconds ... ")
                for (idx in 1..waitSeconds) {
                    sleep(1000)
                    if (statistic.curTaskNum == 0L) {
                        break
                    }
                }
                if (statistic.curTaskNum > 0) {
                    log.warn("still has ${statistic.curTaskNum} tasks not completed")
                }
                log.info("service ${conf.service} stopped")
            }
        })
    }

    /**
     * 计算是否到了新的一天
     */
    private fun checkNewDay(statistic: RpcStatistic) {
        // 每分钟计算一次是否到了新的一天
        val tickerChannel = ticker(delayMillis = 60 * 1000, initialDelayMillis = 0)
        GlobalScope.launch {
            var preDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            while (true) {
                tickerChannel.receive()
                val curDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if (preDay != curDay) {
                    preDay = curDay
                    statistic.newDay()
                }
            }
        }.start()
    }

    /**
     * 得到统计数据，仅测试用途
     */
    fun getStatistic() = if(this::statistic.isInitialized) statistic else null
}