package com.wcomp.app.tokens.connect.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Error(
    @JsonProperty("error")
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String
)
