package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import javax.validation.Validation
import javax.validation.constraints.Size

@UnitTest
class ValidationHandlerTest {

    @Test
    fun `should return an error when validation fail`() {
        // Given a bean to validate that will generate an error
        val myBean = MyBean("12345")
        // And a Handler
        val handler = MyHandler(SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))

        // When I handle a request
        val request = MockServerRequest.builder().body(Mono.just(myBean))
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should return ok when validation pass`() {
        // Given a bean to validate that will be validated with success
        val myBean = MyBean("1234567890123")
        // And a Handler
        val handler = MyHandler(SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))

        // When I handle a request
        val request = MockServerRequest.builder().body(Mono.just(myBean))
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.OK)
    }
}

private data class MyBean(
        @get:Size(min = 10)
        val property: String
)

private class MyHandler(validator: Validator) : AbstractValidationHandler<MyBean>(validator) {
    override fun processBody(body: MyBean, request: ServerRequest): Mono<ServerResponse> {
        return ServerResponse.ok().build()
    }
}
