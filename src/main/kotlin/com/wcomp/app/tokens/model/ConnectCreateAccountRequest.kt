package com.wcomp.app.tokens.model

import com.wcomp.app.tokens.utils.extensions.obfuscateEmail
import java.time.ZonedDateTime
import javax.validation.constraints.*

data class ConnectCreateAccountRequest (
    @get:NotBlank(message = "{NotNull.email}")
    @get:Email(message = "{NotValid.email}") // vérifie aussi la contrainte de la taille
    val email: String,
    @get:NotBlank(message = "{NotNull.password}")
    @get:Size(min = 8, max = 50, message = "{Size.password}")
    val password: String,
    @get:NotNull(message = "{NotNull.firstname}")
    @get:Size(min = 1, max = 50, message = "{Size.firstname}")
    @get:Pattern(regexp = "(\\p{IsAlphabetic}|\\s|-|')*", message = "{Pattern.firstname}")
    val firstname: String,
    @get:NotNull(message = "{NotNull.lastname}")
    @get:Size(min = 1, max = 50, message = "{Size.lastname}")
    @get:Pattern(regexp = "(\\p{IsAlphabetic}|\\s|-|')*", message = "{Pattern.lastname}")
    val lastname: String,
    @get:NotNull(message = "{NotNull.birthdate}")
    @get:Past(message = "{NotPast.birthdate}")
    val birthDate: ZonedDateTime,
    @get:NotNull(message = "{NotNull.language}")
    @get:Pattern(message = "{NotValid.language}", regexp = "(:?fr|de|es|en)")
    val language: String
) {

    override fun toString() = "ConnectCreateAccountRequest(email='${email.obfuscateEmail()}', birthDate=$birthDate, language='$language')"
}
