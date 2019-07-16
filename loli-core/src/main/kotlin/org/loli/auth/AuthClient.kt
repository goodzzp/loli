package org.loli.auth

/**
 * 鉴权客户端
 */
object AuthClient {
    /**
     * 鉴权函数
     */
    private var authMethod: suspend (String) -> Any? = {null}
    /**
     * 鉴权失败时的返回status和info
     */
    var AUTH_ERROR = Pair(1, "token已失效，请重新登陆")

    /**
     * 注册鉴权函数
     * @param auth 鉴权函数，token string 到 Any转换
     */
    fun register(auth: suspend (String) -> Any?){
        authMethod = auth
    }

    /**
     * 鉴权
     */
    suspend fun auth(token: String): Any?{
        return authMethod(token)
    }
}