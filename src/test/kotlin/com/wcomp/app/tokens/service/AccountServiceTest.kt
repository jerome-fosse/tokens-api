package com.wcomp.app.tokens.service

import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.cache.AccountInfoCacheRepository
import com.wcomp.app.tokens.connect.model.AccountInfo
import com.wcomp.app.tokens.connect.model.CreateAccount
import com.wcomp.app.tokens.connect.model.TokenInfo
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.data.repository.AccountRepository
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.exeption.NotMatchingDataException
import com.wcomp.app.tokens.utils.JWTValidator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@ExtendWith(MockitoExtension::class)
@UnitTest
class AccountServiceTest {
    companion object {
        private const val RSA_KEY =
                """-----BEGIN PUBLIC KEY-----
                MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6vB+aOUF7s5sKGLCQPA2
                bsG9KG3RuoFYpVWTc1/wpHMI+QSE3Hcu540wf00O059NDwVUEl4XaRBfz1KtBx8u
                IiMX68xdRDnEeqxjnOCx2zyuCW84dqj1c1VuqSw14Qf8syCBNqSERGchtCioYLKs
                Xf9qceIscVmm2kjVHzxhNz2wvwVDrXZTI7mITGl4UxAUoiP900ohln1aW7zYao8l
                9Jv9kamR81fDnaOkK+WshSqm4ktfa7CQNS7d50w63K7kQ8balQ7jokIN3RLB6LFS
                ZTosbv/B+S9skBn/aQxM0jIH6bh74OZd6onCl/rfZ3K/fp1DKIRpJzs4Md7X6PGa
                iQIDAQAB
                -----END PUBLIC KEY-----"""

        private val jwtValidator = JWTValidator(RSA_KEY)
        private val accountRepository = mock(AccountRepository::class.java)
        private val accountInfoCacheRepository = mock(AccountInfoCacheRepository::class.java)
        private val fakeTokenGenerator = FakeTokenGenerator()
    }

