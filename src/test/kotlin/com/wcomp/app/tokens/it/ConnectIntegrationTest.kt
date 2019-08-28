package com.wcomp.app.tokens.it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.wcomp.app.tokens.DockerTest
import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.IntegrationTest
import com.wcomp.app.tokens.UsingMongoDBAndRedis
import com.wcomp.app.tokens.connect.cache.AccountInfoCacheRepository
import com.wcomp.app.tokens.connect.model.AccountInfo
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.model.ConnectAccountInfoResponse
import com.wcomp.app.tokens.model.DeviceResponse
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
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


/**
 * Tests d'intégration Utilisant Connect.
 *  - Connect est mocké avec Wiremock
 *  - MongoDB et Redis tournent dans des conteneur Docker
 *  - la validation de l'access token est désactivée
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ConnectIntegrationTest.Initializer::class])
@ActiveProfiles("test", "logtxt")
@IntegrationTest
@DockerTest
class ConnectIntegrationTest(@LocalServerPort private val localServerPort: Int,
                             @Autowired private val mongoOperations: ReactiveMongoOperations,
                             @Autowired private val cache: AccountInfoCacheRepository): UsingMongoDBAndRedis() {

    companion object {
        private lateinit var connectMock: WireMockServer

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            connectMock = WireMockServer(WireMockConfiguration.options().dynamicPort())

            // AccountInfo stubs
            connectMock.stubFor(get("/api/accounts/35adcf57-2cf7-4945-a980-e9753eb146f7")
                    .willReturn(aResponse()
                            .withStatus(418)
                    )
            )

            connectMock.stubFor(get("/api/accounts/0bc59432-048c-400e-81f0-4aa6812186ce")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"iuc\":\"0bc59432-048c-400e-81f0-4aa6812186ce\",\"email\":\"awsome@user.com\",\"firstName\":\"Awsome\",\"lastName\":\"User\"," + "\"mobileNumber\":null}")
                    )
            )

            // CreateAccount stubs
            connectMock.stubFor(post("/api/accounts?send_notif_email=true&callback_mobile=http%3A%2F%2Fdo-not-forget-to-give-it-to-us&callback=http%3A%2F%2Fdo-not-forget-to-give-it-to-us&is_migrated=false")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                    )
            )

            // Logout stubs
            connectMock.stubFor(post("/api/accounts/logout")
                    .withRequestBody(equalTo("{\"refreshToken\":\"123456789\"}"))
                    .willReturn(aResponse()
                            .withStatus(204)
                    )
            )

            connectMock.stubFor(post("/api/accounts/logout")
                    .withRequestBody(equalTo("{\"refreshToken\":\"987654321\"}"))
                    .willReturn(aResponse()
                            .withStatus(502)
                    )
            )

            // RegisterToken stubs
            connectMock.stubFor(get("/SvcECZ/oauth2/tokeninfo?access_token=8c9541d2-8846-4fe6-aed3-111059ac70d8")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"scope\":[\"openid\",\"profile\"],\"grant_type\":\"authorization_code\",\"realm\":\"/connect\",\"openid\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":53,\"access_token\":\"047419a1-ca25-4f8c-8119-c3453522810f\",\"profile\":\"123\"}")
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

    private val fakeTokenGenerator = FakeTokenGenerator()
    private lateinit var baseUrl: String

    @BeforeEach
    fun beforeEach() {
        baseUrl = "http://localhost:$localServerPort"
    }

    @AfterEach
    fun afterEach() {
        mongoOperations.dropCollection(Account::class.java).block()
        cache.deleteAll()
        connectMock.resetRequests()
    }

    /*************************************
     ***                               ***
     *** AccountInfo Integration Tests ***
     ***                               ***
     *************************************/

    @Test
    fun `getAccountInfo should return PartnerError when retrieving AccountInfo via Connect failed`() {
        // Given
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        // And an account in database
        mongoOperations.save(Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When
        val response = restTemplate.exchange("$baseUrl/connect/accounts?deviceId=01", HttpMethod.GET, HttpEntity(null, headers), String::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.I_AM_A_TEAPOT)
        connectMock.verify(1, getRequestedFor(urlEqualTo("/api/accounts/35adcf57-2cf7-4945-a980-e9753eb146f7")))
    }

    @Test
    fun `getAccountInfo should return AccountInfo when retrieving AccountInfo via Connect succeeded`() {
        // Given
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("0bc59432-048c-400e-81f0-4aa6812186ce"))

        // And an account in database
        val lastSeen = LocalDateTime.of(2019, 4, 3, 14, 2, 9)
        mongoOperations.save(Account(iuc = "0bc59432-048c-400e-81f0-4aa6812186ce", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = lastSeen)
        ))).block()

        // When
        val response = restTemplate
                .exchange("$baseUrl/connect/accounts?deviceId=01", HttpMethod.GET, HttpEntity(null, headers), ConnectAccountInfoResponse::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(ConnectAccountInfoResponse(
                id = "0bc59432-048c-400e-81f0-4aa6812186ce", email = "awsome@user.com", firstname = "Awsome", lastname = "User",
                device = DeviceResponse(deviceId = "01", active = true, lastSeen = lastSeen)
        ))
        connectMock.verify(1, getRequestedFor(urlEqualTo("/api/accounts/0bc59432-048c-400e-81f0-4aa6812186ce")))
    }

    @Test
    fun `getAccountInfo should return AccountInfo when retrieving AccountInfo via cache`() {
        // Given
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("cbe6dd36-f547-4fb1-b271-722d37404b7d"))

        // And an account in database
        val lastSeen = LocalDateTime.of(2019, 2, 2, 10, 45, 10)
        mongoOperations.save(Account(iuc = "cbe6dd36-f547-4fb1-b271-722d37404b7d", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = lastSeen)
        ))).block()

        //And an Account Info in cache
        cache.save(AccountInfo(iuc = "cbe6dd36-f547-4fb1-b271-722d37404b7d", firstName = "John", lastName = "Snow", email = "j.snow@nightwatch.org"))

        // When
        val response = restTemplate
                .exchange("$baseUrl/connect/accounts?deviceId=01", HttpMethod.GET, HttpEntity(null, headers), ConnectAccountInfoResponse::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualToComparingFieldByField(ConnectAccountInfoResponse(
                id = "cbe6dd36-f547-4fb1-b271-722d37404b7d", email = "j.snow@nightwatch.org", firstname = "John", lastname = "Snow",
                device = DeviceResponse(deviceId = "01", active = true, lastSeen = lastSeen)
        ))
        connectMock.verify(0, getRequestedFor(urlEqualTo("/api/accounts/cbe6dd36-f547-4fb1-b271-722d37404b7d")))
    }

    /****************************************
     ***                                  ***
     *** Create Account Integration Tests ***
     ***                                  ***
     ****************************************/

    @Test
    fun `it should create the account and return 200`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // When I create an account
        val request = "{\"email\":\"awsome@user.com\",\"firstname\":\"Awsome\",\"lastname\":\"User\",\"birthDate\":\"2004-04-18T00:00:00+02:00\",\"password\":\"p@$\$word1\",\"language\":\"fr\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.exchange(URI("$baseUrl/connect/accounts"), HttpMethod.PUT, entity, Void::class.java)

        // Then http status code should be equal to 200
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        connectMock.verify(1, postRequestedFor(urlEqualTo("/api/accounts?send_notif_email=true&callback_mobile=http%3A%2F%2Fdo-not-forget-to-give-it-to-us&callback=http%3A%2F%2Fdo-not-forget-to-give-it-to-us&is_migrated=false")))
    }

    /********************************
     ***                          ***
     *** Logout Integration Tests ***
     ***                          ***
     ********************************/

    @Test
    fun `should logout succesfully`() {
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf<MediaType>(MediaType.APPLICATION_JSON)
        headers.add("X-CorrelationId", "test-correlation-id")
        headers.add("X-Realtime-id", "test-realtime-id")
        headers.add("X-UserId", "test-user-id")

        // Given an account with 2 device. Only device 02 is active
        val account = Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = false, lastSeen = LocalDateTime.now().minus(1, ChronoUnit.WEEKS)),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))
        mongoOperations.save(account).block()

        // When I logout
        val validIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7")
        val request = "{\"idToken\": \"$validIdToken\", \"refreshToken\": \"123456789\", \"deviceId\": \"02\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/connect/logout"), entity, Void::class.java)

        // Then status code is OK
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // The refresh token has been invalidated
        connectMock.verify(1, postRequestedFor(urlEqualTo("/api/accounts/logout")).withRequestBody(equalTo("{\"refreshToken\":\"123456789\"}")))

        // And device 02 is deactivated for account 35adcf57-2cf7-4945-a980-e9753eb146f7
        val acc2 = mongoOperations.findOne(Query.query(Criteria.where("_id").`is`("35adcf57-2cf7-4945-a980-e9753eb146f7")), Account::class.java).block()
        assertThat(acc2).isNotNull
        assertThat(acc2!!.devices.size).isEqualTo(2)
        assertThat(acc2.devices[1].deviceId).isEqualTo("02")
        assertThat(acc2.devices[1].active).isFalse()
    }

    @Test
    fun `should fail to logout and throw an exception when the refresh token is invalid`() {
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf<MediaType>(MediaType.APPLICATION_JSON)
        headers.add("X-CorrelationId", "test-correlation-id")
        headers.add("X-Realtime-id", "test-realtime-id")
        headers.add("X-UserId", "test-user-id")

        // Given an account with 2 device. Only device 02 is active
        val account = Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", active = false, lastSeen = LocalDateTime.now().minus(1, ChronoUnit.WEEKS)),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))
        mongoOperations.save(account).block()

        // When I logout
        val validIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7")
        val request = "{\"idToken\": \"$validIdToken\", \"refreshToken\": \"987654321\", \"deviceId\": \"02\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/connect/logout"), entity, ErrorResponse::class.java)

        // Then status code is BAD_REQUEST
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        // connect has been called to invalidate the refresh token
        connectMock.verify(1, postRequestedFor(urlEqualTo("/api/accounts/logout")).withRequestBody(equalTo("{\"refreshToken\":\"987654321\"}")))

        // And Error is send to the client
        assertThat(response.body.code).isEqualTo("CONNECT_INVALID_REFRESH_TOKEN")
        assertThat(response.body.message).isEqualTo("Invalid or expired refresh token!!!")
    }

    /****************************************
     ***                                  ***
     *** RegisterTokens Integration Tests ***
     ***                                  ***
     ****************************************/

    @Test
    fun `should register tokens and return http code 200 without validating the access token`() {
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
        connectMock.verify(0, getRequestedFor(urlEqualTo("/SvcECZ/oauth2/tokeninfo?access_token=8c9541d2-8846-4fe6-aed3-111059ac70d8")))

        // And I have an account created in db with uic = 35adcf57-2cf7-4945-a980-e9753eb146f7
        val acc = mongoOperations.findOne(Query.query(Criteria.where("_id").`is`("35adcf57-2cf7-4945-a980-e9753eb146f7")), Account::class.java).block()
        assertThat(acc).isNotNull()
        assertThat(acc!!.devices.size).isEqualTo(1)
        assertThat(acc.devices[0].deviceId).isEqualTo("5c6124afe4b049253e8bd468")
        assertThat(acc.devices[0].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc.devices[0].active).isTrue()
    }

    @Test
    fun `should return an error 403 when IdToken is not valid`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)

        // And a Request with an invalid idToken
        val request = "{\"idToken\": \"gotohellspring\", \"accessToken\": \"8c9541d2-8846-4fe6-aed3-111059ac70d8\", \"deviceId\": \"5c6124afe4b049253e8bd468\"}"

        val entity = HttpEntity(request, headers)

        // When I submit a register token request
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, Void::class.java)

        // I have a Http Code 403
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        // And there is no account in the db
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val count = mongoOperations.findAll(Account::class.java).collectList().block().size
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `should return an error 400 when IdToken is missing`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // And a Request with missing idToken
        val request = "{\"accessToken\": \"8c9541d2-8846-4fe6-aed3-111059ac70d8\", \"deviceId\": \"5c6124afe4b049253e8bd468\"}"

        val entity = HttpEntity(request, headers)

        // When I submit a register token reequest
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, String::class.java)
        val body = Configuration.defaultConfiguration().jsonProvider().parse(response.body)

        // I have a Http Code 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(JsonPath.read(body, "$.code") as String).isEqualTo("CONNECT_VALIDATION_ERROR")
        assertThat(JsonPath.read(body, "$.message") as String).contains("value failed for JSON property idToken due to missing")

        // And there is no account in the db
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val count = mongoOperations.findAll(Account::class.java).collectList().block().size
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `should return an error 400 when access token is missing`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // And a Request with missing idToken
        val request = "{" +
                "\"idToken\": \"eyAidHlwIjogIkpXVCIsICJhbGciOiAiUlMyNTYiLCAia2lkIjogIlhHZWVOZXZWUGxOWGJDT1ZoSjR3OTJzaThHVT0iIH0.eyAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF6cCI6ICJVTklfMDEwMDVfREVWIiwgInN1YiI6ICIzNWFkY2Y1Ny0yY2Y3LTQ5NDUtYTk4MC1lOTc1M2ViMTQ2ZjciLCAiYXRfaGFzaCI6ICJBMURkamY2TXRPbzBoQjNSY3plM0FnIiwgImlzcyI6ICJodHRwOi8vci5jb25uZWN0LnNuY2YuY29tOjgwL1N2Y0VDWi9vYXV0aDIvc25jZmNvbm5lY3QiLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIzYmY3ZmQ5My02NTFjLTQ3NTMtYTdmOS0wOTdjNWE4MzA5NjQiLCAiaWF0IjogMTU1MTY5Mjc3NywgImF1dGhfdGltZSI6IDE1NTE2OTI3MDksICJleHAiOiAxNTUxNjkzOTc3LCAicmVtZW1iZXJfbWUiOiAidHJ1ZSIsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAidXBkYXRlZF9hdCI6ICIxNTUxNjg5MTA5IiwgInJlYWxtIjogIi9zbmNmY29ubmVjdCIsICJhdWQiOiBbICJVTklfMDEwMDVfREVWIiBdLCAiY19oYXNoIjogIkpxZU5kZ2EwUWNmWm9FTy1YZ2JIU2ciIH0.ZtKU7oKG0gz-pifLXmuZIFq16mwSywT3AjvJEDU_DIRp1DHVul0u3zus1_Jlwqyl5H_gj_TmhxQ6rjy6fm0-Ky-QYazpoXvfvO_7AQNvt-\", " +
                "\"deviceId\": \"5c6124afe4b049253e8bd468\"" +
                "}"

        val entity = HttpEntity(request, headers)

        // When I submit a register token reequest
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, String::class.java)
        val body = Configuration.defaultConfiguration().jsonProvider().parse(response.body)

        // I have a Http Code 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(JsonPath.read(body, "$.code") as String).isEqualTo("CONNECT_VALIDATION_ERROR")
        assertThat(JsonPath.read(body, "$.message") as String).contains("value failed for JSON property accessToken due to missing")

        // And there is no account in the db
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val count = mongoOperations.findAll(Account::class.java).collectList().block().size
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `should return an error 400 when deviceId is missing`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        // And a Request with missing idToken
        val request = "{" +
                "\"idToken\": \"eyAidHlwIjogIkpXVCIsICJhbGciOiAiUlMyNTYiLCAia2lkIjogIlhHZWVOZXZWUGxOWGJDT1ZoSjR3OTJzaThHVT0iIH0.eyAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF6cCI6ICJVTklfMDEwMDVfREVWIiwgInN1YiI6ICIzNWFkY2Y1Ny0yY2Y3LTQ5NDUtYTk4MC1lOTc1M2ViMTQ2ZjciLCAiYXRfaGFzaCI6ICJBMURkamY2TXRPbzBoQjNSY3plM0FnIiwgImlzcyI6ICJodHRwOi8vci5jb25uZWN0LnNuY2YuY29tOjgwL1N2Y0VDWi9vYXV0aDIvc25jZmNvbm5lY3QiLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIzYmY3ZmQ5My02NTFjLTQ3NTMtYTdmOS0wOTdjNWE4MzA5NjQiLCAiaWF0IjogMTU1MTY5Mjc3NywgImF1dGhfdGltZSI6IDE1NTE2OTI3MDksICJleHAiOiAxNTUxNjkzOTc3LCAicmVtZW1iZXJfbWUiOiAidHJ1ZSIsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAidXBkYXRlZF9hdCI6ICIxNTUxNjg5MTA5IiwgInJlYWxtIjogIi9zbmNmY29ubmVjdCIsICJhdWQiOiBbICJVTklfMDEwMDVfREVWIiBdLCAiY19oYXNoIjogIkpxZU5kZ2EwUWNmWm9FTy1YZ2JIU2ciIH0.ZtKU7oKG0gz-pifLXmuZIFq16mwSywT3AjvJEDU_DIRp1DHVul0u3zus1_Jlwqyl5H_gj_TmhxQ6rjy6fm0-Ky-QYazpoXvfvO_7AQNvt-\", " +
                "\"accessToken\": \"8c9541d2-8846-4fe6-aed3-111059ac70d8\"" +
                "}"

        val entity = HttpEntity(request, headers)

        // When I submit a register token reequest
        val response = restTemplate.postForEntity(URI("$baseUrl/token/register"), entity, String::class.java)
        val body = Configuration.defaultConfiguration().jsonProvider().parse(response.body)

        // I have a Http Code 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(JsonPath.read(body, "$.code") as String).isEqualTo("CONNECT_VALIDATION_ERROR")
        assertThat(JsonPath.read(body, "$.message") as String).contains("value failed for JSON property deviceId due to missing")

        // And there is no account in the db
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val count = mongoOperations.findAll(Account::class.java).collectList().block().size
        assertThat(count).isEqualTo(0)
    }



    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                    "wcomp.connect.base-url=http://localhost:${connectMock.port()}",
                    "features.access-token-validation.enabled: false"
            ).applyTo(applicationContext.environment)
        }
    }
}