package com.wcomp.app.tokens.config

import brave.handler.FinishedSpanHandler
import com.wcomp.app.tokens.mdc.AuditSpanHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SleuthConfig {

    @Bean
    internal fun auditFinishedSpan(): FinishedSpanHandler {
        return AuditSpanHandler()
    }

}
