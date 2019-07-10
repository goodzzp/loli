package org.loli.base

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.withCharset

/**
 * 基于apache httpclient构建，测试qps=8000 / 1.3w(keep-alive)
 */
object HttpClientByApache {
    /**
     * 客户端
     */
    private val client = HttpClient(Apache) {
        engine {
            followRedirects = true

            socketTimeout = 10_000  // Max time between TCP packets - default 10 seconds
            connectTimeout = 10_000 // Max time to establish an HTTP connection - default 10 seconds
            connectionRequestTimeout = 20_000 // Max time for the connection manager to start a request - 20 seconds

            customizeClient {
                setMaxConnTotal(10000) // Maximum number of socket connections.
                setMaxConnPerRoute(10000) // Maximum number of requests for a specific endpoint route.
            }
            customizeRequest {
                // Apache's RequestConfig.Builder
            }
        }
    }

    /**
     * 执行post命令，内容是json
     * @param url http地址
     * @param json json数据
     * @return 返回头和体
     */
    suspend fun postJson(url: String, json: String): Pair<Headers, String?> {
        val resp = client.call(url){
            method = HttpMethod.Post
            body = TextContent(json, ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        return Pair(resp.response.headers, resp.response.readText(Charsets.UTF_8))
    }

    /**
     * 执行get请求
     * @param url 请求url
     * @return 返回头和体
     */
    suspend fun get(url: String): Pair<Headers, String?>{
        val resp = client.call(url)
        return Pair(resp.response.headers, resp.response.readText(Charsets.UTF_8))
    }
}