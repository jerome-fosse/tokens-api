package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.model.ConnectCreateAccountRequest
import com.wcomp.app.tokens.service.AccountService
import com.wcomp.app.tokens.connect.PartnerException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.*

@Component
class CreateAccountRequestHandler(private val accountService: AccountService, validator: Validator): AbstractValidationHandler<ConnectCreateAccountRequest>(validator) {

    companion object {
        val logger = LoggerFactory.getLogger(CreateAccountRequestHandler::class.java)
    }

    override fun processBody(body: ConnectCreateAccountRequest, request: ServerRequest): Mono<ServerResponse> {
        logger.info("createConnectAccount with parameters : $body")

        return accountService.createAccount(email = body.email, password = body.password, firstname = body.firstname,
                lastname = body.lastname, birthDate = body.birthDate.toLocalDate(), language = Locale(body.language.toLowerCase()))
                .then(ServerResponse.ok().build())
                .doOnError { logger.error(it.localizedMessage) }
                .onErrorResume {
                    when (it) {
                        is PartnerException -> serverError(it.httpStatusCode, it.errorName, it.error.errorDescription)
                        else -> internalServerError(it)
                    }
                }
    }
}