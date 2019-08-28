package com.wcomp.app.tokens.connect

import com.wcomp.app.tokens.connect.model.*
import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.annotation.NewSpan
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder.fromHttpUrl
import reactor.core.publisher.Mono
import java.util.*

interface ConnectClient {
    fun createAccount(createAccountRequest: CreateAccount, toMigrate: Boolean, language: Locale = Locale.FRENCH): Mono<Boolean>
    fun invalidRefreshToken(refreshToken: String): Mono<Boolean>
    fun validateAccessToken(accessToken: String): Mono<TokenInfo>
    fun getAccountInfo(iuc: String): Mono<AccountInfo>
}

open class DefaultConnectClient(private val configuration: ConnectClientConfiguration, private val webClient: WebClient) : ConnectClient {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ConnectClient::class.java)
        private const val CONNECT = "Connect"
    }

    @NewSpan("ConnectPostAccounts")
    override fun createAccount(createAccountRequest: CreateAccount, toMigrate: Boolean, language: Locale): Mono<Boolean> {
        val query = fromHttpUrl(configuration.accountApiUrl)
                .queryParam("send_notif_email", configuration.sendAccountCreationNotifEmail)
                .queryParam("callback_mobile", configuration.encodedAccountCreationCallbackMobileUrl)
                .queryParam("callback", configuration.encodedAccountCreationCallbackUrl)
                .queryParam("is_migrated", toMigrate)
                .build(true)
                .toUri()

        return webClient
                .post()
                .uri(query)
                .body(fromObject(createAccountRequest))
                .header(ACCEPT_LANGUAGE, language.language)
                .retrieve()
                .onStatus(HttpStatus::isError) {resp ->
                    resp.bodyToMono(String::class.java).defaultIfEmpty("").flatMap {
                        Mono.error<PartnerException>(PartnerException("CONNECT_CREATE_ACCOUNT_ERROR", CONNECT, resp.statusCode(),
                                "Unable to create Connect account for email=${createAccountRequest.email} $it"))
                    }
                }
                .bodyToMono(String::class.java)
                .defaultIfEmpty("")
                .thenReturn(true)
    }

    @NewSpan("ConnectPostLogout")
    override fun invalidRefreshToken(refreshToken: String): Mono<Boolean> {
        LOGGER.debug("Calling Connect logout service with refresh_token={}", refreshToken.obfuscateEnd(10))
        val query = fromHttpUrl(configuration.accountApiUrl).path("/logout").build(true).toUri()

        return webClient
                .post()
                .uri(query)
                .body(fromObject(InvalidTokenBody(refreshToken)))
                .retrieve()
                .onStatus ({ it == HttpStatus.BAD_GATEWAY }) {resp ->
                    resp.bodyToMono(String::class.java).defaultIfEmpty("").flatMap {
                        Mono.error<InvalidRefreshTokenException>(InvalidRefreshTokenException(resp.statusCode(),
                                "Invalid or expired refresh token!!!" + if (it.isEmpty()) "" else "response=$it"))
                    }
                }
                .onStatus(HttpStatus::isError) {resp ->
                    resp.bodyToMono(String::class.java).defaultIfEmpty("").flatMap {
                        Mono.error<PartnerException>(PartnerException("CONNECT_INVALIDATE_REFRESH_TOKEN_UNEXPECTED_ERROR", CONNECT, resp.statusCode(),
                                "Unexpected Error!!! " + if (it.isEmpty()) "" else "response=$it"))
                    }
                }
                .bodyToMono(String::class.java)
                .defaultIfEmpty("")
                .thenReturn(true)
    }

    @NewSpan("ConnectValidateGetTokenInfo")
    override fun validateAccessToken(accessToken: String): Mono<TokenInfo> {
        LOGGER.debug("Calling Connect Tokeninfo service with access_token={}", accessToken.obfuscateEnd(10))

        val query = fromHttpUrl(configuration.openAMApiUrl).path("/tokeninfo")
                .queryParam("access_token", accessToken)
                .build(true)
                .toUri()

        return webClient
                .get()
                .uri(query)
                .retrieve()
                .onStatus({ it == HttpStatus.BAD_REQUEST}) {resp ->
                    resp.bodyToMono(Error::class.java).defaultIfEmpty(Error("CONNECT_TOKEN_INFO_BAD_REQUEST", "")).flatMap {
                        Mono.error<AccessTokenException>(AccessTokenException(resp.statusCode(), it.errorDescription))
                    }
                }
                .onStatus(HttpStatus::isError) { resp ->
                    resp.bodyToMono(Any::class.java).defaultIfEmpty(Error("CONNECT_TOKEN_INFO_UNEXPECTED_ERROR", "")).flatMap {
                        Mono.error<AccessTokenException>(AccessTokenException(resp.statusCode(), "CONNECT_TOKEN_INFO_UNEXPECTED_ERROR"))
                    }
                }
                .bodyToMono(TokenInfo::class.java)
    }

    @NewSpan("ConnectGetAccounts")
    override fun getAccountInfo(iuc: String): Mono<AccountInfo> {
        LOGGER.debug("Calling Connect GET /api/accounts service with iuc={}", iuc)

        val query = fromHttpUrl(configuration.accountApiUrl).path("/").pathSegment(iuc)
                .build(true)
                .toUri()

        return webClient
                .get()
                .uri(query)
                .retrieve()
                .onStatus(HttpStatus::isError) { resp ->
                    resp.bodyToMono(String::class.java).defaultIfEmpty("").flatMap<Throwable> {
                        Mono.error(PartnerException("CONNECT_GET_ACCOUNT_INFO_ERROR", CONNECT, resp.statusCode(),
                                "Unexpected response from the server while retrieving accountInfo for iuc=$iuc, response=$it"))
                    }
                }
                .bodyToMono(AccountInfo::class.java)
    }
}