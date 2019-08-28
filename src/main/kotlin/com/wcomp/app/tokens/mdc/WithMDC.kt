package com.wcomp.app.tokens.mdc

import org.apache.commons.lang3.StringUtils
import org.slf4j.MDC
import java.io.Closeable

/**
 * Classe très largement inspirée de celle du projet mdc-spring-sleuth version sping-boot 1.
 * Elle permet de mettre localement des objets dans le MDC et de les supprimer dans la foulée. Elle peut être notamment
 * très utile pour des logs locaux.
 *
 * /!\ Si vous faites une mise à jour ici pensez à la reporter dans l'autre projet tant que tout ne sera pas migré en spring-boot 2
 */
class WithMDC private constructor() : Closeable {

    companion object {
        fun create(): WithMDC {
            return WithMDC()
        }
    }

    private val oldKeys = mutableMapOf<String, String?>()

    fun custom(key: String, value: String?): WithMDC {
        if (value != null) {
            oldKeys[key] = MDC.get(key)
            MDC.put(key, value)
        }
        return this
    }

    fun partner(partner: String?): WithMDC {
        this.custom(MDCTags.PARTNER.mdcKey, partner)
        return this
    }

    fun service(service: String): WithMDC {
        this.custom(MDCTags.SERVICE.mdcKey, service)
        return this
    }

    fun source(source: String): WithMDC {
        this.custom(MDCTags.MESSAGE_SOURCE.mdcKey, source)
        return this
    }

    fun httpResponseCode(responseCode: String?): WithMDC {
        this.custom(MDCTags.HTTP_RESPONSE_CODE.mdcKey, responseCode)
        return this
    }

    fun logType(logType: String): WithMDC {
        this.custom(MDCTags.LOG_TYPE.mdcKey, logType)
        return this
    }

    fun elapsedTimeMs(elapsedTimeMs: String): WithMDC {
        this.custom(MDCTags.ELAPSED_TIME_MS.mdcKey, elapsedTimeMs)
        return this
    }

    fun httpMethod(method: String): WithMDC {
        this.custom(MDCTags.HTTP_METHOD.mdcKey, method)
        return this
    }

    fun httpInput(input: String): WithMDC {
        this.custom(MDCTags.HTTP_INPUT.mdcKey, input)
        return this
    }

    fun httpOutput(output: String): WithMDC {
        this.custom(MDCTags.HTTP_OUTPUT.mdcKey, output)
        return this
    }

    fun errorCode(errorCode: String?): WithMDC {
        this.custom(MDCTags.ERROR_CODE.mdcKey, errorCode)
        return this
    }

    fun errorMessage(errorMessage: String?): WithMDC {
        if (StringUtils.isNotBlank(errorMessage)) {
            this.custom(MDCTags.ERROR_MESSAGE.mdcKey, errorMessage)
        }
        return this
    }

    override fun close() {
        oldKeys.forEach { (key, value) ->
            if (value == null) {
                MDC.remove(key)
            } else {
                MDC.put(key, value)
            }
        }
    }
}
