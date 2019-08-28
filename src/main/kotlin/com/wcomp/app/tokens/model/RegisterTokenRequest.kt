package com.wcomp.app.tokens.model

import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import javax.validation.constraints.NotNull

data class RegisterTokenRequest(
    @get:NotNull(message = "{NotNull.idToken}")
    val idToken: String,
    @get:NotNull(message = "{NotNull.accessToken}")
    val accessToken: String,
    @get:NotNull(message = "{NotNull.deviceId}")
    val deviceId: String
    ) {

    override fun toString() = "RegisterTokenRequest(idToken='${idToken.obfuscateBegin(20)}', accessToken='${accessToken.obfuscateEnd(5)}', deviceId='$deviceId')"
}
