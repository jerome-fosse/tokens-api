package com.wcomp.app.tokens.mdc

/**
 * Classe très largement inspirée de celle du projet mdc-spring-sleuth version sping-boot 1.
 * Elle permet de mettre localement des objets dans le MDC et de les supprimer dans la foulée. Elle peut être notamment
 * très utile pour des logs locaux.
 *
 * /!\ Si vous faites une mise à jour ici pensez à la reporter dans l'autre projet tant que tout ne sera pas migré en spring-boot 2
 */
enum class MDCTags(val headerName: String? = null, val mdcKey: String) {
    X_CORRELATION_ID("X-CorrelationId", "correlation_id"),
    X_USER_ID("X-UserId", "user_id"),
    X_FORWARDED_FOR("X-Forwarded-For", "x_forwarded_for"),
    X_DEVICE_TYPE("X-Device-Type", "device_type"),
    X_DEVICE_PLATFORM("X-Device-Platform", "device_platform"),
    X_DEVICE_SERVER_URL("X-Device-ServerUrl", "device_server_url"),
    X_REQUEST_ID("X-RequestId", "request_id"),
    X_REALTIME_ID("X-Realtime-Id", "realtime_id"),
    ELAPSED_TIME_MS(mdcKey = "elapsed_time_ms"),
    LOG_TYPE(mdcKey = "log_type"),
    MESSAGE_SOURCE(mdcKey = "message_source"),
    PARTNER(mdcKey = "partner"),
    SERVICE(mdcKey = "service"),
    ERROR_CODE(mdcKey = "error_code"),
    ERROR_MESSAGE(mdcKey = "error_message"),
    HTTP_RESPONSE_CODE(mdcKey = "http_response_code"),
    HTTP_METHOD(mdcKey = "http_method"),
    HTTP_PATH(mdcKey = "http_path"),
    HTTP_INPUT(mdcKey = "http_input"),
    HTTP_OUTPUT(mdcKey = "http_output");

    val isHeader: Boolean
        get() = headerName != null

    companion object {
        val headersTags = values().filter { it.isHeader }.toList()
    }
}
