package com.wcomp.app.tokens.handler

import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.PartnerException
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.model.ConnectAccountInfoResponse
import com.wcomp.app.tokens.model.ConnectCreateAccountRequest
import com.wcomp.app.tokens.service.AccountService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.validation.Validation

@UnitTest
class CreateAccountRequestHandlerTest {

    @Test
    fun `should return HttpCode provided by PartnerException`() {
        // Given
        val handler = CreateAccountRequestHandler(AccountServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectCreateAccountRequest(firstname = "ko", lastname = "lastname", email = "email@wcomp.com", password = "password", language = "fr", birthDate = ZonedDateTime.now())))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT)
    }

    @Test
    fun `should return 200 when accout created`() {
        // Given
        val handler = CreateAccountRequestHandler(AccountServiceMock(), SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().validator))
        val serverRequest = MockServerRequest.builder()
                .header("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(Mono.just(ConnectCreateAccountRequest(firstname = "ok", lastname = "lastname", email = "email@wcomp.com", password = "password", language = "fr", birthDate = ZonedDateTime.now())))

        // When
        val serverResponse = handler.handleRequest(serverRequest).block()

        // Then
        assertThat(serverResponse!!.statusCode()).isEqualTo(HttpStatus.OK)
    }

    private class AccountServiceMock: AccountService {
        override fun getAccountWithActiveDevices(iuc: String, active: Boolean): Mono<Account> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun createAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
            return when {
                firstname == "ko" -> Mono.error(PartnerException("Connect", HttpStatus.I_AM_A_TEAPOT, "WTF ?"))
                else -> Mono.empty()
            }
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