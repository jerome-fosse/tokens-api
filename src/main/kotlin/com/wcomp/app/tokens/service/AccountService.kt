package com.wcomp.app.tokens.service

import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.data.repository.AccountRepository
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import com.wcomp.app.tokens.exeption.NotMatchingDataException
import com.wcomp.app.tokens.model.ConnectAccountInfoResponse
import com.wcomp.app.tokens.model.DeviceResponse
import com.wcomp.app.tokens.connect.ConnectClient
import com.wcomp.app.tokens.connect.cache.AccountInfoCacheRepository
import com.wcomp.app.tokens.connect.model.CreateAccount
import com.wcomp.app.tokens.utils.JWTValidator
import com.wcomp.app.tokens.utils.extensions.obfuscateBegin
import com.wcomp.app.tokens.utils.extensions.obfuscateEmail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*

interface AccountService {
    fun getAccountWithActiveDevices(iuc: String, active: Boolean): Mono<Account>
    fun createAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean>
    fun migrateAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean>
    fun getAccountInfo(deviceId: String, idToken: String): Mono<ConnectAccountInfoResponse>
    fun getAccountWithDevice(idToken: String, deviceId: String): Mono<Account>
}

@Component
class AccountServiceImpl(
        private val jwtValidator: JWTValidator,
        private val connectClient: ConnectClient,
        private val repository: AccountRepository,
        private val accountInfoCacheRepository: AccountInfoCacheRepository): AccountService {

    companion object {
        private val logger = LoggerFactory.getLogger(AccountService::class.java)
    }

    override fun getAccountWithActiveDevices(iuc: String, active: Boolean): Mono<Account> {
        logger.debug("Getting account with id = $iuc")
        return repository.findById(iuc)
                .switchIfEmpty(Mono.error(AccountNotFoundException(iuc)))
                .map { account -> account.copy(devices = account.devices.filter { device -> device.active == active }) }
    }

    override fun createAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
        logger.debug("createConnectAccount with email \"${email.obfuscateEmail()}\", birthDate \"$birthDate\" and language\"$language\"")
        return connectClient.createAccount(CreateAccount(email, password, firstname, lastname, birthDate), false, language)
    }

    override fun migrateAccount(email: String, password: String, firstname: String, lastname: String, birthDate: LocalDate, language: Locale): Mono<Boolean> {
        logger.debug("migrateAccount with email \"${email.obfuscateEmail()}\", birthDate \"$birthDate\" and language\"$language\"")
        return connectClient.createAccount(CreateAccount(email, password, firstname, lastname, birthDate), true, language)
    }

    override fun getAccountInfo(deviceId: String, idToken: String): Mono<ConnectAccountInfoResponse> {
        return Mono.just(idToken)
                .map { jwtValidator.getIuc(it) }
                .flatMap { iuc ->
                    logger.debug("Get AccountInfo for iuc=$iuc and device=$deviceId")
                    findActiveDevice(deviceId = deviceId, iuc = iuc)
                            .switchIfEmpty(Mono.error(NotMatchingDataException("[NO_ACTIVE_DEVICE_FOUND_FOR_IUC] iuc '$iuc', deviceId '$deviceId'")))
                            .flatMap { device ->
                                accountInfoCacheRepository.findById(iuc)
                                    .map { Mono.just(it) }
                                    .orElseGet { connectClient.getAccountInfo(iuc).map { accountInfoCacheRepository.save(it) } }
                                    .map {
                                        ConnectAccountInfoResponse(
                                                id = it.iuc, email = it.email, firstname = it.firstName, lastname = it.lastName, birthDate = it.birthdate,
                                                phoneNumber = it.mobileNumber, device = DeviceResponse.of(device)
                                        )
                                    }
                            }
                }
    }

    override fun getAccountWithDevice(idToken: String, deviceId: String): Mono<Account> {
        logger.debug("Getting account with idToken = ${idToken.obfuscateBegin(20)}")
        val iuc = jwtValidator.getIuc(idToken)
        return repository.findById(iuc)
                .switchIfEmpty(Mono.error(AccountNotFoundException(iuc)))
                .filter { acc -> acc.devices.find { device -> device.deviceId == deviceId } != null }
                .switchIfEmpty(Mono.error(NotMatchingDataException("No Account found with id = $iuc and deviceId = $deviceId")))
    }

    private fun findActiveDevice(deviceId: String, iuc: String): Mono<Device> {
        logger.debug("Finding active device : deviceId=$deviceId, iuc=$iuc")
        return repository.findById(iuc)
                .switchIfEmpty(Mono.empty())
                .map { acc -> acc.devices.filter { device -> device.active && device.deviceId == deviceId } }
                .flatMap {
                    if (it.isEmpty()) {
                        Mono.empty()
                    } else {
                        Mono.just(it.first())
                    }
                }
    }
}