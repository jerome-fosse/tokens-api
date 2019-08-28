package com.wcomp.app.tokens

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

@ContextConfiguration(initializers = [UsingMongoDB.Initializer::class])
open class UsingMongoDB {
    companion object {
        const val MONGODB_PORT = 27017

        @Container
        val mongodb: GenericContainer<Nothing> = GenericContainer<Nothing>("mongo:4.0")
                .withExposedPorts(MONGODB_PORT)
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.data.mongodb.uri=mongodb://127.0.0.1:" + mongodb.getMappedPort(MONGODB_PORT)
            ).applyTo(applicationContext.environment)
        }
    }
}