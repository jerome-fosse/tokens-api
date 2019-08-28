package com.wcomp.app.tokens.connect

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.model.AccountInfo
import com.wcomp.app.tokens.connect.model.CreateAccount
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import java.net.URLEncoder
import java.time.LocalDate
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@UnitTest
class ConnectClientTest {

    companion object {

        private const val CREATION_CALLBACK = "http://dummy.url.com"

        lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            wireMockServer = WireMockServer(options()
                    .dynamicPort()
                    .usingFilesUnderClasspath("com/wcomp/app/tokens/connect"))

            wireMockServer.start()
            configureFor("localhost", wireMockServer.port()) // indispensable en reactif

            wireMockServer.stubFor(post("/api/accounts/logout")
                    .withRequestBody(equalTo("{\"refreshToken\":\"123456789\"}"))
                    .willReturn(aResponse()
                            .withStatus(204)
                    )
            )

            wireMockServer.stubFor(post("/api/accounts/logout")
                    .withRequestBody(equalTo("{\"refreshToken\":\"987654321\"}"))
                    .willReturn(aResponse()
                            .withStatus(502)
                    )
            )

            wireMockServer.stubFor(post("/api/accounts/logout")
                    .withRequestBody(equalTo("{\"refreshToken\":\"111111111\"}"))
                    .willReturn(aResponse()
                            .withStatus(400)
                    )
            )

            wireMockServer.stubFor(get("/SvcECZ/oauth2/tokeninfo?access_token=047419a1-ca25-4f8c-8119-c3453522810f")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"scope\":[\"openid\",\"profile\"],\"grant_type\":\"authorization_code\",\"realm\":\"/connect\",\"openid\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":53,\"access_token\":\"047419a1-ca25-4f8c-8119-c3453522810f\",\"profile\":\"123\"}"))
            )

            wireMockServer.stubFor(get("/SvcECZ/oauth2/tokeninfo?access_token=123456789")
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\":\"invalid_request\",\"error_description\":\"Access Token not valid\"}"))
            )

            wireMockServer.stubFor(get("/api/accounts/1234")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"iuc\":\"1234\",\"email\":\"awsome@user.com\",\"firstName\":\"Awsome\",\"lastName\":\"User\"}"))
            )

            wireMockServer.stubFor(get("/api/accounts/4567")
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"email\":\"awsome@user.com\",\"firstName\":\"Awsome\",\"lastName\":\"User\"}"))
            )

        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            wireMockServer.stop()
        }
    }

    private val configuration = ConnectClientConfiguration(
            user = "user",
            password = "password",
            accountApiUrl = "http://localhost:" + wireMockServer.port() + "/api/accounts",
            openAMApiUrl = "http://localhost:" + wireMockServer.port() + "/SvcECZ/oauth2/",
            accountCreationCallbackUrl = CREATION_CALLBACK,
            accountCreationCallbackMobileUrl = CREATION_CALLBACK,
            sendAccountCreationNotifEmail = true
    )

    val webClient = WebClient.builder()
            .defaultHeader(ACCEPT, APPLICATION_JSON_UTF8_VALUE)
            .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
            .defaultHeader("X-App", "UNI")
            .filter(basicAuthentication("user","password"))
            .build()

    private val connectClient = DefaultConnectClient(configuration, webClient)

    @Test
    fun `should invalidate refresh token`() {
        val status = connectClient.invalidRefreshToken("123456789").block()
        assertThat(status).isTrue()
    }

    @Test
    fun `should not invalidate refresh token and throw InvalidRefreshTokenException`() {
        val thrown = catchThrowable { connectClient.invalidRefreshToken("987654321").block() }
        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException::class.java)
        val ex = thrown as InvalidRefreshTokenException
        assertThat(ex.httpStatusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
        assertThat(ex.error.errorDescription).isEqualTo("Invalid or expired refresh token!!!")
    }

    @Test
    fun `should not invalidate refresh token and throw PartnerException`() {
        val thrown = catchThrowable { connectClient.invalidRefreshToken("111111111").block() }
        assertThat(thrown).isInstanceOf(PartnerException::class.java)
        val ex = thrown as PartnerException
        assertThat(ex.httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `createAccount should call Connect with input parameters when always`() {
        // Arrange
        val callbackUrl = URLEncoder.encode(CREATION_CALLBACK, "UTF-8")
        val url = "/api/accounts?send_notif_email=true&callback_mobile=$callbackUrl&callback=$callbackUrl&is_migrated=false"
        wireMockServer.stubFor(post(url)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                )
        )

        val createAccount = CreateAccount(email = "youpi@dummy.fr", password = "pass;1234", firstname = "John", lastname = "Wayne",
                birthdate = LocalDate.of(2000, 1, 1))
        val language = Locale.ENGLISH

        // Test
        connectClient.createAccount(createAccount, false, language).block()
        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withHeader("Accept-Language", equalTo("en"))
                .withRequestBody(matchingJsonPath("$.email", equalTo("youpi@dummy.fr")))
                .withRequestBody(matchingJsonPath("$.password", equalTo("pass;1234")))
                .withRequestBody(matchingJsonPath("$.firstName", equalTo("John")))
                .withRequestBody(matchingJsonPath("$.lastName", equalTo("Wayne")))
                .withRequestBody(matchingJsonPath("$.birthdate", equalTo("2000-01-01")))
        )
    }

    @Test
    fun `createAccount should call Connect with input parameters and Locale French when language is not set`() {
        // Arrange
        val callbackUrl = URLEncoder.encode(CREATION_CALLBACK, "UTF-8")
        val url = "/api/accounts?send_notif_email=true&callback_mobile=$callbackUrl&callback=$callbackUrl&is_migrated=false"
        wireMockServer.stubFor(post(url)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                )
        )

        val createAccount = CreateAccount("youpi@dummy.fr", "pass;1234", "John", "Wayne", LocalDate.of(2000, 1, 1))

        // Test
        connectClient.createAccount(createAccount, false).block()

        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withHeader("Accept-Language", equalTo("fr"))
                .withRequestBody(matchingJsonPath("$.email", equalTo("youpi@dummy.fr")))
                .withRequestBody(matchingJsonPath("$.password", equalTo("pass;1234")))
                .withRequestBody(matchingJsonPath("$.firstName", equalTo("John")))
                .withRequestBody(matchingJsonPath("$.lastName", equalTo("Wayne")))
                .withRequestBody(matchingJsonPath("$.birthdate", equalTo("2000-01-01")))
        )
    }

    @Test
    fun `should get TokenInfo when access token is valid`() {
        val tokenInfo = connectClient.validateAccessToken("047419a1-ca25-4f8c-8119-c3453522810f").block()

        assertThat(tokenInfo?.grantType).isEqualTo("authorization_code")
        assertThat(tokenInfo?.realm).isEqualTo("/connect")
        assertThat(tokenInfo?.openid).isEqualTo("abc")
        assertThat(tokenInfo?.tokenType).isEqualTo("Bearer")
        assertThat(tokenInfo?.expiresIn).isEqualTo(53)
        assertThat(tokenInfo?.accessToken).isEqualTo("047419a1-ca25-4f8c-8119-c3453522810f")
        assertThat(tokenInfo?.profile).isEqualTo("123")
        assertThat(tokenInfo?.scope?.get(0)).isEqualTo("openid")
        assertThat(tokenInfo?.scope?.get(1)).isEqualTo("profile")
    }

    @Test
    fun `should raise an error when access token is invalid`() {
        val thrown = catchThrowable { connectClient.validateAccessToken("123456789").block() }

        assertThat(thrown).isNotNull()
        assertThat(thrown).isInstanceOf(AccessTokenException::class.java)
        val ex = thrown as AccessTokenException
        assertThat(ex.httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.error.errorDescription).isEqualTo("Access Token not valid")
    }

    @Test
    fun `getAccountInfo should return Account when ConnectResponse is OK`() {
        // Given && When
        val accountInfo = connectClient.getAccountInfo("1234").block()

        // Then
        assertThat(accountInfo).isNotNull
        assertThat(accountInfo).isEqualToIgnoringNullFields(AccountInfo(iuc = "1234", email = "awsome@user.com", firstName = "Awsome", lastName = "User"))
    }

    @Test
    fun `getAccountInfo should raise PartnerError when ConnectResponse is not OK`() {
        // Given && When
        val thrown = catchThrowable { connectClient.getAccountInfo("4567").block() }

        // Then
        assertThat(thrown).isNotNull()
        assertThat(thrown).isInstanceOf(PartnerException::class.java)
        val ex = thrown as PartnerException
        assertThat(ex.httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.error.errorDescription).isEqualTo("Unexpected response from the server while retrieving accountInfo for iuc=4567, response={\"email\":\"awsome@user.com\",\"firstName\":\"Awsome\",\"lastName\":\"User\"}")
    }
}
