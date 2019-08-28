package com.wcomp.app.tokens.mdc

import brave.handler.MutableSpan
import brave.propagation.B3Propagation
import brave.propagation.ExtraFieldPropagation
import brave.propagation.TraceContext
import ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.mdc.MDCTags.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*


@UnitTest
class AuditSpanHandlerTest {
    private lateinit var listAppender: ListAppender<ILoggingEvent>

    private val toTest = AuditSpanHandler()
    private val propagationFieldFactory = ExtraFieldPropagation
            .newFactory(B3Propagation.FACTORY, "X-CorrelationId",
                    "X-UserId",
                    "X-DeviceId",
                    "X-Realtime-Id",
                    "X-Device-Type",
                    "X-Device-Platform",
                    "X-Device-ServerUrl")

    @BeforeEach
    fun beforeEach() {
        val xlLogger = LoggerFactory.getLogger("AUDIT_LOGGER") as Logger
        xlLogger.detachAndStopAllAppenders()
        xlLogger.level = DEBUG

        this.listAppender = ListAppender()
        this.listAppender.start()
        xlLogger.addAppender(this.listAppender)
    }

    @Test
    fun `should always log service when service is available`() {
        // Given
        val span = MutableSpan()
        span.tag("service", "ServiceBiduleMuche");
        span.tag("http.method", "POST");
        span.tag("http.path", "/my/awsome/service");
        span.name("myAwsomeSpanNameThatShouldNotBeUsedInAuditLog");
        span.startTimestamp(1000);
        span.finishTimestamp(2000);
        span.tag("http.status_code", "200");

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, span)

