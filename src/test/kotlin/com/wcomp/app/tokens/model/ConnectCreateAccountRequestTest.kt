package com.wcomp.app.tokens.model

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.validation.Validation
import javax.validation.Validator

@UnitTest
class ConnectCreateAccountRequestTest {

    companion object {
        lateinit var validator: Validator

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val factory = Validation.buildDefaultValidatorFactory()
            validator = factory.validator
        }
    }

    @Test
    fun `email should be valid`() {
        // Given
        val request = ConnectCreateAccountRequest("someone.domain.com",
                "pa$\$word", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "de")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The email is invalid")
    }

    @Test
    fun `email should have less than 320 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@very-loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong-domain.com",
                "pa$\$word", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "en")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The email is invalid")
    }

    @Test
    fun `password should not be shorter than 8 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "1234567", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The password should have a size between 8 and 50 characters")
    }

    @Test
    fun `password should not be longer than 50 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "123456789012345678901234567890123456789012345678901", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "en")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The password should have a size between 8 and 50 characters")
    }

    @Test
    fun `password should be longer than 7 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "12345678", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `password should be shorter than 51 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "12345678901234567890123456789012345678901234567890", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "de")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `firstname should not be empty`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The firstname should have a size between 1 and 50 characters")
    }

    @Test
    fun `firstname should not be more than 50 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopa", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The firstname should have a size between 1 and 50 characters")
    }

    @Test
    fun `firstname should be less than 51 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiop", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `firstname could have small letters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "abcdefghijklmnopqrstuvwxyz", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `firstname could have capital letters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `firstname could have some other characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "éèêôöïä- ", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `firstname could not have numbers`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "1234567890", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The firstname contains forbidden characters")
    }

    @Test
    fun `lastname could not be empty`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The lastname should have a size between 1 and 50 characters")
    }

    @Test
    fun `lastname should not be more than 50 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopa", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The lastname should have a size between 1 and 50 characters")
    }

    @Test
    fun `lastname should be less than 51 characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiop", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `lastname could have small letters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "abcdefghijklmnopqrstuvwxyz", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `lastname could have capital letters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `lastname could have some other characters`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "éèêôöïä- ", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `lastname could not have numbers`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "1234567890", ZonedDateTime.now().minus(1, ChronoUnit.YEARS), "fr")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The lastname contains forbidden characters")
    }

    @Test
    fun `birthdate could not be in the future`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "lastname", ZonedDateTime.now().plus(1, ChronoUnit.MINUTES), "es")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The birthdate is not in the past")
    }

    @Test
    fun `birthdate should be in the past`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.MINUTES), "de")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(0)
    }

    @Test
    fun `language should be in pattern`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "firstname", "lastname", ZonedDateTime.now().minus(1, ChronoUnit.MINUTES), "it")

        // When
        val violations = validator.validate(request)

        // Then
        assertThat(violations).isNotNull
        assertThat(violations.size).isEqualTo(1)
        assertThat(violations.iterator().next().message).isEqualTo("The language is not valid. Accepted languages are : fr,de,es,en")
    }

    @Test
    fun `toString does not contains password firstname or lastname`() {
        // Given
        val request = ConnectCreateAccountRequest("someone@domain.com",
                "pa$\$word", "fir\$tname", "la\$tname", ZonedDateTime.now().minus(1, ChronoUnit.MINUTES), "en")

        // When
        val toString = request.toString()

        // Then
        assertThat(toString).doesNotContain("pa$\$word")
        assertThat(toString).doesNotContain("fir\$tname")
        assertThat(toString).doesNotContain("la\$tname")
    }
}
