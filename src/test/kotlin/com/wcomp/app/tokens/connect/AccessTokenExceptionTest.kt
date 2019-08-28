package com.wcomp.app.tokens.connect

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@UnitTest
class AccessTokenExceptionTest {

    @Test
    fun `exception message should be correctly formated`() {
        // Given
        val ex = AccessTokenException(HttpStatus.BAD_REQUEST, "my error")

        // When
        val msg = ex.message

        // Then
        assertThat(msg).isEqualTo("Access token error : partner=Connect, statusCode=400 BAD_REQUEST : my error")
    }
}