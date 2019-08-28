package com.wcomp.app.tokens.mdc

import brave.Span
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.mdc.MDCTags.*
import com.wcomp.app.tokens.connect.PartnerException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus

@UnitTest
class SpanCustomizationTest {

    @Test
    fun tagError_shouldTagGivenSpan_whenPartnerException() {
        // Given
        val test = mock(Span::class.java)
        val givenException = PartnerException("testPartner", HttpStatus.NOT_FOUND, "A dull error")

        // When
        SpanCustomization.tagError(test, givenException)

        // Then
        verify(test).tag(HTTP_RESPONSE_CODE.mdcKey, "404")
        verify(test).tag(ERROR_CODE.mdcKey, "404")
        verify(test).tag(ERROR_MESSAGE.mdcKey, "Partner error : partner=testPartner, statusCode=404 NOT_FOUND : A dull error")
    }

    @Test
    fun tagError_shouldTagGivenSpan_whenRandomException() {
        // Given
        val test = mock(Span::class.java)
        val givenException = RuntimeException("A total random exception")

        // When
        SpanCustomization.tagError(test, givenException)

        // Then
        verify(test).tag(ERROR_CODE.mdcKey, "java.lang.RuntimeException")
        verify(test).tag(ERROR_MESSAGE.mdcKey, "A total random exception")
    }

    @Test
    fun tagError_shouldNotFail_whenSpanIsNull() {
        // Given && When
        val thrown = catchThrowable { SpanCustomization.tagError(null, RuntimeException()) }

        // Then should never fail
        assertThat(thrown).isNull()
    }

    @Test
    fun tagError_shouldNotFail_whenExceptionIsNull() {
        // Given && When
        val test = mock(Span::class.java)
        val thrown = catchThrowable { SpanCustomization.tagError(test, null) }

        // Then should never fail
        assertThat(thrown).isNull()
    }

    @Test
    fun tagHttpStatus_shouldNotFail_whenSpanIsNull() {
        // Given && When
        val thrown = catchThrowable { SpanCustomization.tagHttpStatus(null, HttpStatus.CREATED) }

        // Then should never fail
        assertThat(thrown).isNull()
    }

    @Test
    fun tagHttpStatus_shouldNotFail_whenHttpStatusIsNull() {
        // Given && When
        val test = mock(Span::class.java)
        val thrown = catchThrowable { SpanCustomization.tagHttpStatus(test, null) }

        // Then should never fail
        assertThat(thrown).isNull()
    }

    @Test
    fun tagHttpStatus_shouldTagHttpStatus() {
        // Given && When
        val test = mock(Span::class.java)
        SpanCustomization.tagHttpStatus(test, HttpStatus.CREATED)

        // Then
        verify(test).tag(HTTP_RESPONSE_CODE.mdcKey, "201")
    }

}