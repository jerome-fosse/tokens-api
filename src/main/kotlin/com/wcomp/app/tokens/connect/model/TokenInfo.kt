package com.wcomp.app.tokens.connect.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenInfo (
    val scope: List<String>,
    @JsonProperty("grant_type")
    val grantType: String,
    val realm: String,
    val openid: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("access_token")
    val accessToken: String,
    val profile: String
)
