package org.loli.util

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField


/**
 * 常用反射方法
 */
object KotlinReflectUtil {
    /**
     * 设置属性（支持val属性）
     * @param property 属性
     * @param obj 对象
     * @param value 设置的值
     */
    fun setProperty(property: KProperty<*>, obj: Any, value: Any?){
        val javaProp = property.javaField!!
        if(javaProp.isAccessible){
            javaProp.set(obj, value)
        }
        else{
            javaProp.isAccessible = true
            javaProp.set(obj, value)
            javaProp.isAccessible = false
        }
    }

    /**
     * 得到属性值（支持val属性）
     * @param property 属性
     * @param obj 对象
     */
    fun getProperty(property: KProperty<*>, obj: Any): Any?{
        val ret: Any?
        val javaProp = property.javaField!!
        if(javaProp.isAccessible){
            ret = javaProp.get(obj)
        }
        else{
            javaProp.isAccessible = true
            ret = javaProp.get(obj)
            javaProp.isAccessible = false
        }
        return ret
    }

    /**
     * 根据名字得到类里面的属性
     * @param clsDef 类定义
     * @param propertyName 属性名称
     */
    fun findProperty(clsDef: KClass<*>, propertyName: String): KProperty<*>?{
        clsDef.members.filter { it.name == propertyName && it is KProperty<*> }.forEach {
            return it as KProperty<*>
        }
        return null
    }

    /**
     * 根据名字得到类里面的方法
     * @param clsDef 类定义
     * @param methodName 方法名
     */
    fun findMethod(clsDef: KClass<*>, methodName: String): KCallable<*>?{
        clsDef.members.filter { it.name == methodName}.forEach {
            return it
        }
        return null
    }

    /**
     * 拷贝from到ret，对于任意属性A，ret.A = (from.A ?: default.A)
     */
    inline fun <reified T: Any> copy(from: T, default: T, ret: T){
        for(member in T::class.members){
            if(member is KProperty<*>){
                setProperty(member, ret, (getProperty(member, from) ?: getProperty(member, default)))
            }
        }
    }
}