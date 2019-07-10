package org.loli.util

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpHeaders
import io.ktor.request.contentCharset
import io.ktor.request.header
import org.loli.base.RpcException
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.math.max

/**
 * 把错误类型转化为string
 * @param e 错误
 * @param maxLine 返回的最大行数，默认15行
 */
fun toString(e: Throwable, maxLine: Int = 15): String{
    val buf = ByteArrayOutputStream(4096)
    val stream = PrintStream(buf, true, "utf-8")
    e.printStackTrace(stream)
    stream.close()

    val lines = String(buf.toByteArray(), Charsets.UTF_8).split('\n')
    val ret = StringBuilder()
    for(index in 0 until max(lines.size, maxLine)) {
        ret.appendln(lines[index])
    }
    return ret.toString()
}

/**
 * 读取http请求的post数据
 * @param maxByte post接受的最大字节数
 */
suspend fun ApplicationCall.receiveTextNew(maxByte: Long): String{
    val len = request.header(HttpHeaders.ContentLength)?.toInt() ?: 0
    if(len > maxByte){
        throw RpcException("post data exceeds $maxByte bytes")
    }else if(len > 0){
        val buf = ByteArray(len)
        val channel = request.receiveChannel()
        channel.readFully(buf, 0, len)

        var charset: Charset = Charsets.UTF_8
        request.contentCharset()?.let { charset = it }

        return String(buf, charset)
    }
    return ""
}

