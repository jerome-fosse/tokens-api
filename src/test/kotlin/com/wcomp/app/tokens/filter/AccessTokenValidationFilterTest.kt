package com.wcomp.app.tokens.filter

import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.model.AccountInfo
import com.wcomp.app.tokens.connect.model.CreateAccount
import com.wcomp.app.tokens.connect.model.TokenInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.*

@UnitTest
class AccessTokenValidationFilterTest {

    @Test
    fun `should return 400 Bad request when X-access-token header is missing`() {
        // Given
        val connectClient = MockConnectClient()
        val toTest = AccessTokenValidationFilter(true, connectClient)
        val successExecution = HandlerFunction { ServerResponse.ok().build() }
        val serverRequest = MockServerRequest.builder().build()

        // When
        val serverResponse = toTest.filter(serverRequest, successExecution).block()

        // Then
        assertThat(serverResponse?.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(connectClient.accessTokenValidated).isFalse()
    }

    @Test
    fun `should call Connect access token validation when X-access-token available and feature enabled`() {
        // Given
        val connectClient = MockConnectClient()
        val toTest = AccessTokenValidationFilter(true, connectClient)
        val successExecution = HandlerFunction { ServerResponse.ok().build() }
        val serverRequest = MockServerRequest.builder().header("X-access-token", "accessToken").build()

        // When
        val serverResponse = toTest.filter(serverRequest, successExecution).block()

        // Then
        assertThat(serverResponse?.statusCode()).isEqualTo(HttpStatus.OK)
        assertThat(connectClient.accessToken).isEqualTo("accessToken")
        assertThat(connectClient.accessTokenValidated).isTrue()
    }

    @Test
    fun `should not validate access token when feature disabled`() {
        // Given
        val connectClient = MockConnectClient()
        val disabled = AccessTokenValidationFilter(false, connectClient)

        val successExecution = HandlerFunction { ServerResponse.ok().build() }
        val serverRequest = MockServerRequest.builder().build()

        // When
        val serverResponse = disabled.filter(serverRequest, successExecution).block()

        // Then
        assertThat(serverResponse?.statusCode()).isEqualTo(HttpStatus.OK)
        assertThat(connectClient.accessTokenValidated).isFalse()
    }

    private inner class MockConnectClient: ConnectClient {
        var accessToken = ""
        var accessTokenValidated = false

        override fun createAccount(createAccountRequest: CreateAccount, toMigrate: Boolean, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun invalidRefreshToken(refreshToken: String): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun validateAccessToken(accessToken: String): Mono<TokenInfo> {
            this.accessToken = accessToken
            accessTokenValidated = true
            return Mono.just(TokenInfo(emptyList(), "grantType", "realm","openid", "tokenType", 1234566778, "accessToken", "profile"))
        }

        override fun getAccountInfo(iuc: String): Mono<AccountInfo> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
