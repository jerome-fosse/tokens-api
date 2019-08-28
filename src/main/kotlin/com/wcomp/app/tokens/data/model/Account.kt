package com.wcomp.app.tokens.data.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "accounts")
data class Account(
        @Id
        val iuc: String,
        val devices: List<Device>) {


    fun upsertDevice(deviceId: String): Account {
        val clones = when (devices.indexOfFirst { it.deviceId == deviceId }) {
            -1 -> devices.plusElement(Device(deviceId = deviceId, lastSeen = LocalDateTime.now(), active = true))
            else -> devices.map { if (it.deviceId == deviceId) it.copy(active = true, lastSeen = LocalDateTime.now()) else it }
        }

        return this.copy(devices = clones)
    }

    fun updateDeviceWithMaasToken(deviceId: String, maasToken: String): Account {
        return this.copy(devices = devices.map { if (it.deviceId == deviceId) it.copy(maasToken = maasToken) else it })
    }
}
