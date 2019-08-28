package com.wcomp.app.tokens.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import java.time.LocalDateTime

data class AccountResponse(
        val iuc: String,
        val devices: List<DeviceResponse>
) {

    companion object {
        fun of(account: Account): AccountResponse {
            return AccountResponse(
                    iuc = account.iuc,
                    devices = account.devices.map { DeviceResponse.of(it) }
            )
        }
    }
}

data class DeviceResponse(
        val deviceId: String,
        @get:JsonFormat(shape = JsonFormat.Shape.STRING)
        val lastSeen: LocalDateTime,
        val active: Boolean,
        val maasToken: String? = null
) {
    companion object {
        fun of (device: Device): DeviceResponse {
            return DeviceResponse(deviceId = device.deviceId, active = device.active, lastSeen = device.lastSeen, maasToken = device.maasToken)
        }
    }
}
