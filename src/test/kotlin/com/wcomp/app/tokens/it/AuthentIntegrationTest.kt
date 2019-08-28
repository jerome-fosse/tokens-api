package com.wcomp.app.tokens.it

import com.wcomp.app.tokens.DockerTest
import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.IntegrationTest
import com.wcomp.app.tokens.UsingMongoDB
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.model.AccountResponse
import com.wcomp.app.tokens.model.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.time.LocalDateTime

/**
 * Test d'intégration utilisant la configuration par défault
 *  - Connect n'est pas utilisé
 *  - la validation de l'access token est désactivée
 *  - Redis est désactivé
 *  - MongoDB toune dans un conteneur Docker
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "logtxt")
@IntegrationTest
@DockerTest
class AuthentIntegrationTest(@LocalServerPort private val localServerPort: Int, @Autowired private val mongoOperations: ReactiveMongoOperations): UsingMongoDB() {

    private lateinit var baseUrl: String
    private val fakeTokenGenerator = FakeTokenGenerator()

    @BeforeEach
    fun beforeEach() {
        baseUrl = "http://localhost:$localServerPort"
    }

    @AfterEach
    fun afterEach() {
        mongoOperations.dropCollection(Account::class.java).block()
    }


    /*********************************
     *** Accounts Integration Test ***
     *********************************/

    @Test
    fun `should return an account with its devices`() {
        val restTemplate = TestRestTemplate()
        // Given an Account with 2 actives devices and 2 inactives devices
        val a = Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "03", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "04", lastSeen = LocalDateTime.now(), active = true)
        ))
        mongoOperations.save(a).block()

        // When I request the account 123456789 with its active devices
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        val response = restTemplate.exchange("$baseUrl/accounts?deviceId=01", HttpMethod.GET, HttpEntity(null, headers), AccountResponse::class.java)

        // Then HTTP Status is 200
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)

        // And IUC = 35adcf57-2cf7-4945-a980-e9753eb146f7
        val account = response.getBody()
        assertThat(account).isNotNull()
        assertThat(account!!.iuc).isEqualTo("35adcf57-2cf7-4945-a980-e9753eb146f7")

        // And the account has 2 active devices
        assertThat(account.devices.size).isEqualTo(4)
        assertThat(account.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].active).isTrue()
        assertThat(account.devices[1].deviceId).isEqualTo("02")
        assertThat(account.devices[1].active).isFalse()
        assertThat(account.devices[2].deviceId).isEqualTo("03")
        assertThat(account.devices[2].active).isFalse()
        assertThat(account.devices[3].deviceId).isEqualTo("04")
        assertThat(account.devices[3].active).isTrue()
    }

    @Test
    fun `should return 404 when account does not exists`() {
        val restTemplate = TestRestTemplate()
        // Given no Account

        // When I request the account 35adcf57-2cf7-4945-a980-e9753eb146f7 with its active devices
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        val response = restTemplate.exchange("$baseUrl/accounts?deviceId=01", HttpMethod.GET, HttpEntity(null, headers), ErrorResponse::class.java)

        // Then HTTP Status is 404
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        // And the error is correctly formatted
        val error = response.body
        assertThat(error).isNotNull()
        assertThat(error!!.code).isEqualTo("CONNECT_ACCOUNT_NOT_FOUND")
        assertThat(error.message).isEqualTo("Account with id 35adcf57-2cf7-4945-a980-e9753eb146f7 does not exists.")
    }

    @Test
    fun `should return 400 when account exists but it does not have requested device`() {
        val restTemplate = TestRestTemplate()
        // Given an Account with 2 devices
        val a = Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false)
        ))
        mongoOperations.save(a).block()

        // When I request the account 35adcf57-2cf7-4945-a980-e9753eb146f7 with device 03
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        val response = restTemplate.exchange("$baseUrl/accounts?deviceId=03", HttpMethod.GET, HttpEntity(null, headers), ErrorResponse::class.java)

        // Then HTTP Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        // And the error is correctly formatted
        val error = response.body
        assertThat(error).isNotNull()
        assertThat(error!!.code).isEqualTo("CONNECT_BAD_REQUEST")
        assertThat(error.message).isEqualTo("No Account found with id = 35adcf57-2cf7-4945-a980-e9753eb146f7 and deviceId = 03")
    }

    @Test
    fun `should return 400 when deviceId parameter is missing`() {
        val restTemplate = TestRestTemplate()
        // Given an Account with 2 devices
        val a = Account(iuc = "35adcf57-2cf7-4945-a980-e9753eb146f7", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false)
        ))
        mongoOperations.save(a).block()

        // When I request the account 35adcf57-2cf7-4945-a980-e9753eb146f7 with device 03
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", fakeTokenGenerator.generateNotExpiredSignedToken("35adcf57-2cf7-4945-a980-e9753eb146f7"))

        val response = restTemplate.exchange("$baseUrl/accounts", HttpMethod.GET, HttpEntity(null, headers), ErrorResponse::class.java)

        // Then HTTP Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        // And the error is correctly formatted
        val error = response.body
        assertThat(error).isNotNull()
        assertThat(error!!.code).isEqualTo("CONNECT_BAD_REQUEST")
        assertThat(error.message).isEqualTo("Missing deviceId parameter.")
    }

    /*************************************************
     *** AccountWithDevicesActive Integration Test ***
     *************************************************/

    @Test
    fun shouldReturnAnAccountWithItsInActiveDevices() {
        // Given an Account with 2 actives devices and 2 inactives devices
        val a = Account(iuc = "123456789", devices = listOf(
                Device(deviceId = "01", lastSeen = LocalDateTime.now(), active = true),
                Device(deviceId = "02", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "03", lastSeen = LocalDateTime.now(), active = false),
                Device(deviceId = "04", lastSeen = LocalDateTime.now(), active = true)
        ))
        mongoOperations.save(a).block()
        val restTemplate = TestRestTemplate()

        // When I request the account 123456789 with its active devices
        val response = restTemplate.getForEntity("$baseUrl/accounts/123456789?active=false", AccountResponse::class.java)

        // Then HTTP Status is 200
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // And IUC = 123456789
        val account = response.getBody()
        assertThat(account).isNotNull
        assertThat(account!!.iuc).isEqualTo("123456789")

        // And the account has 2 active devices
        assertThat(account.devices.size).isEqualTo(2)
        assertThat(account.devices[0].deviceId).isEqualTo("02")
        assertThat(account.devices[0].active).isFalse()
        assertThat(account.devices[1].deviceId).isEqualTo("03")
        assertThat(account.devices[1].active).isFalse()
    }

    @Test
    fun shouldReturnHttp404_WhenAccountDoesNotExist() {
        // When I request any account with an empty database
        val restTemplate = TestRestTemplate()
        val response = restTemplate.getForEntity("$baseUrl/accounts/123456789", ErrorResponse::class.java)

        // Then HTTP Status is 404
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        // And the error is correctly formatted
        val error = response.getBody()
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo("CONNECT_ACCOUNT_NOT_FOUND")
        assertThat(error?.message).isEqualTo("Account with id 123456789 does not exists.")
    }

    @Test
    fun shouldReturnHttp400_WhenPathVariableFormatIsIncorrect() {
        // When I request any account and active is not a boolean
        val restTemplate = TestRestTemplate()
        val response = restTemplate.getForEntity("$baseUrl/accounts/123456789?active=abcd", ErrorResponse::class.java)

        // Then HTTP Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        // And the error is correctly formatted
        val error = response.body
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo("CONNECT_VALIDATION_ERROR")
    }

    /**************************************
     *** SaveMaasToken Integration Test ***
     **************************************/

    @Test
    fun `should update device with maas token`() {
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
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, Void::class.java)

        // Then Http Status is 204
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // And maas token has been saved
        val account = mongoOperations.findById("35adcf57-2cf7-4945-a980-e9753eb146f7", Account::class.java).block()
        assertThat(account).isNotNull
        assertThat(account!!.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isEqualTo("maas01")
    }

    @Test
    fun `should raise an error 400 when device id is null`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", "eyAidHlwIjogIkpXVCIsICJhbGciOiAiUlMyNTYiLCAia2lkIjogIlhHZWVOZXZWUGxOWGJDT1ZoSjR3OTJzaThHVT0iIH0.eyAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF6cCI6ICJVTklfMDEwMDVfREVWIiwgInN1YiI6ICIzNWFkY2Y1Ny0yY2Y3LTQ5NDUtYTk4MC1lOTc1M2ViMTQ2ZjciLCAiYXRfaGFzaCI6ICJBMURkamY2TXRPbzBoQjNSY3plM0FnIiwgImlzcyI6ICJodHRwOi8vci5jb25uZWN0LnNuY2YuY29tOjgwL1N2Y0VDWi9vYXV0aDIvc25jZmNvbm5lY3QiLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIzYmY3ZmQ5My02NTFjLTQ3NTMtYTdmOS0wOTdjNWE4MzA5NjQiLCAiaWF0IjogMTU1MTY5Mjc3NywgImF1dGhfdGltZSI6IDE1NTE2OTI3MDksICJleHAiOiAxNTUxNjkzOTc3LCAicmVtZW1iZXJfbWUiOiAidHJ1ZSIsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAidXBkYXRlZF9hdCI6ICIxNTUxNjg5MTA5IiwgInJlYWxtIjogIi9zbmNmY29ubmVjdCIsICJhdWQiOiBbICJVTklfMDEwMDVfREVWIiBdLCAiY19oYXNoIjogIkpxZU5kZ2EwUWNmWm9FTy1YZ2JIU2ciIH0.ZtKU7oKG0gz-pifLXmuZIFq16mwSywT3AjvJEDU_DIRp1DHVul0u3zus1_Jlwqyl5H_gj_TmhxQ6rjy6fm0-Ky-QYazpoXvfvO_7AQNvt-")
        headers.set("X-access-token", "8c9541d2-8846-4fe6-aed3-111059ac70d8")

        // When I save the maasToken but the device id is missing
        val request = "{\"deviceTokenMaas\":\"maas01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, ErrorResponse::class.java)

        // Then Http Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        // And the body contains an Error
        assertThat(response.body).isNotNull
        assertThat(response.body!!.code).isEqualTo("CONNECT_VALIDATION_ERROR")
        assertThat(response.body!!.message).contains("value failed for JSON property deviceId due to missing")
    }

    @Test
    fun `should raise an error 400 when maas token is null`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-id-token", "eyAidHlwIjogIkpXVCIsICJhbGciOiAiUlMyNTYiLCAia2lkIjogIlhHZWVOZXZWUGxOWGJDT1ZoSjR3OTJzaThHVT0iIH0.eyAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF6cCI6ICJVTklfMDEwMDVfREVWIiwgInN1YiI6ICIzNWFkY2Y1Ny0yY2Y3LTQ5NDUtYTk4MC1lOTc1M2ViMTQ2ZjciLCAiYXRfaGFzaCI6ICJBMURkamY2TXRPbzBoQjNSY3plM0FnIiwgImlzcyI6ICJodHRwOi8vci5jb25uZWN0LnNuY2YuY29tOjgwL1N2Y0VDWi9vYXV0aDIvc25jZmNvbm5lY3QiLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIzYmY3ZmQ5My02NTFjLTQ3NTMtYTdmOS0wOTdjNWE4MzA5NjQiLCAiaWF0IjogMTU1MTY5Mjc3NywgImF1dGhfdGltZSI6IDE1NTE2OTI3MDksICJleHAiOiAxNTUxNjkzOTc3LCAicmVtZW1iZXJfbWUiOiAidHJ1ZSIsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAidXBkYXRlZF9hdCI6ICIxNTUxNjg5MTA5IiwgInJlYWxtIjogIi9zbmNmY29ubmVjdCIsICJhdWQiOiBbICJVTklfMDEwMDVfREVWIiBdLCAiY19oYXNoIjogIkpxZU5kZ2EwUWNmWm9FTy1YZ2JIU2ciIH0.ZtKU7oKG0gz-pifLXmuZIFq16mwSywT3AjvJEDU_DIRp1DHVul0u3zus1_Jlwqyl5H_gj_TmhxQ6rjy6fm0-Ky-QYazpoXvfvO_7AQNvt-")
        headers.set("X-access-token", "8c9541d2-8846-4fe6-aed3-111059ac70d8")

        // When I save the maasToken but the maas token is missing
        val request = "{\"deviceId\":\"01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, ErrorResponse::class.java)

        // Then Http Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        // And the body contains an Error
        assertThat(response.body).isNotNull
        assertThat(response.body!!.code).isEqualTo("CONNECT_VALIDATION_ERROR")
        assertThat(response.body!!.message).contains("value failed for JSON property deviceTokenMaas due to missing")
    }

    @Test
    fun `should raise an error 400 when idToken is Missing`() {
        // Given le merdier Spring pour créer un RestTemplate
        val restTemplate = TestRestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set("X-access-token", "8c9541d2-8846-4fe6-aed3-111059ac70d8")

        // When I save the maasToken but the id token is missing
        val request = "{\"deviceId\":\"01\", \"deviceTokenMaas\":\"maas01\"}"
        val entity = HttpEntity(request, headers)
        val response = restTemplate.postForEntity(URI("$baseUrl/token/maas"), entity, ErrorResponse::class.java)

        // Then Http Status is 400
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        // And the body contains an Error
        assertThat(response.body).isNotNull
        assertThat(response.body!!.code).isEqualTo("CONNECT_BAD_REQUEST")
        assertThat(response.body!!.message).contains("Missing request header 'X-id-token'")
    }
}