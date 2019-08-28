package com.wcomp.app.tokens.service

import com.auth0.jwt.exceptions.JWTVerificationException
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.repository.AccountRepository
import com.wcomp.app.tokens.utils.JWTValidator
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import com.wcomp.app.tokens.utils.extensions.obfuscateEnd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

interface TokenService {
    fun registerToken(idToken: String, accessToken: String, deviceId: String): Mono<Account>
    fun saveMaasToken(idToken: String, deviceId: String, maasToken: String): Mono<Account>
    fun logoutAndInvalidateRefreshToken(idToken: String, refreshToken: String, deviceId: String): Mono<Boolean>
}

@Component
class TokenServiceImpl (
        private val jwtValidator: JWTValidator,
        private val accountRepository: AccountRepository,
        private val connectClient: ConnectClient,
        @Value("\${features.access-token-validation.enabled}")
        private val accessTokenValidationEnabled: Boolean
    ): TokenService {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenService::class.java)
    }

    override fun registerToken(idToken: String, accessToken: String, deviceId: String): Mono<Account> {
        val iuc = try { jwtValidator.getIuc(idToken) } catch (e: JWTVerificationException) {
            logger.error("Unable to decode idToken : idToken=${idToken.obfuscateBegin(20)}", e)
            return Mono.error(e)
        }

        return Mono.just(accessTokenValidationEnabled)
                .flatMap { flag ->
                    if (flag) {
                        logger.debug("Calling Connect to validate the access token : accessToken=${accessToken.obfuscateEnd(10)}")
                        connectClient.validateAccessToken(accessToken)
                                .doOnSuccess { logger.debug("The access token is valid : accessToken=${accessToken.obfuscateEnd(10)}") }
                                .thenReturn(true)
                    } else {
                        Mono.just(true)
                    }
                }
                .then(accountRepository.deactivateDeviceForAccountsWhereUIDNotEqual(deviceId, iuc))
                .then(accountRepository.addDeviceToExistingAccountOrCreateNewAccount(deviceId, iuc))
    }

    override fun saveMaasToken(idToken: String, deviceId: String, maasToken: String): Mono<Account> {
        return Mono.just(idToken)
                .map { jwtValidator.decodeAndCheckValidity(it).subject }
                .flatMap { iuc ->
                    logger.debug("Saving Maas Token ${maasToken.obfuscateEnd(10)} for device $deviceId in account $iuc.")
                    accountRepository.saveMaasTokenForDeviceAndAccount(deviceId, iuc, maasToken)
                }
    }

    override fun logoutAndInvalidateRefreshToken(idToken: String, refreshToken: String, deviceId: String): Mono<Boolean> {
        return Mono.just(idToken)
                .map { jwtValidator.getIuc(it) }
                .flatMap { iuc ->
                    accountRepository.deactivateDeviceForAccount(deviceId, iuc)
                            .doOnSuccess { logger.debug("Device(s) have been modified for account : nbDevices=$it, iuc=$iuc") }
                            .then(connectClient.invalidRefreshToken(refreshToken))
                }
    }
}