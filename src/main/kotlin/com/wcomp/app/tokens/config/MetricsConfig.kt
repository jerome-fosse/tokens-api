package com.wcomp.app.tokens.config

import com.codahale.metrics.Histogram
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SlidingWindowReservoir
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {

    @Bean(name = ["ConnectHistogram"])
    fun connectHistogram(metricRegistry: MetricRegistry): Histogram {
        // FIXME config ~xxxx requêtes pour un serveur en heure. On reprend la taille par défaut de l'ExponentiallyDecayingReservoir.
        return metricRegistry.register("wcomp.connect.create", Histogram(SlidingWindowReservoir(1028)))
    }

    @Bean
    fun simpleMeterRegistry(): MeterRegistry {
        return SimpleMeterRegistry()
    }

}
