package com.wcomp.app.tokens.handler

import com.auth0.jwt.exceptions.JWTDecodeException
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.PartnerException
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.exeption.NotMatchingDataException
import com.wcomp.app.tokens.model.ConnectAccountInfoResponse
import com.wcomp.app.tokens.model.DeviceResponse
import com.wcomp.app.tokens.service.AccountService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@UnitTest
class AccountInfoRequestHandlerTest {

    @Test
    fun `should return 403 when id-token is not valid`() {
        // Given
        val handler = AccountInfoRequestHandler(AccountServiceMock())
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-not-valid")
                .queryParam("deviceId", "my-device-id")
                .build()

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `should return 400 when no data found with criterias`() {
        // Given
        val handler = AccountInfoRequestHandler(AccountServiceMock())
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-not-found")
                .queryParam("deviceId", "my-device-id")
                .build()

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should return HttpCode provided by PartnerException`() {
        // Given
        val handler = AccountInfoRequestHandler(AccountServiceMock())
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-partner-ko")
                .queryParam("deviceId", "my-device-id")
                .build()

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT)
    }

    @Test
    fun `should return 200 when account found with criteria`() {
        // Given
        val handler = AccountInfoRequestHandler(AccountServiceMock())
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("X-id-token", "id-token-ok")
                .queryParam("deviceId", "my-device-id")
                .build()

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.OK)
    }

    private class AccountServiceMock : AccountService {
        override fun getAccountWithActiveDevices(iuc: String, active: Boolean): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun createAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun migrateAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getAccountInfo(deviceId: String, idToken: String): Mono<ConnectAccountInfoResponse> {
            return when (idToken) {
                "id-token-not-valid" -> Mono.error(JWTDecodeException("bad id token"))
                "id-token-not-found" -> Mono.error(NotMatchingDataException("User not found"))
                "id-token-partner-ko" -> Mono.error(PartnerException("Connect", HttpStatus.I_AM_A_TEAPOT, "WTF ?"))
                "id-token-ok" -> Mono.just(ConnectAccountInfoResponse(id = "iuc", lastname = "lastname", firstname = "firstname", email = "email", device = DeviceResponse(deviceId = "01", active = true, lastSeen = LocalDateTime.now())))
                else -> Mono.empty()
            }
        }

        override fun getAccountWithDevice(idToken: String, deviceId: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}