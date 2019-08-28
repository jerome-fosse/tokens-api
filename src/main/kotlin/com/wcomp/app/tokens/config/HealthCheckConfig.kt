package com.wcomp.app.tokens.config

import com.codahale.metrics.Histogram
import com.codahale.metrics.MetricRegistry
import com.wcomp.app.healthcheck.annotations.AppHealthIndicator
import com.wcomp.app.healthcheck.annotations.IndicatorType
import com.wcomp.app.healthcheck.health.HistoAcceptableMeanIndicator
import com.wcomp.app.healthcheck.health.ReactiveHealthIndicatorWrapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class HealthCheckConfig {

    // External parters
    @AppHealthIndicator(indicatorType = IndicatorType.EXTERNAL)
    @Bean
    internal fun checkConnectHealth(connectHistogram: Histogram, @Value("\${wcomp.connect.acceptable.mean:0.98}") acceptableMean: Double): HealthIndicator {
        return HistoAcceptableMeanIndicator(connectHistogram, acceptableMean, "CONNECT")
    }

    // Middlewares
    @AppHealthIndicator(indicatorType = IndicatorType.MIDDLEWARES)
    @Bean
    internal fun checkMongoHealth(@Qualifier("mongoHealthIndicator") mongoReactiveHealthIndicator: ReactiveHealthIndicator): ReactiveHealthIndicator {
        return ReactiveHealthIndicatorWrapper("MONGO", mongoReactiveHealthIndicator)
    }

    @AppHealthIndicator(indicatorType = IndicatorType.MIDDLEWARES)
    @Bean
    internal fun checkRedisHealth(@Qualifier("redisHealthIndicator") redisReactiveHealthIndicator: ReactiveHealthIndicator): ReactiveHealthIndicator {
        return ReactiveHealthIndicatorWrapper("REDIS", redisReactiveHealthIndicator)
    }

    @AppHealthIndicator(indicatorType = IndicatorType.MIDDLEWARES)
    @Bean
    internal fun checkRedisTime(redisHistogram: Histogram, @Value("\${Redis.acceptable.responseTime:300}") redisAcceptableMean: Double): HealthIndicator {
        return HistoAcceptableMeanIndicator(redisHistogram, redisAcceptableMean, "REDIS_TIME")
    }

    // Beans a deplacer?
    @Bean
    fun metricRegistry(): MetricRegistry {
        return MetricRegistry()
    }
}