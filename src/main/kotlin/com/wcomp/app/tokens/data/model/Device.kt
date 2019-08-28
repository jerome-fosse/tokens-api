package com.wcomp.app.tokens.data.model

import org.springframework.data.mongodb.core.index.Indexed
import java.io.Serializable
import java.time.LocalDateTime

data class Device(
        @Indexed
        val deviceId: String,
        val lastSeen: LocalDateTime,
        val active: Boolean = false,
        val maasToken: String? = null) : Serializable
