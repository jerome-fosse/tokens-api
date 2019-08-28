package com.wcomp.app.tokens.connect

import java.net.URLEncoder


data class ConnectClientConfiguration(
        val user: String,
        val password: String,
        val accountApiUrl: String,
        val openAMApiUrl: String,
        val accountCreationCallbackUrl: String,
        val accountCreationCallbackMobileUrl: String,
        val sendAccountCreationNotifEmail: Boolean? = false
) {

    val encodedAccountCreationCallbackUrl: String
    val encodedAccountCreationCallbackMobileUrl: String

    init {
        encodedAccountCreationCallbackUrl = URLEncoder.encode(accountCreationCallbackUrl, "UTF-8")
        encodedAccountCreationCallbackMobileUrl = URLEncoder.encode(accountCreationCallbackMobileUrl, "UTF-8")
    }
}
