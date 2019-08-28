package com.wcomp.app.tokens.connect.model

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.time.LocalDate
import javax.validation.constraints.NotBlank

@RedisHash(value = "accountInfo")
data class AccountInfo(
        @NotBlank
        @Id
        val iuc: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val civility: String? = null,
        val mobileNumber: String? = null,
        val birthdate: LocalDate? = null,
        val numFid: String? = null,
        val status: String? = null,
        val tosVersion: String? = null
)
