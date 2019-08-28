package com.wcomp.app.tokens.exeption

class AccountNotFoundException(private val iuc: String) : RuntimeException() {

    override val message: String?
        get() = "Account with id $iuc does not exists."
}
