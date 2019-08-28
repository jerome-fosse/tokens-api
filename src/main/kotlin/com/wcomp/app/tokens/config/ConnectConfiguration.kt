package com.wcomp.app.tokens.config

import brave.SpanCustomizer
import com.codahale.metrics.Histogram
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.ConnectClientConfiguration
import com.wcomp.app.tokens.connect.DefaultConnectClient
import com.wcomp.app.tokens.filter.PartnerHistogramFilter
import com.wcomp.app.tokens.filter.SpanCustomizationExchangeFilter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationProperties(prefix = "wcomp.connect")
class ConnectConfiguration {
    lateinit var user: String
    lateinit var password: String
    lateinit var accountApiUrl: String
    lateinit var openAMApiUrl: String
    lateinit var accountCreationCallbackUrl: String
    lateinit var accountCreationCallbackMobileUrl: String
    var sendAccountCreationNotifEmail: Boolean? = false

    @Bean
    internal fun connectClientConfiguration(): ConnectClientConfiguration {
        return ConnectClientConfiguration(
                user = user,
                password = password,
                accountApiUrl = accountApiUrl,
                openAMApiUrl = openAMApiUrl,
                accountCreationCallbackUrl = accountCreationCallbackUrl,
                accountCreationCallbackMobileUrl = accountCreationCallbackMobileUrl,
                sendAccountCreationNotifEmail = sendAccountCreationNotifEmail
        )
    }

    @Bean
    internal fun connectClient(configuration: ConnectClientConfiguration,
                               histogram: Histogram,
                               spanCustomizer: SpanCustomizer): ConnectClient {

        val webClient = WebClient.builder()
                .defaultHeader(ACCEPT, APPLICATION_JSON_UTF8_VALUE)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .defaultHeader("X-App", "UNI")
                .filter(ExchangeFilterFunctions.basicAuthentication(configuration.user, configuration.password))
                .filter(SpanCustomizationExchangeFilter("Connect", spanCustomizer))
                .filter(PartnerHistogramFilter("Connect", histogram))
                .build()

        return DefaultConnectClient(configuration, webClient)
    }
}