    @Test
    fun `should return an account without inactive devices`() {
        // Given an Account with 2 actives devices and 2 inactives devices
        val a = Account(iuc = "123456789", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "03", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "04", lastSeen = LocalDateTime.now(), active = true)
        ))
        `when`(accountRepository.findById("123456789")).thenReturn(Mono.just(a))
        val connectClient = MockConnectClient()

        // When I get the account 123456789 with active devices
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val account = service.getAccountWithActiveDevices("123456789", true).block()

        // Then account id = 123456789
        assertThat(account?.iuc).isEqualTo("123456789")

        // And all devices are actives
        assertThat(account?.devices?.size).isEqualTo(2)
        assertThat(account?.devices?.get(0)?.deviceId).isEqualTo("01")
        assertThat(account?.devices?.get(0)?.active).isTrue()
        assertThat(account?.devices?.get(1)?.deviceId).isEqualTo("04")
        assertThat(account?.devices?.get(1)?.active).isTrue()
    }

    @Test
    fun `should return an account without active devices`() {
        // Given an Account with 2 actives devices and 2 inactives devices
        val a = Account(iuc = "123456789", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "03", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "04", lastSeen = LocalDateTime.now(), active = true)
        ))
        `when`(accountRepository.findById("123456789")).thenReturn(Mono.just(a))
        val connectClient = MockConnectClient()

        // When I get the account 123456789 with active devices
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val account = service.getAccountWithActiveDevices("123456789", false).block()

        // Then account id = 123456789
        assertThat(account?.iuc).isEqualTo("123456789")

        // And all devices are inactive
        assertThat(account?.devices?.size).isEqualTo(2)
        assertThat(account?.devices?.get(0)?.deviceId).isEqualTo("02")
        assertThat(account?.devices?.get(0)?.active).isFalse()
        assertThat(account?.devices?.get(1)?.deviceId).isEqualTo("03")
        assertThat(account?.devices?.get(1)?.active).isFalse()
    }

    @Test
    fun `should throw an AccountNotFoundException when the account does not exists`() {
        // When the repository does not found the account 123456789
        `when`(accountRepository.findById("123456789")).thenReturn(Mono.empty())
        val connectClient = MockConnectClient()

        // When I get the account 123456789 with active devices
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountWithActiveDevices("123456789", true).block() }

        // Then an AccountNotFoundException is thrown
        assertThat(thrown).isNotNull()
        assertThat(thrown).isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `createAccount should call ConnectClient with ToMigrate flag to false`() {
        val createAccount = CreateAccount(email = "youpi@yopmail.com", password = "pass;1234", firstname = "Blaise", lastname = "S", birthdate = LocalDate.of(1922, 4, 1))
        val connectClient = MockConnectClient()

        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val result = service.createAccount(createAccount.email, createAccount.password, createAccount.firstname, createAccount.lastname, createAccount.birthdate, Locale.FRENCH).block()

        assertThat(result).isTrue()
        assertThat(connectClient.createAccount.first).isEqualTo(createAccount)
        assertThat(connectClient.createAccount.second).isEqualTo(false)
        assertThat(connectClient.createAccount.third).isEqualTo(Locale.FRENCH)
    }

    @Test
    fun `create account should call ConnectClient with ToMigrate flag to true`() {
        val createAccount = CreateAccount(email = "youpi@yopmail.com", password = "pass;1234", firstname = "Blaise", lastname = "S", birthdate = LocalDate.of(1922, 4, 1))
        val connectClient = MockConnectClient()

        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val result = service.migrateAccount(createAccount.email, createAccount.password, createAccount.firstname, createAccount.lastname, createAccount.birthdate, Locale.FRENCH).block()

        assertThat(result).isTrue()
        assertThat(connectClient.createAccount.first).isEqualTo(createAccount)
        assertThat(connectClient.createAccount.second).isEqualTo(true)
        assertThat(connectClient.createAccount.third).isEqualTo(Locale.FRENCH)
    }

    @Test
    fun `getAccountInfo should throw NoMatchindDataException when there is no account`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(Mono.empty())

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountInfo("deviceId", token).block() }

        // Then
        assertThat(thrown).isInstanceOf(NotMatchingDataException::class.java)
    }

    @Test
    fun `getAccountInfo should throw NoMatchindDataException when no device`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Mono.just(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = emptyList()))
        )

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountInfo("deviceId", token).block() }

        // Then
        assertThat(thrown).isInstanceOf(NotMatchingDataException::class.java)
    }

    @Test
    fun `getAccountInfo should throw NoMatchindDataException when device is inactive`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val device = Device(deviceId = "deviceId", active = false, lastSeen = LocalDateTime.now())
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Mono.just(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(device)))
        )

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountInfo("deviceId", token).block() }

        // Then
        assertThat(thrown).isInstanceOf(NotMatchingDataException::class.java)
    }

    @Test
    fun `getAccountInfo should throw NoMatchindDataException when no device matching deviceId`() {
        //Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Mono.just(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                        Device(deviceId = "wrongDeviceId", active = true, lastSeen = LocalDateTime.now())
                )))
        )

        //When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountInfo("deviceId", token).block() }

        // Then
        assertThat(thrown).isInstanceOf(NotMatchingDataException::class.java)
    }

    @Test
    fun `getAccountInfo should not call Connect when device found and Token is valid and data in cache`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Mono.just(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                        Device(deviceId = "deviceId", active = true, lastSeen = LocalDateTime.now())
                )))
        )
        `when`(accountInfoCacheRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Optional.of(AccountInfo(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", email = "mission.impossible@connect.fr", firstName = "Jim", lastName = "Phelps"))
        )

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val accountInfo = service.getAccountInfo("deviceId", token).block()

        // Then
        assertThat(accountInfo?.id).isEqualTo("35adcf57-2cf7-4945-a980-e9753eb146f7")
        assertThat(accountInfo?.email).isEqualTo("mission.impossible@connect.fr")
        assertThat(accountInfo?.firstname).isEqualTo("Jim")
        assertThat(accountInfo?.lastname).isEqualTo("Phelps")
    }

    @Test
    fun `getAccountInfo should call Connect when device is found and idToken is valid and data is not in cache`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val accinfo = AccountInfo(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", email = "mission.impossible@connect.fr", firstName = "Jim", lastName = "Phelps")
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(
                Mono.just(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                        Device(deviceId = "deviceId", active = true, lastSeen = LocalDateTime.now())
                )))
        )
        `when`(accountInfoCacheRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(Optional.empty())
        `when`(accountInfoCacheRepository.save(accinfo)).thenReturn(accinfo)

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val accountInfo = service.getAccountInfo("deviceId", token).block()

        // Then
        assertThat(accountInfo?.id).isEqualTo("35adcf57-2cf7-4945-a980-e9753eb146f7")
        assertThat(accountInfo?.email).isEqualTo("mission.impossible@connect.fr")
        assertThat(accountInfo?.firstname).isEqualTo("Jim")
        assertThat(accountInfo?.lastname).isEqualTo("Phelps")
    }

    @Test
    fun `getAccountWithDevice should throw AccountNotFoundException when there is no Account for an idToken`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(Mono.empty())

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountWithDevice(token, "deviceId").block() }

        // Then
        assertThat(thrown).isNotNull()
        assertThat(thrown).isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `getAccountWithDevice should throw NotMatchingDataException when there is an Account for an idToken but that account does not have a device with the given deviceId`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(Mono.just(
                Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                        Device("badId1", lastSeen = LocalDateTime.now(), active = true),
                        Device("badId2", lastSeen = LocalDateTime.now(), active = true)
                ))
        ))

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val thrown = catchThrowable { service.getAccountWithDevice(token, "deviceId").block() }

        // Then
        assertThat(thrown).isNotNull()
        assertThat(thrown).isInstanceOf(NotMatchingDataException::class.java)
    }

    @Test
    fun `getAccountWithDevice should return an account with its devices when an account exist with the given deviceId`() {
        // Given
        val expireAt = Date.from(LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        val token = fakeTokenGenerator.generateSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7", expireAt)
        val connectClient = MockConnectClient()
        `when`(accountRepository.findById("35adcf57-2cf7-4945-a980-e9753eb146f7")).thenReturn(Mono.just(
                Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                        Device("badId", lastSeen = LocalDateTime.now(), active = true),
                        Device("deviceId", lastSeen = LocalDateTime.now(), active = true)
                ))
        ))

        // When
        val service = AccountServiceImpl(jwtValidator, connectClient, accountRepository, accountInfoCacheRepository)
        val account = service.getAccountWithDevice(token, "deviceId").block()

        // Then
        assertThat(account).isNotNull()
        assertThat(account.iuc).isEqualTo("35adcf57-2cf7-4945-a980-e9753eb146f7")
        assertThat(account.devices.size).isEqualTo(2)
        assertThat(account.devices[0].deviceId).isEqualTo("badId")
        assertThat(account.devices[1].deviceId).isEqualTo("deviceId")
    }

    private inner class MockConnectClient: ConnectClient {
        lateinit var createAccount: Triple<CreateAccount, Boolean, Locale>
        lateinit var getAccountInfo: String

        override fun createAccount(createAccountRequest: CreateAccount, toMigrate: Boolean, language: Locale): Mono<Boolean> {
            createAccount = Triple(createAccountRequest, toMigrate, language)

            return when (createAccountRequest) {
                CreateAccount(
                        email = "youpi@yopmail.com",
                        password = "pass;1234",
                        firstname = "Blaise",
                        lastname = "S",
                        birthdate = LocalDate.of(1922, 4, 1)) -> Mono.just(true)
                else -> Mono.empty()
            }
        }

        override fun invalidRefreshToken(refreshToken: String): Mono<Boolean> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun validateAccessToken(accessToken: String): Mono<TokenInfo> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getAccountInfo(iuc: String): Mono<AccountInfo> {
            this.getAccountInfo = iuc
            return when (iuc) {
                "35adcf57-2cf7-4945-a980-e9753eb146f7" -> Mono.just(AccountInfo(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", email = "mission.impossible@connect.fr", firstName = "Jim", lastName = "Phelps"))
                else -> Mono.empty()
            }
        }
    }
}