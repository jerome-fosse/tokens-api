package com.wcomp.app.tokens.connect

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@UnitTest
class InvalidRefreshTokenExceptionTest {

    @Test
    fun `exception message should be correctly formated`() {
        // Given
        val ex = InvalidRefreshTokenException(HttpStatus.BAD_REQUEST, "my error")

        // When
        val msg = ex.message

        // Then
        assertThat(msg).isEqualTo("Refresh token error : partner=Connect, statusCode=400 BAD_REQUEST : my error")
    }
}