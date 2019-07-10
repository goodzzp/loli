package org.loli.util

import kotlin.reflect.KParameter

/**
 * 类描述
 */
data class RpcClassDescription (
    /**
     * 类名称
     */
    val name: String,
    /**
     * 描述
     */
    val desc: String,
    /**
     * 属性名
     */
    val propNames: MutableList<String> = ArrayList(),
    /**
     * 属性类型
     */
    val propTypes: MutableList<String> = ArrayList(),
    /**
     * 属性描述
     */
    val propDescs: MutableList<String> = ArrayList(),
    /**
     * 属性
     */
    val props: MutableList<KParameter> = ArrayList()
)