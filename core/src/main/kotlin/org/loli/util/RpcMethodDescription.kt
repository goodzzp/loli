package org.loli.util

import kotlin.reflect.KType

/**
 * 函数描述
 */
data class RpcMethodDescription(
    /**
     * 函数名
     */
    val name: String,
    /**
     * 描述
     */
    val desc: String,
    /**
     * 返回值
     */
    val returnType: KType,
    /**
     * 返回描述
     */
    val returnDesc: String,
    /**
     * 返回
     */
    val ret: KType,
    /**
     * 参数
     */
    val params: MutableList<ParamInfo> = ArrayList()
){
    /**
     * 参数信息
     */
    data class ParamInfo(
        /**
         * 类型
         */
        val type: KType,
        /**
         * 描述
         */
        val desc: String,
        /**
         * 名称
         */
        val name: String
    )
}