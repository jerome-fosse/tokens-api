package com.wcomp.app.tokens.data.repository

import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import com.wcomp.app.tokens.exeption.AccountNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface AccountCustomRepository {
    fun addDeviceToExistingAccountOrCreateNewAccount(deviceId: String, iuc: String): Mono<Account>
    fun deactivateDeviceForAccountsWhereUIDNotEqual(deviceId: String, iuc: String): Mono<Long>
    fun deactivateDeviceForAccount(deviceID: String, iuc: String): Mono<Long>
    fun saveMaasTokenForDeviceAndAccount(deviceId: String, iuc: String, maasToken: String): Mono<Account>
}

@Repository
@Primary
class AccountCustomRepositoryImpl(private val mongoOperations: ReactiveMongoOperations): AccountCustomRepository {
    companion object {
        val logger = LoggerFactory.getLogger(AccountRepository::class.java)
    }

    override fun addDeviceToExistingAccountOrCreateNewAccount(deviceId: String, iuc: String): Mono<Account> {
        logger.debug("addDeviceToExistingAccountOrCreateNewAccount - deviceId: {}, iuc: {}", deviceId, iuc)

        return mongoOperations.save(mongoOperations.findById(iuc, Account::class.java)
                .map { it.upsertDevice(deviceId) }
                .switchIfEmpty(Mono.just(Account(iuc = iuc, devices = listOf(Device(deviceId = deviceId, lastSeen = LocalDateTime.now(), active = true)))))
        )
        .doOnSuccess { logger.debug("Device $deviceId added for iuc $iuc.") }
        .doOnError { logger.error("error while adding device to iuc=$iuc, deviceId=$deviceId", it) }
    }

    override fun deactivateDeviceForAccountsWhereUIDNotEqual(deviceId: String, iuc: String): Mono<Long> {
        logger.debug("deactivateDeviceForAccountsWhereUIDNotEqual - deviceId: {}, iuc: {}", deviceId, iuc)

        return mongoOperations.updateMulti(
                query(where("_id").ne(iuc).and("devices.deviceId").`is`(deviceId)),
                Update().set("devices.$.active", false),
                Account::class.java
        )
        .map { it.modifiedCount }
        .doOnSuccess { logger.debug("$it device(s) deactivated for iuc $iuc and deviceId $deviceId.") }
        .doOnError { logger.error("Error while deactivating device for other accounts beside iuc=$iuc, deviceId=$deviceId", it) }
    }

    override fun deactivateDeviceForAccount(deviceID: String, iuc: String): Mono<Long> {
        logger.debug("deactivateDeviceForAccount - deviceId: {}, iuc: {}", deviceID, iuc)

        return mongoOperations.updateFirst(
                query(where("_id").`is`(iuc).and("devices").elemMatch(where("deviceId").`is`(deviceID).and("active").`is`(true))),
                Update().set("devices.$.active", false),
                Account::class.java
        ).map { it.modifiedCount }
    }

    override fun saveMaasTokenForDeviceAndAccount(deviceId: String, iuc: String, maasToken: String): Mono<Account> {
        logger.debug("saveMaasTokenForDeviceAndAccount - deviceId: {}, iuc: {}", deviceId, iuc)

        return mongoOperations.save(mongoOperations.findById(iuc, Account::class.java)
                .map { it.updateDeviceWithMaasToken(deviceId, maasToken) }
                .switchIfEmpty(Mono.error(AccountNotFoundException(iuc)))
        )
    }
}