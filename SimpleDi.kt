package com.example
/**
 * @author Aleksei Cherniaev, klukwist@gmail.com
 */

import com.example.InstanceType.ParamFactory.Params
import kotlin.reflect.KClass

/**
 * val instanceSomeClass = get<SomeClass> {
 *    params(someParam1ToSomeClass, someParam2ToSomeClass)
 * }
 */
inline fun <reified T : Any> get(noinline params: (Params.() -> Unit)? = null): T = SimpleDiStorage.getInstance(params)

/**
 * module {
 *     factory<C> { CImpl(a = get()) } // for register Factory.
 *     single<A> { AImpl() } // for register Singleton
 * }
 */

fun module(scope: SimpleDiScope.() -> Unit) {
    scope.invoke(SimpleDiScope)
}

object SimpleDiScope {
    inline fun <reified T : Any> factory(factory: InstanceType.Factory<T>) {
        SimpleDiStorage.addFactory(factory)
    }

    inline fun <reified T : Any> factoryWithParams(factory: InstanceType.ParamFactory<T>) {
        SimpleDiStorage.addFactory(factory)
    }

    inline fun <reified T : Any> singleton(factory: InstanceType.Factory<T>) {
        SimpleDiStorage.addFactory(InstanceType.Singleton<T>(factory))
    }
}


sealed interface InstanceType<T> {
    fun interface Factory<T> : InstanceType<T> {
        fun build(): T
    }

    fun interface ParamFactory<T> : InstanceType<T> {
        fun build(vararg params: Any): T

        class Params {
            var parameters: Array<out Any> = arrayOf()
                private set

            fun params(vararg parameters: Any) {
                this.parameters = parameters
            }
        }
    }

    class Singleton<T>(private val factory: Factory<T>) : InstanceType<T> {
        val instance: T by lazy {
            factory.build()
        }
    }
}


@PublishedApi
internal object SimpleDiStorage {
    val instances = mutableMapOf<KClass<*>, InstanceType<*>>()

    inline fun <reified T : Any> addFactory(factory: InstanceType<T>) {
        check(instances[T::class] == null) {
            "Definition for ${T::class} already added."
        }
        instances[T::class] = factory
    }

    inline fun <reified T : Any> getInstance(noinline parameters: (Params.() -> Unit)? = null): T {
        return when (val factory = instances[T::class]) {
            is InstanceType.Singleton -> factory.instance as T
            is InstanceType.Factory -> factory.build() as T
            is InstanceType.ParamFactory -> {
                val factoryParams = Params().apply(requireNotNull(parameters)).parameters
                factory.build(*factoryParams) as T
            }
            null -> error("No factory provided for class: ${T::class.java}")
        }
    }
}
