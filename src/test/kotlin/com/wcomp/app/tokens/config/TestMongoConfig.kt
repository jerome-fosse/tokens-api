package com.wcomp.app.tokens.config

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration


@Configuration
@Profile("test")
class TestMongoConfig : AbstractReactiveMongoConfiguration() {

    @Value("\${spring.data.mongodb.uri}")
    private val connectionUrl: String? = null

    @Bean
    override fun reactiveMongoClient(): MongoClient {
        return MongoClients.create(connectionUrl)
    }

    override fun getDatabaseName(): String {
        return "mockTokens"
    }
}
