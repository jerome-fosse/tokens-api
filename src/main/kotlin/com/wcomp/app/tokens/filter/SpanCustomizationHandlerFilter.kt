package com.wcomp.app.tokens.filter

import brave.SpanCustomizer
import com.wcomp.app.tokens.mdc.SpanCustomization.tagError
import com.wcomp.app.tokens.mdc.SpanCustomization.tagHttpStatus
import org.springframework.web.reactive.function.server.HandlerFilterFunction
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.*

class SpanCustomizationHandlerFilter(private val spanCustomizer: SpanCustomizer, private val serviceName: String = "Connect") : HandlerFilterFunction<ServerResponse, ServerResponse> {

    override fun filter(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse> {
        return next
                .handle(request)
                .doOnSuccessOrError { response, err ->
                    val service = Optional.ofNullable(serviceName).orElseGet { "${request.methodName()} ${request.path()}" }
                    spanCustomizer.tag("service", service)
                    Optional.ofNullable(response).ifPresent { resp -> tagHttpStatus(spanCustomizer, resp.statusCode()) }
                    Optional.ofNullable(err).ifPresent { ex -> tagError(spanCustomizer, ex) }
                }
    }
}

