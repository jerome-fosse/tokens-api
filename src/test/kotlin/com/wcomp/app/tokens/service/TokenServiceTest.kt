package com.wcomp.app.tokens.service

import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.model.AccountInfo
import com.wcomp.app.tokens.connect.model.CreateAccount
import com.wcomp.app.tokens.connect.model.TokenInfo
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.data.repository.AccountRepository
import com.wcomp.app.tokens.utils.JWTValidator
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@UnitTest
@ExtendWith(MockitoExtension::class)
class TokenServiceTest {

    companion object {
        const val RSA_KEY = """-----BEGIN PUBLIC KEY-----
                            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6vB+aOUF7s5sKGLCQPA2
                            bsG9KG3RuoFYpVWTc1/wpHMI+QSE3Hcu540wf00O059NDwVUEl4XaRBfz1KtBx8u
                            IiMX68xdRDnEeqxjnOCx2zyuCW84dqj1c1VuqSw14Qf8syCBNqSERGchtCioYLKs
                            Xf9qceIscVmm2kjVHzxhNz2wvwVDrXZTI7mITGl4UxAUoiP900ohln1aW7zYao8l
                            9Jv9kamR81fDnaOkK+WshSqm4ktfa7CQNS7d50w63K7kQ8balQ7jokIN3RLB6LFS
                            ZTosbv/B+S9skBn/aQxM0jIH6bh74OZd6onCl/rfZ3K/fp1DKIRpJzs4Md7X6PGa
                            iQIDAQAB
                            -----END PUBLIC KEY-----"""

        private val accountRepository = mock(AccountRepository::class.java)
        private val fakeTokenGenerator = FakeTokenGenerator()
    }

    @Test
    fun `logout and invalidate RefreshToken should throw JWTVerificationException when idToken is not valid`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, true)

        // When && Asserts
        StepVerifier.create(tokenService.logoutAndInvalidateRefreshToken("badToken", "regreshToken", "deviceId"))
                .expectError()
                .verifyThenAssertThat()
                .hasOperatorErrorOfType(JWTVerificationException::class.java)
                .hasOperatorErrorWithMessageContaining("The token was expected to have 3 parts, but got 1.")
    }

    @Test
    fun `logout and invalidate RefreshToken should deactivate device and invalidate RefreshToken when idToken is valid`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, true)
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("Connect", expireAt)
        `when`(accountRepository.deactivateDeviceForAccount("deviceId", "Connect")).thenReturn(Mono.just(1L))

        // When
        val result = tokenService.logoutAndInvalidateRefreshToken(token, "refreshToken", "deviceId").block()

        // Then
        assertThat(result).isTrue()
        assertThat(connectClient.refreshToken).isEqualTo("refreshToken")
        assertThat(connectClient.invalidRefreshToken).isTrue()
    }

    @Test
    fun `should not validate access token when feature flag is set to false`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, false)
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("Connect", expireAt)
        `when`(accountRepository.deactivateDeviceForAccountsWhereUIDNotEqual("deviceId", "Connect")).thenReturn(Mono.just(1L))
        `when`(accountRepository.addDeviceToExistingAccountOrCreateNewAccount("deviceId", "Connect"))
                .thenReturn(Mono.just(Account(iuc = "Connect", devices = listOf(Device(deviceId = "deviceId", active = true, lastSeen = LocalDateTime.now())))))

        // When
        tokenService.registerToken(token, "accessToken", "deviceId").block()

        // Then
        assertThat(connectClient.validateAccessToken).isFalse()
    }

    @Test
    fun `should validate access token when feature flag is set to true`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, true)
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("Connect", expireAt)
        `when`(accountRepository.deactivateDeviceForAccountsWhereUIDNotEqual("deviceId", "Connect")).thenReturn(Mono.just(1L))
        `when`(accountRepository.addDeviceToExistingAccountOrCreateNewAccount("deviceId", "Connect"))
                .thenReturn(Mono.just(Account(iuc = "Connect", devices = listOf(Device(deviceId = "deviceId", active = true, lastSeen = LocalDateTime.now())))))

        // When
        tokenService.registerToken(token, "accessToken", "deviceId").block()

        // Then
        assertThat(connectClient.validateAccessToken).isTrue()
        assertThat(connectClient.accessToken).isEqualTo("accessToken")
    }

    @Test
    fun `should return an error when id-token is nit valid`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, true)

        // When
        val thrown = Assertions.catchThrowable { tokenService.registerToken("id-token", "accessToken", "deviceId").block() }

        // Then
        assertThat(thrown).isInstanceOf(JWTDecodeException::class.java)
    }

    @Test
    fun `should save maas token when account exists in database`() {
        // Given
        val jwtValidator = JWTValidator(RSA_KEY)
        val connectClient = MockConnectClient()
        val tokenService = TokenServiceImpl(jwtValidator, accountRepository, connectClient, true)
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("Connect", expireAt)
        `when`(accountRepository.saveMaasTokenForDeviceAndAccount(iuc = "Connect", deviceId = "my-device", maasToken = "my-maas-token"))
                .thenReturn(Mono.just(Account(iuc = "Connect", devices = listOf(Device(deviceId = "my-device", active = true, lastSeen = LocalDateTime.now(), maasToken = "my-maas-token")))))

        // When
        val account = tokenService.saveMaasToken(token, "my-device", "my-maas-token").block()

        // Then
        assertThat(account!!.iuc).isEqualTo("Connect")
        assertThat(account.devices.size).isEqualTo(1)
        assertThat(account.devices[0].deviceId).isEqualTo("my-device")
        assertThat(account.devices[0].maasToken).isEqualTo("my-maas-token")
    }

    private inner class MockConnectClient: ConnectClient {
        var refreshToken = ""
        var invalidRefreshToken = false

        var accessToken = ""
        var validateAccessToken = false

        override fun createAccount(createAccountRequest: CreateAccount, toMigrate: Boolean, language: Locale): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun invalidRefreshToken(refreshToken: String): Mono<Boolean> {
            this.refreshToken = refreshToken
            this.invalidRefreshToken = true
            return when (refreshToken) {
                "refreshToken" -> Mono.just(true)
                else -> Mono.empty()
            }
        }

        override fun validateAccessToken(accessToken: String): Mono<TokenInfo> {
            this.accessToken = accessToken
            this.validateAccessToken = true

            return when (accessToken) {
                "accessToken" -> Mono.just(TokenInfo(accessToken = "accessToken", expiresIn = 100, grantType = "GRANT", openid = "ID",
                        profile = "PROFILE", realm = "REALM", tokenType = "TYPE", scope = listOf("SCOPE")))
                else -> Mono.empty()
            }
        }

        override fun getAccountInfo(iuc: String): Mono<AccountInfo> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}