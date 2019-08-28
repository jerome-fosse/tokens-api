package com.wcomp.app.tokens.mdc

import ch.qos.logback.classic.spi.ILoggingEvent
import com.fasterxml.jackson.core.JsonGenerator
import net.logstash.logback.composite.loggingevent.MdcJsonProvider
import java.io.IOException
import java.util.*

class CustomMdcJsonProvider : MdcJsonProvider() {

    @Throws(IOException::class)
    override fun writeTo(generator: JsonGenerator, event: ILoggingEvent) {
        copyHeaderFields(event.mdcPropertyMap)
        super.writeTo(generator, event)
    }

    /**
     * Recopie des champs headers pour qu'ils soient MDC friendly.
     * Par exemple : X-CorrelationId est recopié en correlation_id, etc.
     *
     * @param mdcPropertyMap Liste des propriétés du MDC
     */
    private fun copyHeaderFields(mdcPropertyMap: MutableMap<String, String>) {
        MDCTags.headersTags.forEach { headerTag ->
            Optional.ofNullable(mdcPropertyMap[headerTag.headerName])
                    .ifPresent { headerValue -> mdcPropertyMap[headerTag.mdcKey] = headerValue }
        }
    }


}

