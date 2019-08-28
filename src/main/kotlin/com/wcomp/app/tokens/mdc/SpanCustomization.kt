package com.wcomp.app.tokens.mdc

import brave.SpanCustomizer
import com.wcomp.app.tokens.mdc.MDCTags.*
import com.wcomp.app.tokens.connect.PartnerException
import org.springframework.http.HttpStatus

object SpanCustomization {

    fun tagError(span: SpanCustomizer?, t: Throwable?) {
        if (span == null || t == null) {
            return
        }

        if (t is PartnerException) {
            val errorCode = t.httpStatusCode.value().toString()
            span.tag(PARTNER.mdcKey, t.partnerName)
            span.tag(HTTP_RESPONSE_CODE.mdcKey, errorCode)
            span.tag(ERROR_CODE.mdcKey, errorCode)
            span.tag(ERROR_MESSAGE.mdcKey, t.message)
        } else {
            span.tag(ERROR_CODE.mdcKey, t.javaClass.name)
            span.tag(ERROR_MESSAGE.mdcKey, t.message)
        }
    }

    fun tagHttpStatus(span: SpanCustomizer?, status: HttpStatus?) {
        if (span == null || status == null) {
            return
        }
        span.tag(HTTP_RESPONSE_CODE.mdcKey, status.value().toString())
    }
}
