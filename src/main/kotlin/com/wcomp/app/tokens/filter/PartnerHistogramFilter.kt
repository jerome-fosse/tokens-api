package com.wcomp.app.tokens.filter

import com.codahale.metrics.Histogram
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

class PartnerHistogramFilter(private val partnerName: String, private val histogram: Histogram) : ExchangeFilterFunction {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PartnerHistogramFilter::class.java)
    }

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request).doOnSuccessOrError { resp, err ->
            var result = 1
            if (resp != null && resp.statusCode().is5xxServerError || err != null) {
                result = 0
            }
            histogram.update(result)
            LOGGER.debug("Call to partner {} was {}, for request : {} {}", partnerName, if (result == 1) "OK" else "KO", request.method(), request.url().path)
        }
    }
}

