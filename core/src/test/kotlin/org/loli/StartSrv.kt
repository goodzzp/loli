package org.loli

import org.loli.base.RpcAuth
import org.loli.base.RpcCall
import org.loli.base.RpcDesc
import org.loli.base.RpcExport
import org.loli.service.AbsRpc
import org.loli.service.RpcConf
import org.loli.service.StartRpcJson

@RpcExport("Add", "加法器")
@RpcAuth(false)
class Add : AbsRpc() {
    @RpcExport(desc = "计算加法")
    fun add(
        @RpcDesc("第一个参数") a: Int,
        @RpcDesc("第二个参数") b: Int
    ): Int {
        return a + b
    }
}

fun main() {
    // 注册rpc对外类
    RpcCall.register(Add::class)

    // 配置
    val conf = RpcConf("test", "测试", "127.0.0.1", 8000)

    // 启动服务
    StartRpcJson().start(conf)
}