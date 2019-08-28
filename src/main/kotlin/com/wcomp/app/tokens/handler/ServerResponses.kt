package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono


fun internalServerError(ex: Throwable): Mono<ServerResponse> = ServerResponse.status(INTERNAL_SERVER_ERROR)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse("CONNECT_UNEXPECTED_ERROR", ex.message ?: "The fuse must have blown.")))

fun forbiddenServerError(errorCode: ApplicationErrorCode, message: String) = ServerResponse.status(FORBIDDEN)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse(errorCode.name, message)))

fun badRequestServerError(errorCode: ApplicationErrorCode, message: String) = ServerResponse.status(BAD_REQUEST)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse(errorCode.name, message)))

fun notFoundServerError(errorCode: ApplicationErrorCode, message: String) = ServerResponse.status(NOT_FOUND)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse(errorCode.name, message)))

fun serverError(status: HttpStatus, errorCode: ApplicationErrorCode, message: String) = ServerResponse.status(status)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse(errorCode.name, message)))

fun serverError(status: HttpStatus, errorCode: String, message: String) = ServerResponse.status(status)
        .contentType(APPLICATION_JSON_UTF8).body(fromObject(ErrorResponse(errorCode, message)))