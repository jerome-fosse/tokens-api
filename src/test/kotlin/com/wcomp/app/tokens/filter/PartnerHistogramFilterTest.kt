package com.wcomp.app.tokens.filter

import com.codahale.metrics.Histogram
import com.codahale.metrics.Reservoir
import com.codahale.metrics.Snapshot
import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI

@UnitTest
class PartnerHistogramFilterTest {

    private lateinit var toTest: PartnerHistogramFilter
    private lateinit var reservoir: SpyReservoir

    @BeforeEach
    fun beforeEach() {
        reservoir = SpyReservoir()
        toTest = PartnerHistogramFilter("partner", Histogram(reservoir))
    }

    @Test
    fun `should update histogram with value 1 when request execution succeeded`() {
        // Given
        val successExecution = ExchangeFunction { Mono.just(ClientResponse.create(HttpStatus.OK).build()) }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        toTest.filter(clientRequest, successExecution).block()

        // Then
        assertThat(reservoir.updated).isEqualTo(1)
    }

    @Test
    fun `should update histogram with value 0 when request execution failed`() {
        // Given
        val failExecution = ExchangeFunction { Mono.error<ClientResponse>(IllegalArgumentException("An error occured !")) }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        val err = assertThrows(IllegalArgumentException::class.java) { toTest.filter(clientRequest, failExecution).block() }

        // Then
        assertThat(err).isNotNull()
        assertThat(reservoir.updated).isEqualTo(0)
    }

    @Test
    fun `should update histogram with value 0 when response status is Bad gateway`() {
        // Given
        val failExecution = ExchangeFunction { Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()) }
        val clientRequest = ClientRequest.create(HttpMethod.GET, URI("http://url")).build()

        // When
        toTest.filter(clientRequest, failExecution).block()

        // Then
        assertThat(reservoir.updated).isEqualTo(0)
    }

    private inner class SpyReservoir : Reservoir {
        var updated: Long = 0

        override fun update(value: Long) {
            updated += value
        }
        override fun size(): Int = 1028
        override fun getSnapshot(): Snapshot {
            TODO("not implemented")
        }
    }
}

