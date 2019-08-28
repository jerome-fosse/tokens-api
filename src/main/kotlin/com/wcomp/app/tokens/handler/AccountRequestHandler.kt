package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.exeption.NotMatchingDataException
import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_BAD_REQUEST
import com.wcomp.app.tokens.model.AccountResponse
import com.wcomp.app.tokens.model.ErrorResponse
import com.wcomp.app.tokens.service.AccountService
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono

@Component
class AccountRequestHandler(private val accountService: AccountService) : RequestHandler {

    companion object {
        val logger = LoggerFactory.getLogger(AccountRequestHandler::class.java)
    }

    override fun handleRequest(request: ServerRequest): Mono<ServerResponse> {
        val idToken = request.headers().header("X-id-token")[0]
        val deviceId = request.queryParam("deviceId")
        if (!deviceId.isPresent) {
            return badRequestServerError(CONNECT_BAD_REQUEST, "Missing deviceId parameter.")
        }
        logger.info("Getting Account with idToken = ${idToken.obfuscateBegin(20)}, device = ${deviceId.get()}.")

        return accountService.getAccountWithDevice(idToken, deviceId.get())
                .flatMap { ok().body(fromObject(AccountResponse.of(it))) }
                .doOnError { logger.error(it.localizedMessage, it) }
                .onErrorResume {
                    when (it) {
                        is AccountNotFoundException -> status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse("CONNECT_ACCOUNT_NOT_FOUND", it.message ?: "")))
                        is NotMatchingDataException -> badRequestServerError(CONNECT_BAD_REQUEST, it.message)
                        else -> internalServerError(it)
                    }
                }
    }
}