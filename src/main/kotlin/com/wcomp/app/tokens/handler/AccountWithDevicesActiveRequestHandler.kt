package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.model.AccountResponse
import com.wcomp.app.tokens.model.ErrorResponse
import com.wcomp.app.tokens.service.AccountService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono

@Component
class AccountWithDevicesActiveRequestHandler(private val accountService: AccountService) : RequestHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(AccountWithDevicesActiveRequestHandler::class.java)
    }

    override fun handleRequest(request: ServerRequest): Mono<ServerResponse> {
        val iuc = request.pathVariable("iuc")
        val active = when (request.queryParam("active").orElse("true")) {
            "true" -> true
            "false" -> false
            else -> return badRequest().body(fromObject(ErrorResponse("CONNECT_VALIDATION_ERROR", "Parameter format error. Active should be true ou false")))
        }
        logger.info("Getting Account with iuc = $iuc and devices active status = $active.")

        return accountService.getAccountWithActiveDevices(iuc, active)
                .flatMap { ok().body(fromObject(AccountResponse.of(it))) }
                .doOnError { logger.error(it.localizedMessage, it) }
                .onErrorResume {
                    when (it) {
                        is AccountNotFoundException -> status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse("CONNECT_ACCOUNT_NOT_FOUND", it.message ?: "")))
                        else -> internalServerError(it)
                    }
                }
    }
}