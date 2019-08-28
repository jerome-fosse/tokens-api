package com.wcomp.app.tokens.config

import com.wcomp.app.tokens.connect.model.AccountInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.convert.KeyspaceConfiguration
import org.springframework.data.redis.core.convert.KeyspaceConfiguration.KeyspaceSettings
import org.springframework.data.redis.core.convert.MappingConfiguration
import org.springframework.data.redis.core.index.IndexConfiguration
import org.springframework.data.redis.core.mapping.RedisMappingContext
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@Configuration
@EnableRedisRepositories(basePackages = ["com.wcomp.app.tokens.connect.cache"])
class RedisConfig {

    @Value("\${spring.redis.time-to-live.account-info}")
    private val timeToLiveForAccountInfo: Long = 0

    @Bean
    internal fun redisTemplate(lettuceConnectionFactory: LettuceConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = lettuceConnectionFactory
        return template
    }

    @Bean
    internal fun keyValueMappingContext(): RedisMappingContext {
        return RedisMappingContext(MappingConfiguration(IndexConfiguration(), keyspaceConfiguration()))
    }

    private fun keyspaceConfiguration(): KeyspaceConfiguration {
        val keyspaceSettings = KeyspaceSettings(AccountInfo::class.java, "accountInfo")
        keyspaceSettings.timeToLive = timeToLiveForAccountInfo

        val keyspaceConfiguration = KeyspaceConfiguration()
        keyspaceConfiguration.addKeyspaceSettings(keyspaceSettings)
        return keyspaceConfiguration
    }
}
