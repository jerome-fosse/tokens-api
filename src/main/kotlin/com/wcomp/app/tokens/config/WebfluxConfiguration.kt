package com.wcomp.app.tokens.config

import brave.SpanCustomizer
import com.wcomp.app.tokens.filter.AccessTokenValidationFilter
import com.wcomp.app.tokens.filter.IdTokenPredicateFilter
import com.wcomp.app.tokens.filter.SpanCustomizationHandlerFilter
import com.wcomp.app.tokens.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router

@Configuration
class WebfluxConfiguration(val spanCustomizer: SpanCustomizer) {

    @Bean
    fun createAccountRouter(handler: CreateAccountRequestHandler) = router {
        ("/connect/accounts" and accept(MediaType.APPLICATION_JSON) and contentType(MediaType.APPLICATION_JSON)).nest {
            PUT("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer, "PUT /connect/accounts"))

    @Bean
    fun accountInfoRouter(handler: AccountInfoRequestHandler, accessTokenValidationFilter: AccessTokenValidationFilter) = router {
        ("/connect/accounts" and accept(MediaType.APPLICATION_JSON)).nest {
            GET("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))
            .filter(IdTokenPredicateFilter())
            .filter(accessTokenValidationFilter)

    @Bean
    fun logoutRouter(handler: LogoutRequestHandler) = router {
        ("/connect/logout" and accept(MediaType.APPLICATION_JSON) and contentType(MediaType.APPLICATION_JSON)).nest {
            POST("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))

    @Bean
    fun registerTokenRouter(handler: RegisterTokenRequestHandler) = router {
        ("/token/register" and contentType(MediaType.APPLICATION_JSON) and accept(MediaType.APPLICATION_JSON)).nest {
            POST("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))

    @Bean
    fun saveMaasTokenRouter(handler: SaveMaasTokenRequestHandler, accessTokenValidationFilter: AccessTokenValidationFilter) = router {
        ("/token/maas" and accept(MediaType.APPLICATION_JSON) and contentType(MediaType.APPLICATION_JSON)).nest {
            POST("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))
            .filter(IdTokenPredicateFilter())
            .filter(accessTokenValidationFilter)

    @Bean
    fun accountWithDevicesActiveRouter(handler: AccountWithDevicesActiveRequestHandler) = router {
        ("/accounts/{iuc}" and accept(MediaType.APPLICATION_JSON)).nest {
            GET("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))

    @Bean
    fun accountsRouter(handler: AccountRequestHandler) = router {
        ("/accounts" and contentType(MediaType.APPLICATION_JSON) and accept(MediaType.APPLICATION_JSON)).nest {
            GET("", handler::handleRequest)
        }
    }.filter(SpanCustomizationHandlerFilter(spanCustomizer))
            .filter(IdTokenPredicateFilter())
}