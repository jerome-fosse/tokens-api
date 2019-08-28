package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ACCOUNT_NOT_FOUND
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ID_TOKEN_ERROR
import com.wcomp.app.tokens.model.SaveMaasTokenRequest
import com.wcomp.app.tokens.service.TokenService
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import reactor.core.publisher.Mono

@Component
class SaveMaasTokenRequestHandler(private val tokenService: TokenService, validator: Validator): AbstractValidationHandler<SaveMaasTokenRequest>(validator) {

    companion object {
        val logger = LoggerFactory.getLogger(SaveMaasTokenRequestHandler::class.java)
    }

    override fun processBody(body: SaveMaasTokenRequest, request: ServerRequest): Mono<ServerResponse> {
        val idToken = request.headers().header("X-id-token")[0]
        logger.info("Save Maas Token $body with idToken ${idToken.obfuscateBegin(20)}")

        return tokenService.saveMaasToken(idToken, body.deviceId, body.deviceTokenMaas)
                .then(noContent().build())
                .doOnError { logger.error("Error while saving maas token : ${it.localizedMessage}") }
                .onErrorResume {
                    when (it) {
                        is AccountNotFoundException -> notFoundServerError(CONNECT_ACCOUNT_NOT_FOUND, it.localizedMessage)
                        is JWTVerificationException -> forbiddenServerError(CONNECT_ID_TOKEN_ERROR, it.localizedMessage)
                        else -> internalServerError(it)
                    }
                }
    }

}