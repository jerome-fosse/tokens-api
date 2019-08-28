package com.wcomp.app.tokens.model

import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import javax.validation.constraints.NotNull

data class ConnectLogoutRequest (
    @get:NotNull(message = "{NotNull.idToken}")
    val idToken: String,
    @get:NotNull(message = "{NotNull.refreshToken}")
    val refreshToken: String,
    @get:NotNull(message = "{NotNull.deviceId}")
    val deviceId: String
) {
    override fun toString() = "ConnectLogoutRequest(idToken='${idToken.obfuscateBegin(20)}', refreshToken='${refreshToken.obfuscateEnd(5)}', deviceId='$deviceId')"
}
