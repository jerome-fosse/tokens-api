package com.wcomp.app.tokens.connect.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class CreateAccount(
        @field:JsonProperty("email")
        val email: String,
        @field:JsonProperty("password")
        val password: String,
        @field:JsonProperty("firstName")
        val firstname: String,
        @field:JsonProperty("lastName")
        val lastname: String,
        @field:JsonProperty("birthdate")
        @field:JsonFormat(pattern = "yyyy-MM-dd")
        val birthdate: LocalDate
)
