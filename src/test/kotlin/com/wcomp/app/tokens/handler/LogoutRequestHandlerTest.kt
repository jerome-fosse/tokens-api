package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTDecodeException
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.InvalidRefreshTokenException
import com.wcomp.app.tokens.connect.PartnerException
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.model.ConnectLogoutRequest
import com.wcomp.app.tokens.service.TokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import reactor.core.publisher.Mono
import javax.validation.Validation

@UnitTest
class LogoutRequestHandlerTest {

    @Test
    fun `should return 400 when refresh token is invalid`() {
        // Given
        val handler = LogoutRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectLogoutRequest(idToken = "id-token", refreshToken = "bad-refresh-token", deviceId = "device-id")))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should return 403 when id token is invalid`() {
        // Given
        val handler = LogoutRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectLogoutRequest(idToken = "bad-id-token", refreshToken = "refresh-token", deviceId = "device-id")))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `should return 200 provided by PartnerException`() {
        // Given
        val handler = LogoutRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectLogoutRequest(idToken = "connect-error", refreshToken = "refresh-token", deviceId = "device-id")))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT)
    }

    @Test
    fun `should return 200 when logout`() {
        // Given
        val handler = LogoutRequestHandler(TokenServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectLogoutRequest(idToken = "id-token", refreshToken = "refresh-token", deviceId = "device-id")))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.OK)
    }

    private class TokenServiceMock: TokenService {
        override fun registerToken(idToken: String, accessToken: String, deviceId: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun saveMaasToken(idToken: String, deviceId: String, maasToken: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun logoutAndInvalidateRefreshToken(idToken: String, refreshToken: String, deviceId: String): Mono<Boolean> {
            return when {
                refreshToken == "bad-refresh-token" -> Mono.error(InvalidRefreshTokenException(HttpStatus.FORBIDDEN, "ko"))
                idToken == "bad-id-token" -> Mono.error(JWTDecodeException("bad id token"))
                idToken == "connect-error" -> Mono.error(PartnerException("Connect", HttpStatus.I_AM_A_TEAPOT, "error"))
                else -> Mono.empty()
            }
        }
    }
}