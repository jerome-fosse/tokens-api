package com.wcomp.app.tokens.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.wcomp.app.tokens.utils.extensions.obfuscateEmail
import java.time.LocalDate

data class ConnectAccountInfoResponse(
        val id: String,
        val email: String,
        val firstname: String,
        val lastname: String,
        @get:JsonFormat(shape = JsonFormat.Shape.STRING)
        val birthDate: LocalDate? = null,
        val phoneNumber: String? = null,
        val device: DeviceResponse
) {

    /**
     * To avoid personal infos in logs or elsewhere
     */
    override fun toString() = "ConnectAccountInfoResponse[id='$id', email='${email.obfuscateEmail()}', birthDate='$birthDate', devices='$device']"
}