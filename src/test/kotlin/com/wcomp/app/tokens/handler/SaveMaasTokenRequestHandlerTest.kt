package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.model.SaveMaasTokenRequest
import com.wcomp.app.tokens.service.TokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import reactor.core.publisher.Mono
import javax.validation.Validation

@UnitTest
class SaveMaasTokenRequestHandlerTest {

    @Test
    fun `should return 404 when account does not exist`() {
        // Given
        val handler = SaveMaasTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-not-found")
                .body(Mono.just(SaveMaasTokenRequest(deviceId = "device-id-is-here", deviceTokenMaas = "random-maas-token")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should return 403 when id-token is not valid`() {
        // Given
        val handler = SaveMaasTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-not-valid")
                .body(Mono.just(SaveMaasTokenRequest(deviceId = "device-id-is-here", deviceTokenMaas = "random-maas-token")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `should return 204 when id-token is valid`() {
        // Given
        val handler = SaveMaasTokenRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val request = MockServerRequest.builder()
                .header("Accept", APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-valid")
                .body(Mono.just(SaveMaasTokenRequest(deviceId = "device-id-is-here", deviceTokenMaas = "random-maas-token")))

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.NO_CONTENT)
    }

    private class TokenServiceMock: TokenService {
        override fun registerToken(idToken: String, accessToken: String, deviceId: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun saveMaasToken(idToken: String, deviceId: String, maasToken: String): Mono<Account> {
            return when (idToken) {
                "id-token-not-found" -> Mono.error(AccountNotFoundException("iuc-not-found"))
                "id-token-not-valid" -> Mono.error(JWTVerificationException("id-token not valid"))
                "id-token-valid" -> Mono.just(Account(iuc = "iuc", devices = emptyList()))
                else  -> Mono.empty()
            }
        }

        override fun logoutAndInvalidateRefreshToken(idToken: String, refreshToken: String, deviceId: String): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
