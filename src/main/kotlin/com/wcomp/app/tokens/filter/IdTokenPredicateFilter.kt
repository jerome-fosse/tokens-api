package com.wcomp.app.tokens.filter

import com.wcomp.app.tokens.model.ErrorResponse
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFilterFunction
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class IdTokenPredicateFilter : HandlerFilterFunction<ServerResponse, ServerResponse> {
    override fun filter(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse> {
        return if (request.headers().header("X-id-token").isEmpty()) {
            ServerResponse.badRequest().body(BodyInserters.fromObject(ErrorResponse("CONNECT_BAD_REQUEST", "Missing request header 'X-id-token'")))
        } else next.handle(request)
    }
}
