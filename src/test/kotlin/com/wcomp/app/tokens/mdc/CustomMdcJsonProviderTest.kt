package com.wcomp.app.tokens.mdc

import ch.qos.logback.classic.spi.ILoggingEvent
import com.fasterxml.jackson.core.JsonGenerator
import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*



@UnitTest
class CustomMdcJsonProviderTest {

    @Test
    fun `writeTo should copy header tag in MDC`() {
        // Given
        val customMdcJsonProvider = CustomMdcJsonProvider()
        val loggingEvent = mock(ILoggingEvent::class.java)
        val mdcMap = HashMap<String, String>()
        mdcMap["X-CorrelationId"] = "cid"
        mdcMap["X-UserId"] = "uid"
        mdcMap["X-Forwarded-For"] = "forwardedFor"
        mdcMap["X-Device-Type"] = "deviceType"
        mdcMap["X-Device-Platform"] = "ANDROID"
        mdcMap["X-Device-ServerUrl"] = "deviceServer"
        mdcMap["X-RequestId"] = "reqId"
        mdcMap["X-Realtime-Id"] = "realTimeId"
        `when`(loggingEvent.mdcPropertyMap).thenReturn(mdcMap)

        // When
        customMdcJsonProvider.writeTo(mock(JsonGenerator::class.java), loggingEvent)

        // Then
        val expectedMDCMap = HashMap<String, String>()
        expectedMDCMap["X-CorrelationId"] = "cid"
        expectedMDCMap["X-UserId"] = "uid"
        expectedMDCMap["X-Forwarded-For"] = "forwardedFor"
        expectedMDCMap["X-Device-Type"] = "deviceType"
        expectedMDCMap["X-Device-Platform"] = "ANDROID"
        expectedMDCMap["X-Device-ServerUrl"] = "deviceServer"
        expectedMDCMap["X-RequestId"] = "reqId"
        expectedMDCMap["X-Realtime-Id"] = "realTimeId"
        expectedMDCMap["correlation_id"] = "cid"
        expectedMDCMap["user_id"] = "uid"
        expectedMDCMap["x_forwarded_for"] = "forwardedFor"
        expectedMDCMap["device_type"] = "deviceType"
        expectedMDCMap["device_platform"] = "ANDROID"
        expectedMDCMap["device_server_url"] = "deviceServer"
        expectedMDCMap["request_id"] = "reqId"
        expectedMDCMap["realtime_id"] = "realTimeId"
        assertThat(loggingEvent.mdcPropertyMap).isEqualTo(expectedMDCMap)
    }
}