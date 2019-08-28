package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.exeption.NotMatchingDataException
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ACCOUNT_INFO_BAD_REQUEST
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_ID_TOKEN_ERROR
import com.wcomp.app.tokens.service.AccountService
import com.wcomp.app.tokens.connect.PartnerException
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.SpanName
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AccountInfoRequestHandler(private val accountService: AccountService) : RequestHandler {

    companion object {
        val logger = LoggerFactory.getLogger(AccountInfoRequestHandler::class.java)
    }

    @SpanName("GET /connect/accounts")
    override fun handleRequest(request: ServerRequest): Mono<ServerResponse> {
        val idToken = request.headers().header("X-id-token")[0]
        val deviceId = request.queryParam("deviceId")

        return if (!deviceId.isPresent) {
            badRequestServerError(CONNECT_ACCOUNT_INFO_BAD_REQUEST, "Missing deviceId parameter")
        } else {
            logger.info("Getting Account with deviceId = ${deviceId.get()} and idToken = ${idToken.obfuscateBegin(20)}.")
            accountService.getAccountInfo(deviceId.get(), idToken)
                    .flatMap { ok().body(fromObject(it)) }
                    .doOnError { logger.error(it.localizedMessage) }
                    .onErrorResume {
                        when (it) {
                            is JWTVerificationException -> forbiddenServerError(CONNECT_ID_TOKEN_ERROR, it.localizedMessage)
                            is NotMatchingDataException -> badRequestServerError(CONNECT_ACCOUNT_INFO_BAD_REQUEST, it.message)
                            is PartnerException -> serverError(it.httpStatusCode, it.errorName, it.error.errorDescription)
                            else -> internalServerError(it)
                        }
                    }
        }
    }
}