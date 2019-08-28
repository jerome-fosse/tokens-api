package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ACCESS_TOKEN_ERROR
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ID_TOKEN_ERROR
import com.wcomp.app.tokens.model.RegisterTokenRequest
import com.wcomp.app.tokens.service.TokenService
import com.wcomp.app.tokens.connect.AccessTokenException
import com.wcomp.app.tokens.connect.PartnerException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class RegisterTokenRequestHandler(private val tokenService: TokenService, validator: Validator) : AbstractValidationHandler<RegisterTokenRequest>(validator) {

    companion object {
        val logger = LoggerFactory.getLogger(RegisterTokenRequestHandler::class.java)
    }

    override fun processBody(body: RegisterTokenRequest, request: ServerRequest): Mono<ServerResponse> {
        logger.info("register token : $body")

        return tokenService.registerToken(idToken = body.idToken, accessToken = body.accessToken, deviceId = body.deviceId)
                .then(ok().build())
                .onErrorResume {
                    when (it) {
                        is AccessTokenException -> badRequestServerError(CONNECT_ACCESS_TOKEN_ERROR, it.error.errorDescription)
                        is JWTVerificationException -> forbiddenServerError(CONNECT_ID_TOKEN_ERROR, it.localizedMessage)
                        is PartnerException -> serverError(it.httpStatusCode, it.errorName, it.error.errorDescription)
                        else -> internalServerError(it)
                    }
                }
    }

}

