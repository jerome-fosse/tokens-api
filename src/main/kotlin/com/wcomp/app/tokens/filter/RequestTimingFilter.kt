package com.wcomp.app.tokens.filter

import io.micrometer.core.instrument.LongTaskTimer
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.server.HandlerFilterFunction
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit.MILLISECONDS


class RequestTimingFilter(private val timer: LongTaskTimer) : HandlerFilterFunction<ServerResponse, ServerResponse> {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RequestTimingFilter::class.java)
    }

    override fun filter(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse> {
        val sample = timer.start()
        return next
                .handle(request)
                .doOnSuccessOrError { _, _ ->
                    LOGGER.debug("${request.method()} ${request.uri()} duration : ${sample.duration(MILLISECONDS)}ms")
                    sample.stop()
                }
    }
}
