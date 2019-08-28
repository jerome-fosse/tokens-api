package com.wcomp.app.tokens.config

import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestHealthConfig {

    @MockBean(name = "redisHealthIndicator")
    private val redisHealthIndicator: ReactiveHealthIndicator? = null

    @MockBean(name = "mongoHealthIndicator")
    private val mongoHealthIndicator: ReactiveHealthIndicator? = null

}
