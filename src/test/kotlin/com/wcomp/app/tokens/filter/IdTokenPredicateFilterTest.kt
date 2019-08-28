package com.wcomp.app.tokens.filter

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerResponse

@UnitTest
class IdTokenPredicateFilterTest {

    @Test
    fun `filter should return 400 when access token is missing`() {
        // Given
        val filter = IdTokenPredicateFilter()
        val handler = HandlerFunction { ServerResponse.ok().build() }
        val serverRequest = MockServerRequest.builder().build()

        // When
        val response = filter.filter(serverRequest, handler).block()

        // Then
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should proceed to next handler when X-id-token header is present`() {
        // Given
        val filter = IdTokenPredicateFilter()
        val handler = HandlerFunction { ServerResponse.ok().build() }
        val serverRequest = MockServerRequest.builder()
                .header("X-id-token", "id-token")
                .build()

        // When
        val response = filter.filter(serverRequest, handler).block()

        // Then
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK)
    }
}