package com.wcomp.app.tokens.utils

import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.utils.extensions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@UnitTest
class StringExtensionsTest {

    @Test
    fun `should anonymize a string and keep the x first characters if x is greater than half the size of the string`() {
        // Given a String of 20 characters
        val s = "azertyuiopqsdfghjklm"

        // When anonymizing the string and keep the 10 first characters
        val a = s.obfuscateEnd(10)

        // Then
        assertThat(a).isEqualTo("azertyuiop********************")
    }

    @Test
    fun `should anonymize a string and keep the first half of it when the string size is lesser than 2 times the offset`() {
        // Given a String of 20 characters
        val s = "azertyuiopqsdfghjklm"

        // When anonymizing the string and keep the 15 first characters
        val a = s.obfuscateEnd(15)

        // Then
        assertThat(a).isEqualTo("azertyuiop********************")
    }

    @Test
    fun `should keep the first 5 characters of a String`() {
        // Given a string of 10 characters
        val value = "1234567890"

        // When I keep the 5 first characters
        val s = value.first(5)

        // then
        assertThat(s).isEqualTo("12345")
    }

    @Test
    fun `should keep the last 5 characters of a String`() {
        // Given a string of 10 characters
        val value = "1234567890"

        // When I keep the 5 last characters
        val s = value.last(5)

        // then
        assertThat(s).isEqualTo("67890")
    }

    @Test
    fun `should anonymize a string and keep last 3 characters`() {
        // Given a string of 10 characters
        val value = "1234567890"

        // When I anonymize the string and keep the last 3 characters
        val s = value.obfuscateBegin(3)

        // Then
        assertThat(s).isEqualTo("********************890")
    }

    @Test
    fun `should anonymize and email address`() {
        // Given an email address
        val email = "jfosse.prestataire@wcomp.com"

        // When anonymized
        val ano = email.obfuscateEmail()

        // Then should be anonymized
        assertThat(ano).isEqualTo("jfo*******@wcomp.com")
    }
}