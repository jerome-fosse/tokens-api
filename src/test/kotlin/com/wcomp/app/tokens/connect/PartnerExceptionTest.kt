package com.wcomp.app.tokens.connect

import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.connect.model.Error
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@UnitTest
class PartnerExceptionTest {

    @Test
    fun `should throw a PartnerException with a partner error`() {
        // Given
        val myError = Error(error = "My error", errorDescription = "My description")
        val ex = PartnerException(partnerName = "Connect", httpStatusCode = HttpStatus.BAD_REQUEST, error = myError)

        // When && Then
        assertThat(ex.message).isEqualTo("Partner error : partner=Connect, statusCode=${HttpStatus.BAD_REQUEST} : ${myError.errorDescription}")
    }

    @Test
    fun `should throw a PartnerException with a custom error`() {
        // Given
        val myError = Error(error = "My error", errorDescription = "My description")
        val ex = PartnerException(errorName = "Custom error", partnerName = "Connect", httpStatusCode = HttpStatus.BAD_REQUEST, error = myError)

        // When && Then
        assertThat(ex.message).isEqualTo("Custom error : partner=Connect, statusCode=${HttpStatus.BAD_REQUEST} : ${myError.errorDescription}")
    }
}