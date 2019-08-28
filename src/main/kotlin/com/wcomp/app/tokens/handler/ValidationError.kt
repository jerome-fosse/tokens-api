package com.wcomp.app.tokens.handler

import com.google.common.collect.ImmutableList

data class ValidationError(val typeName: String, val errors: ImmutableList<String>) {

    override fun toString(): String {
        return "Validations error(s) occured while validating an object of type $typeName : $errors"
    }
}