package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.handler.ApplicationErrorCode.CONNECT_VALIDATION_ERROR
import org.slf4j.LoggerFactory
import org.springframework.core.codec.DecodingException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import java.lang.reflect.ParameterizedType

abstract class AbstractValidationHandler<T>(private val validator: Validator): RequestHandler {
    companion object {
        val logger = LoggerFactory.getLogger(AbstractValidationHandler::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    val bodyType = (this.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

    override fun handleRequest(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(bodyType)
                .flatMap { body ->
                    val errors = BeanPropertyBindingResult(body, bodyType.name)
                    validator.validate(body, errors)

                    if (errors.allErrors.isEmpty()) {
                        processBody(body, request)
                    } else {
                        onValidationErrors(errors, request)
                    }
                }
                .doOnError { logger.error("Error while handling bean of type : ${bodyType.name}", it) }
                .onErrorResume(ServerWebInputException::class.java) {
                    badRequestServerError(CONNECT_VALIDATION_ERROR, it?.rootCause?.localizedMessage ?: it.localizedMessage)
                }
                .onErrorResume(DecodingException::class.java) {
                    badRequestServerError(CONNECT_VALIDATION_ERROR, it?.cause?.localizedMessage ?: it.localizedMessage)
                }
    }

    protected fun onValidationErrors(errors: Errors, request: ServerRequest): Mono<ServerResponse> {
        return badRequestServerError(CONNECT_VALIDATION_ERROR, extractFieldsErrors(errors))
    }

    private fun extractFieldsErrors(errors: Errors) =
        "${errors.fieldErrorCount} error(s) while validating ${errors.objectName} : ${errors.fieldErrors.map { it.defaultMessage }.reduce {s1, s2 -> "$s1 - $s2"}}"

    abstract fun processBody(body: T, request: ServerRequest): Mono<ServerResponse>
}