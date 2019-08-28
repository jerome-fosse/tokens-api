package com.wcomp.app.tokens.connect

import com.wcomp.app.tokens.connect.model.Error
import org.springframework.http.HttpStatus

open class PartnerException(
        val errorName: String = "Partner error",
        val partnerName: String,
        val httpStatusCode: HttpStatus,
        val error: Error
) : RuntimeException("$errorName : partner=$partnerName, statusCode=$httpStatusCode : ${error.errorDescription}") {

    constructor(partnerName: String, httpStatusCode: HttpStatus, error: String) :
            this(partnerName = partnerName, httpStatusCode = httpStatusCode, error = Error(httpStatusCode.toString(), error))

    constructor(errorName: String, partnerName: String, httpStatusCode: HttpStatus, error: String) :
            this(errorName = errorName, partnerName = partnerName, httpStatusCode = httpStatusCode, error = Error(httpStatusCode.toString(), error))
}

