package com.wcomp.app.tokens.model

import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import javax.validation.constraints.NotNull

data class SaveMaasTokenRequest(
        @get:NotNull(message = "{NotNull.deviceId}")
        val deviceId: String,
        @get:NotNull(message = "{NotNull.deviceTokenMaas}")
        val deviceTokenMaas: String
) {

    override fun toString() = "SaveMaasTokenRequest[deviceId='$deviceId', deviceTokenMaas='${deviceTokenMaas.obfuscateEnd(10)}']"
}
