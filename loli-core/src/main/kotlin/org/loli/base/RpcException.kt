package org.loli.base

/**
 * rpc错误定义
 */
open class RpcException(message: String): Exception(message){
    /**
     * 鉴权错误
     */
    class AuthError(message: String): RpcException(message)
}