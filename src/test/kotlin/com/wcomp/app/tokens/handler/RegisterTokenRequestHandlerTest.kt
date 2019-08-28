package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTDecodeException
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.model.RegisterTokenRequest
import com.wcomp.app.tokens.service.TokenService
import com.wcomp.app.tokens.connect.AccessTokenException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import reactor.core.publisher.Mono
import javax.validation.Validation

@UnitTest
class RegisterTokenRequestHandlerTest {

    @Test
    fun `should return 400 when access token is not valid`() {
        // Given
        val handler = RegisterTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(RegisterTokenRequest(idToken = "id-token-valid", deviceId = "device-id-is-here", accessToken = "access-token-not-valid")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `should return 403 when id-token is not valid`() {
        // Given
        val handler = RegisterTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(RegisterTokenRequest(idToken = "id-token-not-valid", deviceId = "device-id-is-here", accessToken = "access-token-valid")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(FORBIDDEN)
    }

    @Test
    fun `should return 200 when register token succeed`() {
        // Given
        val handler = RegisterTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(RegisterTokenRequest(idToken = "id-token-valid", deviceId = "device-id-is-here", accessToken = "access-token-valid")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(OK)
    }

    private class TokenServiceMock: TokenService {
        override fun registerToken(idToken: String, accessToken: String, deviceId: String): Mono<Account> {
            return when (Pair(idToken, accessToken)) {
                Pair("id-token-valid", "access-token-not-valid") -> Mono.error(AccessTokenException(BAD_REQUEST, "bad access token"))
                Pair("id-token-not-valid", "access-token-valid") -> Mono.error(JWTDecodeException("bad id token"))
                Pair("id-token-valid", "access-token-valid") -> Mono.just(Account(iuc = "iuc", devices = emptyList()))
                else -> Mono.empty()
            }
        }

        override fun saveMaasToken(idToken: String, deviceId: String, maasToken: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun logoutAndInvalidateRefreshToken(idToken: String, refreshToken: String, deviceId: String): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}