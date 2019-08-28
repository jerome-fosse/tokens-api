package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ID_TOKEN_ERROR
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_INVALID_REFRESH_TOKEN
import com.wcomp.app.tokens.model.ConnectLogoutRequest
import com.wcomp.app.tokens.service.TokenService
import com.wcomp.app.tokens.connect.InvalidRefreshTokenException
import com.wcomp.app.tokens.connect.PartnerException
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class LogoutRequestHandler(private val tokenService: TokenService, validator: Validator) : AbstractValidationHandler<ConnectLogoutRequest>(validator) {

    companion object {
        val logger = LoggerFactory.getLogger(LogoutRequestHandler::class.java)
    }

    override fun processBody(body: ConnectLogoutRequest, request: ServerRequest): Mono<ServerResponse> {
        logger.info("logoutAndInvalidateRefreshToken with idToken ${body.idToken.obfuscateBegin(20)} - refreshToken ${body.idToken.obfuscateEnd(5)} - deviceId ${body.deviceId}.")

        return tokenService.logoutAndInvalidateRefreshToken(body.idToken, body.refreshToken, body.deviceId)
                .then(ok().build())
                .doOnError { logger.error(it.localizedMessage) }
                .onErrorResume {
                    when (it) {
                        is InvalidRefreshTokenException -> badRequestServerError(CONNECT_INVALID_REFRESH_TOKEN, it.error.errorDescription)
                        is JWTVerificationException -> forbiddenServerError(CONNECT_ID_TOKEN_ERROR, it.localizedMessage)
                        is PartnerException -> serverError(it.httpStatusCode, it.errorName, it.error.errorDescription)
                        else -> internalServerError(it)
                    }
                }
    }
}