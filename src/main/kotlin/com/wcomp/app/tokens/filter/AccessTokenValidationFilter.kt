package com.wcomp.app.tokens.filter

import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ACCESS_TOKEN_ERROR
import com.wcomp.app.tokens.handler.badRequestServerError
import com.wcomp.app.tokens.model.ErrorResponse
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.PartnerException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFilterFunction
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class AccessTokenValidationFilter(@param:Value("\${features.access-token-validation.controller.enabled}")
                                  private val accessTokenValidationEnabled: Boolean,
                                  private val connectClient: ConnectClient) : HandlerFilterFunction<ServerResponse, ServerResponse> {

    companion object {
        private val logger = LoggerFactory.getLogger(AccessTokenValidationFilter::class.java)
    }

    override fun filter(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse> {
        if (accessTokenValidationEnabled) {
            val accessTokenHeaders = request.headers().header("X-access-token")

            if (accessTokenHeaders.isEmpty()) {
                return ServerResponse.badRequest().body(BodyInserters
                        .fromObject(ErrorResponse("CONNECT_BAD_REQUEST", "Missing request header 'X-access-token'")))
            }
            logger.debug("Calling Connect to validate the access token.")
            return connectClient.validateAccessToken(accessTokenHeaders[0])
                    .doOnSuccess { logger.debug("The access token is valid") }
                    .then(next.handle(request))
                    .doOnError { logger.error(it.localizedMessage) }
                    .onErrorResume {
                        when (it) {
                            is PartnerException -> badRequestServerError(CONNECT_ACCESS_TOKEN_ERROR, it.error.errorDescription)
                            else -> badRequestServerError(CONNECT_ACCESS_TOKEN_ERROR, it.localizedMessage)
                        }
                    }
        }
        return next.handle(request)
    }
}

