package com.wcomp.app.tokens.it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.wcomp.app.tokens.DockerTest
import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.IntegrationTest
import com.wcomp.app.tokens.UsingMongoDB
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.model.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tests d'intégration vérifiant la validation de l'access token
 *  - Connect est mocké avec Wiremock
 *  - MongoDB tourne dans un conteneur Docker
 *  - Redis est désactivé
 *  - la validation de l'access token est validée
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [FeatureFlippingIntegrationTest.Initializer::class])
@ActiveProfiles("test", "logtxt")
@IntegrationTest
@DockerTest
class FeatureFlippingIntegrationTest(@LocalServerPort private val localServerPort: Int, @Autowired private val mongoOperations: ReactiveMongoOperations): UsingMongoDB() {
    companion object {
        private lateinit var connectMock: WireMockServer

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            connectMock = WireMockServer(WireMockConfiguration.options().dynamicPort())

            connectMock.stubFor(get("/SvcECZ/oauth2/tokeninfo?access_token=8c9541d2-8846-4fe6-aed3-111059ac70d8")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"scope\":[\"openid\",\"profile\"],\"grant_type\":\"authorization_code\",\"realm\":\"/connect\",\"openid\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":53,\"access_token\":\"047419a1-ca25-4f8c-8119-c3453522810f\",\"profile\":\"123\"}")
                    )
            )

            connectMock.stubFor(get("/SvcECZ/oauth2/tokeninfo?access_token=123456789")
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\":\"invalid_request\",\"error_description\":\"Access Token not valid\"}")
                    )
            )

            connectMock.start()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            connectMock.stop()
        }
    }

    private lateinit var baseUrl: String
    private val fakeTokenGenerator = FakeTokenGenerator()

    @BeforeEach
    fun beforeEach() {
        baseUrl = "http://localhost:$localServerPort"
    }

    @AfterEach
    fun afterEach() {
        mongoOperations.dropCollection(Account::class.java).block()
        connectMock.resetRequests()
    }

    /* Register Token Integration Tests */

    @Test
    fun `should register tokens and return http code 200`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // And a Request with a valid idToken
        val validIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7")
        val request = "{\"idToken\": \"$validIdToken\", \"accessToken\": \"8c9541d2-8846-4fe6-aed3-111059ac70d8\", \"deviceId\": \"5c6124afe4b049253e8bd468\"}"
        val entity = HttpEntity(request, headers)

        // When I submit a register token reequest
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, Void::class.java)

        // I have a Http Code 200
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        connectMock.verify(1, getRequestedFor(urlEqualTo("/SvcECZ/oauth2/tokeninfo?access_token=8c9541d2-8846-4fe6-aed3-111059ac70d8")))

        // And I have an account created in db with uic = 35adcf57-2cf7-4945-a980-e9753eb146f7
        val acc = mongoOperations.findOne(Query.query(Criteria.where("_id").`is`("35adcf57-2cf7-4945-a980-e9753eb146f7")), Account::class.java).block()
        assertThat(acc).isNotNull()
        assertThat(acc!!.devices.size).isEqualTo(1)
        assertThat(acc.devices[0].deviceId).isEqualTo("5c6124afe4b049253e8bd468")
        assertThat(acc.devices[0].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc.devices[0].active).isTrue()
    }

    @Test
    fun `should return an error 400 when access token is not valid`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // And a Request with a valid idToken
        val validIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7")
        val request = "{\"idToken\": \"$validIdToken\", \"accessToken\": \"123456789\", \"deviceId\": \"5c6124afe4b049253e8bd468\"}"
        val entity = HttpEntity(request, headers)

        // When I submit a register token reequest
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, Void::class.java)

        // I have a Http Code 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        connectMock.verify(1, getRequestedFor(urlEqualTo("/SvcECZ/oauth2/tokeninfo?access_token=123456789")))
    }

    /* Save Maas Token Integration Tests */

    @Test
    fun `should raise an error if X-access-token is missing`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        // And an account in database
        mongoOperations.save(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When I save the maasToken
        val request = "{\"deviceId\":\"01\", \"deviceTokenMaas\":\"maas01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, ErrorResponse::class.java)

        // Then Http Status is 400 because X-access-token is missing
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body.code).isEqualTo("CONNECT_BAD_REQUEST")
        assertThat(response.body.message).isEqualTo("Missing request header 'X-access-token'")

        // And maas token has not been saved
        val account = mongoOperations.findById("35adcf57-2cf7-4945-a980-e9753eb146f7", Account::class.java).block()
        assertThat(account).isNotNull
        assertThat(account!!.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isNull()
    }

    @Test
    fun `should raise and error when X-access-token is not valid`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))
        headers.set("X-access-token", "123456789")

        // And an account in database
        mongoOperations.save(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When I save the maasToken
        val request = "{\"deviceId\":\"01\", \"deviceTokenMaas\":\"maas01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, ErrorResponse::class.java)

        // Then Http Status is 400 because X-access-token is missing
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body.code).isEqualTo("CONNECT_ACCESS_TOKEN_ERROR")
        assertThat(response.body.message).isEqualTo("Access Token not valid")
        connectMock.verify(1, getRequestedFor(urlEqualTo("/SvcECZ/oauth2/tokeninfo?access_token=123456789")))

        // And maas token has not been saved
        val account = mongoOperations.findById("35adcf57-2cf7-4945-a980-e9753eb146f7", Account::class.java).block()
        assertThat(account).isNotNull
        assertThat(account!!.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isNull()
    }

    @Test
    fun `should update device with maas token and validate access token`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))
        headers.set("X-access-token", "8c9541d2-8846-4fe6-aed3-111059ac70d8")

        // And an account in database
        mongoOperations.save(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When I save the maasToken
        val request = "{\"deviceId\":\"01\", \"deviceTokenMaas\":\"maas01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, Void::class.java)

        // Then Http Status is 204
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // The access token has been validated
        connectMock.verify(1, getRequestedFor(urlEqualTo("/SvcECZ/oauth2/tokeninfo?access_token=8c9541d2-8846-4fe6-aed3-111059ac70d8")))

        // And maas token has been saved
        val account = mongoOperations.findById("35adcf57-2cf7-4945-a980-e9753eb146f7", Account::class.java).block()
        assertThat(account).isNotNull
        assertThat(account!!.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isEqualTo("maas01")
    }


    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                    "wcomp.connect.base-url=http://localhost:${connectMock.port()}",
                    "features.access-token-validation.enabled: true",
                    "features.access-token-validation.controller.enabled: true"
            ).applyTo(applicationContext.environment)
        }
    }
}