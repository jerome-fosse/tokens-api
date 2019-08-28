package com.wcomp.app.tokens

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

@ContextConfiguration(initializers = [UsingRedis.Initializer::class])
open class UsingRedis {

    companion object {
        val REDIS_PORT = 6379

        @Container
        var redis: GenericContainer<Nothing> = GenericContainer<Nothing>("redis:3.0.5").withExposedPorts(REDIS_PORT)
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.redis.port=" + redis.getMappedPort(REDIS_PORT)
            ).applyTo(applicationContext.environment)
        }
    }

}
