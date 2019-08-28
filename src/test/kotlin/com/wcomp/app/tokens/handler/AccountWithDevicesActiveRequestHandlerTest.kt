package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.model.ConnectAccountInfoResponse
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
class AccountWithDevicesActiveRequestHandlerTest {

    @Test
    fun `should return 404 when account not found`() {
        // Given
        val handler = AccountWithDevicesActiveRequestHandler(AccountServiceMock())
        val request = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .pathVariable("iuc", "iuc-not-found")
                .queryParam("active", "true")
                .build()

        // When
        val response = handler.handleRequest(request).block()

        // Then
        assertThat(response!!.statusCode()).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should return 200 when account found`() {
        // Given
        val handler = AccountWithDevicesActiveRequestHandler(AccountServiceMock())
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .pathVariable("iuc", "iuc-found")
                .queryParam("active", "true")
                .build()

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.OK)
    }

    private class AccountServiceMock: AccountService {
        override fun getAccountWithActiveDevices(iuc: String, active: Boolean): Mono<Account> {
            return when (iuc) {
                "iuc-not-found" -> Mono.error(AccountNotFoundException(iuc))
                "iuc-found" -> Mono.just(Account(iuc = iuc, devices = listOf(Device(deviceId = "1", active = true, lastSeen = LocalDateTime.now()))))
                else -> Mono.empty()
            }
        }

        override fun createAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun migrateAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getAccountInfo(deviceId: String, idToken: String): Mono<ConnectAccountInfoResponse> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getAccountWithDevice(idToken: String, deviceId: String): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}