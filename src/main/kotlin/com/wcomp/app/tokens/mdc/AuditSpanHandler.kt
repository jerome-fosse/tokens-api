package com.wcomp.app.tokens.mdc

import brave.handler.FinishedSpanHandler
import brave.handler.MutableSpan
import brave.propagation.ExtraFieldPropagation
import brave.propagation.TraceContext
import com.wcomp.app.tokens.mdc.MDCTags.*
import org.apache.commons.lang3.StringUtils.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

/**
 * Permet de LOGGER la durée d'un span terminé.
 * /!\ Nécessite que le span soit "sampled".
 */
class AuditSpanHandler : FinishedSpanHandler() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER")
    }

    override fun handle(context: TraceContext, span: MutableSpan): Boolean {
        WithMDC.create().use { withMDC ->
            val spanDuration = (span.finishTimestamp() - span.startTimestamp()) / 1000
            recreateMDCFromTrace(context, span, spanDuration, withMDC)

            if (finishedWithoutError(span)) {
                LOGGER.info("{} : duration={}ms", getServiceName(span), spanDuration)
            } else {
                LOGGER.error("{} : duration={}ms, error_code={}, error_msg='{}'",
                        getServiceName(span), spanDuration, MDC.get(ERROR_CODE.mdcKey), MDC.get(ERROR_MESSAGE.mdcKey))
            }
        }
        return true // keep this span
    }

    private fun recreateMDCFromTrace(traceContext: TraceContext, span: MutableSpan, spanDuration: Long, withMDC: WithMDC) {
        val serviceName = getServiceName(span)

        withMDC.custom("spanId", traceContext.spanId().toString())
        withMDC.custom("traceId", traceContext.traceId().toString())
        withMDC.custom("parentId", traceContext.parentId().toString())
        withMDC.service(serviceName).elapsedTimeMs(spanDuration.toString())
        getPartner(span).ifPresent { withMDC.partner(it) }
        getHttpStatusCode(span).ifPresent { withMDC.httpResponseCode(it) }
        getHttpMethod(span).ifPresent { withMDC.httpMethod(it) }
        getHttpPath(span).ifPresent { withMDC.custom("http_path", it) }
        getErrorCode(span).ifPresent { withMDC.errorCode(it) }
        getErrorMessage(span).ifPresent { withMDC.errorMessage(it) }

        // Retrieve headers from trace context and put the values in the MDC for local logging purposes
        MDCTags.headersTags.forEach { mdcHeader ->
            withMDC.custom(mdcHeader.mdcKey, ExtraFieldPropagation.get(traceContext, mdcHeader.headerName))
        }
    }

    private fun getServiceName(span: MutableSpan) = when {
        isNotBlank(span.tag("service")) -> span.tag("service")
        isNoneBlank(span.tag("http.method"), span.tag("http.path")) -> "${span.tag("http.method")} ${span.tag("http.path")}"
        else -> span.name()
    }

    private fun finishedWithoutError(span: MutableSpan) =
        isBlank(span.tag("error")) && isBlank(span.tag(ERROR_CODE.mdcKey))

    private fun getErrorMessage(span: MutableSpan) = when (val s = span.tag(ERROR_MESSAGE.mdcKey)) {
        null -> Optional.ofNullable(span.tag("error"))
        else -> Optional.of(s)
    }

    private fun getErrorCode(span: MutableSpan): Optional<String> {
        val errorCode = span.tag(ERROR_CODE.mdcKey)
        return if (errorCode == null && isNotBlank(span.tag("error"))) {
            // sometimes we have span with error but no error code, in that case we assign a default errorCode
            Optional.of("ERROR")
        } else Optional.ofNullable(errorCode)
    }

    private fun getHttpStatusCode(span: MutableSpan) = Optional.ofNullable(span.tag("http.status_code"))

    private fun getHttpMethod(span: MutableSpan) = Optional.ofNullable(span.tag("http.method"))

    private fun getHttpPath(span: MutableSpan) = Optional.ofNullable(span.tag("http.path"))

    private fun getPartner(span: MutableSpan) = Optional.ofNullable(span.tag(PARTNER.mdcKey))
}
