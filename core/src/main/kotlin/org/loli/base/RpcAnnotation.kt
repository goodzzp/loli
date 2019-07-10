package org.loli.base

/**
 * 需要暴露出去的方法或类
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RpcExport(
    /**
     * 名称，默认为类或方法名
     */
    val name : String = "",
    /**
     * 描述
     */
    val desc : String
)

/**
 * 描述
 */
annotation class RpcDesc(
    /**
     * 描述
     */
    val desc: String
)

/**
 * 方法返回
 */
@Target(AnnotationTarget.FUNCTION)
annotation class RpcReturn(
    /**
     * 描述
     */
    val desc: String
)

/**
 * 是否需要鉴权，默认是
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RpcAuth(
    /**
     * 是否需要鉴权
     */
    val need: Boolean = true
)