        // Then
        assertEquals(1, listAppender.list.size)
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("ServiceBiduleMuche : duration=1ms")
        assertThat(loggingEvent.level).isEqualTo(INFO)

        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "ServiceBiduleMuche"
        expectedMDC[HTTP_RESPONSE_CODE.mdcKey] = "200"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"
        expectedMDC[HTTP_METHOD.mdcKey] = "POST"
        expectedMDC[HTTP_PATH.mdcKey] = "/my/awsome/service"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)
        // we check that the MDC is cleaned afterwards
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `should use HTTP method and HTTP path instead of span name when logging duration of finished HTTP span`() {
        // Given
        val span = MutableSpan()
        span.tag("http.method", "POST")
        span.tag("http.path", "/my/awsome/service")
        span.name("myAwsomeSpanNameThatShouldNotBeUsedInAuditLog")
        span.startTimestamp(1000)
        span.finishTimestamp(2000)
        span.tag("http.status_code", "200")

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, span)

        // Then
        assertEquals(1, listAppender.list.size)
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("POST /my/awsome/service : duration=1ms")
        assertThat(loggingEvent.level).isEqualTo(INFO)

        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "POST /my/awsome/service" // should not use span name
        expectedMDC[HTTP_RESPONSE_CODE.mdcKey] = "200"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"
        expectedMDC[HTTP_METHOD.mdcKey] = "POST"
        expectedMDC[HTTP_PATH.mdcKey] = "/my/awsome/service"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)
        // we check that the MDC is cleaned afterwards
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `should log finished span duration with level ERROR and put HTTP status code and trace context in MDC when HTTP span ended with error`() {
        // Given
        val httpErrorSpan = MutableSpan()
        httpErrorSpan.name("myAwsomeService")
        httpErrorSpan.startTimestamp(1000)
        httpErrorSpan.finishTimestamp(2000)
        httpErrorSpan.tag("http.method", "POST")
        httpErrorSpan.tag("http.path", "/my/awsome/service")
        httpErrorSpan.tag("http.status_code", "400")
        httpErrorSpan.tag("error_code", "400")
        httpErrorSpan.tag("error_message", "Bad request")

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, httpErrorSpan)

        // Then
        assertEquals(1, listAppender.list.size)
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("POST /my/awsome/service : duration=1ms, error_code=400, error_msg='Bad request'")
        assertThat(loggingEvent.level).isEqualTo(ERROR)

        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "POST /my/awsome/service"
        expectedMDC[HTTP_RESPONSE_CODE.mdcKey] = "400"
        expectedMDC[HTTP_METHOD.mdcKey] = "POST"
        expectedMDC[HTTP_PATH.mdcKey] = "/my/awsome/service"
        expectedMDC[ERROR_CODE.mdcKey] = "400"
        expectedMDC[ERROR_MESSAGE.mdcKey] = "Bad request"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)
        // we check that the MDC is cleaned
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `should log finished span duration with level ERROR and put trace context in MDC when span ended with error`() {
        // Given
        val errorSpan = MutableSpan()
        errorSpan.name("myAwsomeService")
        errorSpan.startTimestamp(1000)
        errorSpan.finishTimestamp(2000)
        errorSpan.tag("error_code", "KO")
        errorSpan.tag("error", "Unexpected error")

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, errorSpan)

        // Then
        assertEquals(1, listAppender.list.size.toLong())
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("myAwsomeService : duration=1ms, error_code=KO, error_msg='Unexpected error'")
        assertThat(loggingEvent.level).isEqualTo(ERROR)
        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "myAwsomeService"
        expectedMDC[ERROR_CODE.mdcKey] = "KO"
        expectedMDC[ERROR_MESSAGE.mdcKey] = "Unexpected error"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"

        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)
        // we check that the MDC is cleaned
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `should log finished span duration with level ERROR and set default error code in MDC when span ended with error but has no error code`() {
        // Given
        val spanInErrorWhithoutErrorCode = MutableSpan()
        spanInErrorWhithoutErrorCode.name("myAwsomeService")
        spanInErrorWhithoutErrorCode.startTimestamp(1000)
        spanInErrorWhithoutErrorCode.finishTimestamp(2000)
        spanInErrorWhithoutErrorCode.tag("error", "Unexpected error")

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, spanInErrorWhithoutErrorCode)

        // Then
        assertEquals(1, listAppender.list.size)
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("myAwsomeService : duration=1ms, error_code=ERROR, error_msg='Unexpected error'")
        assertThat(loggingEvent.level).isEqualTo(ERROR)
        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "myAwsomeService"
        expectedMDC[ERROR_CODE.mdcKey] = "ERROR" // sets a default error code
        expectedMDC[ERROR_MESSAGE.mdcKey] = "Unexpected error"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)
        // we check that the MDC is cleaned
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `should log partner when available`() {
        // Given
        val span = MutableSpan()
        span.name("myAwsomeService")
        span.tag("partner", "CapriciousPartner")
        span.startTimestamp(1000)
        span.finishTimestamp(2000)

        val traceContext = givenSpanTraceContext()

        // When
        toTest.handle(traceContext, span)

        // Then
        assertEquals(1, listAppender.list.size)
        val loggingEvent = listAppender.list[0]

        assertThat(loggingEvent.formattedMessage).isEqualTo("myAwsomeService : duration=1ms")
        assertThat(loggingEvent.level).isEqualTo(INFO)

        val expectedMDC = defaultMDCContext()
        expectedMDC[SERVICE.mdcKey] = "myAwsomeService"
        expectedMDC[PARTNER.mdcKey] = "CapriciousPartner"
        expectedMDC[ELAPSED_TIME_MS.mdcKey] = "1"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDC)

        // we check that the MDC is cleaned afterwards
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    private fun givenSpanTraceContext(): TraceContext {
        val traceContext = propagationFieldFactory.decorate(TraceContext.newBuilder()
                .spanId(10)
                .traceId(1)
                .parentId(2)
                .build())
        ExtraFieldPropagation.set(traceContext, "x-correlationid", "cid")
        ExtraFieldPropagation.set(traceContext, "x-userid", "uid")
        ExtraFieldPropagation.set(traceContext, "x-realtime-id", "realtimeId")
        ExtraFieldPropagation.set(traceContext, "x-device-type", "deviceType")
        ExtraFieldPropagation.set(traceContext, "x-device-platform", "devicePlatform")
        ExtraFieldPropagation.set(traceContext, "x-device-serverurl", "url")
        return traceContext
    }

    private fun defaultMDCContext(): MutableMap<String, String> {
        val expectedMDC = HashMap<String, String>()
        // baggage
        expectedMDC[X_CORRELATION_ID.mdcKey] = "cid"
        expectedMDC[X_USER_ID.mdcKey] = "uid"
        expectedMDC[X_REALTIME_ID.mdcKey] = "realtimeId"
        expectedMDC[X_DEVICE_TYPE.mdcKey] = "deviceType"
        expectedMDC[X_DEVICE_PLATFORM.mdcKey] = "devicePlatform"
        expectedMDC[X_DEVICE_SERVER_URL.mdcKey] = "url"
        // span  context
        expectedMDC["spanId"] = "10"
        expectedMDC["traceId"] = "1"
        expectedMDC["parentId"] = "2"
        return expectedMDC
    }

}