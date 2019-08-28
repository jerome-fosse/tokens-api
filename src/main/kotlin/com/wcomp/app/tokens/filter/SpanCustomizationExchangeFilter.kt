package com.wcomp.app.tokens.filter

import brave.SpanCustomizer
import com.wcomp.app.tokens.mdc.MDCTags
import com.wcomp.app.tokens.mdc.SpanCustomization.tagError
import com.wcomp.app.tokens.mdc.SpanCustomization.tagHttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

class SpanCustomizationExchangeFilter(private val partnerName: String, private val spanCustomizer: SpanCustomizer) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request)
                .doOnError {
                    spanCustomizer.tag(MDCTags.PARTNER.mdcKey, partnerName)
                    tagError(spanCustomizer, it)
                }
                .doOnSuccess {
                    spanCustomizer.tag(MDCTags.PARTNER.mdcKey, partnerName)
                    tagHttpStatus(spanCustomizer, it.statusCode())
                }
    }
}

