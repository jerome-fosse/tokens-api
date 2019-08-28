package com.wcomp.app.tokens.filter

import brave.SpanCustomizer
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.mdc.MDCTags.*
import com.wcomp.app.tokens.connect.PartnerException
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI

@UnitTest
class SpanCustomizationExchangeFilterTest {

    private lateinit var spanCustomizer: SpySpanCustomizer
    private lateinit var toTest: SpanCustomizationExchangeFilter

    @BeforeEach
    fun beforeEach() {
        spanCustomizer = SpySpanCustomizer()
        toTest = SpanCustomizationExchangeFilter("partner", spanCustomizer)
    }

    @Test
    fun filter_shouldTagSpanWithHttpStatusCodeAndErrorInfo_whenPartnerExceptionThrown() {
        // Given
        val partnerFail = ExchangeFunction { Mono.error(PartnerException("partner", SERVICE_UNAVAILABLE, "Yes, as surprising as it can be partner do fail!"))
        }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        val partnerException = assertThrows(PartnerException::class.java) { toTest.filter(clientRequest, partnerFail).block() }

        // Then
        assertThat(partnerException).isNotNull()
        assertThat(spanCustomizer.tags[PARTNER.mdcKey]).isEqualTo("partner")
        assertThat(spanCustomizer.tags[HTTP_RESPONSE_CODE.mdcKey]).isEqualTo("503")
        assertThat(spanCustomizer.tags[ERROR_CODE.mdcKey]).isEqualTo("503")
        assertThat(spanCustomizer.tags[ERROR_MESSAGE.mdcKey]).isEqualTo("Partner error : partner=partner, statusCode=503 SERVICE_UNAVAILABLE : Yes, as surprising as it can be partner do fail!")
    }

    @Test
    fun filter_shouldTagSpanErrorInfo_whenRandomExceptionThrown() {
        // Given
        val failExchange = ExchangeFunction { Mono.error(NullPointerException("Yet another random exception")) }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        val partnerException = assertThrows(NullPointerException::class.java) { toTest.filter(clientRequest, failExchange).block() }

        // Then
        assertThat(partnerException).isNotNull()
        assertThat(spanCustomizer.tags[PARTNER.mdcKey]).isEqualTo("partner")
        assertThat(spanCustomizer.tags[ERROR_CODE.mdcKey]).isEqualTo("java.lang.NullPointerException")
        assertThat(spanCustomizer.tags[ERROR_MESSAGE.mdcKey]).isEqualTo("Yet another random exception")
    }

    @Test
    fun filter_shouldTagSpanWithHttpStatusCode_whenNoError() {
        // Given
        val successExchange = ExchangeFunction { Mono.just(ClientResponse.create(HttpStatus.CREATED).build()) }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        toTest.filter(clientRequest, successExchange).block()

        // Then
        assertThat(spanCustomizer.tags[PARTNER.mdcKey]).isEqualTo("partner")
        assertThat(spanCustomizer.tags[HTTP_RESPONSE_CODE.mdcKey]).isEqualTo("201")
    }

    private inner class SpySpanCustomizer : SpanCustomizer {

        val tags = mutableMapOf<String?, String?>()

        override fun tag(key: String?, value: String?): SpanCustomizer {
            tags[key] = value
            return this
        }

        override fun annotate(value: String?)= this
        override fun name(name: String?)= this
    }
}
