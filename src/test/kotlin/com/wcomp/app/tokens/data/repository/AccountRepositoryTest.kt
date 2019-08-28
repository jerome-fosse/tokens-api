package com.wcomp.app.tokens.data.repository

import com.wcomp.app.tokens.DockerTest
import com.wcomp.app.tokens.UnitTest
import com.wcomp.app.tokens.UsingMongoDB
import com.wcomp.app.tokens.config.TestMongoConfig
import com.wcomp.app.tokens.data.model.Account
import com.wcomp.app.tokens.data.model.Device
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestMongoConfig::class])
@ActiveProfiles(profiles = ["test"])
@UnitTest
@DockerTest
class AccountRepositoryTest(@Autowired val mongoOperations: ReactiveMongoOperations) : UsingMongoDB() {
    private lateinit var repository : AccountCustomRepository

    @BeforeEach
    internal fun beforeEach() {
        repository = AccountCustomRepositoryImpl(mongoOperations)
    }

    @Test
    fun `should create a new account with device if account does not exist`() {
        // Given
        // collection is empty so there is 0 accounts

        // When
        val acc = repository.addDeviceToExistingAccountOrCreateNewAccount("deviceid", "iuc").block()

        // Then A valid Account is returned
        assertThat(acc).isNotNull
        assertThat(acc.iuc).isEqualTo("iuc")
        assertThat(acc.devices.size).isEqualTo(1)
        assertThat(acc.devices[0].deviceId).isEqualTo("deviceid")
        assertThat(acc.devices[0].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc.devices[0].active).isEqualTo(true)

        // And account is saved in db
        val acc2 = mongoOperations.findOne(query(where("_id").`is`("iuc")), Account::class.java).block()
        assertThat(acc2).isNotNull
        assertThat(acc2.iuc).isEqualTo("iuc")
        assertThat(acc2.devices.size).isEqualTo(1)
        assertThat(acc2.devices[0].deviceId).isEqualTo("deviceid")
        assertThat(acc2.devices[0].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc2.devices[0].active).isEqualTo(true)
    }

    @Test
    fun `should add device to an existing account if account does exist`() {
        // Given An existing account
        mongoOperations.save(Account(iuc = "1234", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now().minusDays(1))
        ))).block()

        // When
        val acc = repository.addDeviceToExistingAccountOrCreateNewAccount("02", "1234").block()

        // Then A valid Account is returned
        assertThat(acc).isNotNull
        assertThat(acc.iuc).isEqualTo("1234")
        assertThat(acc.devices.size).isEqualTo(2)
        assertThat(acc.devices[0].deviceId).isEqualTo("01")
        assertThat(acc.devices[1].deviceId).isEqualTo("02")
        assertThat(acc.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc.devices[1].active).isEqualTo(true)

        // And account is updated in db
        val acc2 = mongoOperations.findOne(query(where("_id").`is`("1234")), Account::class.java).block()
        assertThat(acc2).isNotNull
        assertThat(acc2.iuc).isEqualTo("1234")
        assertThat(acc2.devices.size).isEqualTo(2)
        assertThat(acc2.devices[0].deviceId).isEqualTo("01")
        assertThat(acc2.devices[1].deviceId).isEqualTo("02")
        assertThat(acc2.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc2.devices[1].active).isEqualTo(true)
    }

    @Test
    fun `should update device of an existing account if account exist and it has a device with the same deviceId`() {
        // Given An existing account
        mongoOperations.save(Account(iuc = "1234", devices = listOf(
                Device(deviceId = "01", active = false, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "02", active = false, lastSeen = LocalDateTime.now().minusDays(2)),
                Device(deviceId = "03", active = false, lastSeen = LocalDateTime.now().minusDays(3))
        ))).block()

        // When Adding a device to the account
        val acc = repository.addDeviceToExistingAccountOrCreateNewAccount("02", "1234").block()

        // Then A valid Account is returned
        assertThat(acc).isNotNull
        assertThat(acc.iuc).isEqualTo("1234")
        assertThat(acc.devices.size).isEqualTo(3)
        assertThat(acc.devices[0].deviceId).isEqualTo("01")
        assertThat(acc.devices[0].active).isEqualTo(false)
        assertThat(acc.devices[1].deviceId).isEqualTo("02")
        assertThat(acc.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc.devices[1].active).isEqualTo(true)
        assertThat(acc.devices[2].deviceId).isEqualTo("03")
        assertThat(acc.devices[2].active).isEqualTo(false)

        // And account is updated in db
        val acc2 = mongoOperations.findOne(query(where("_id").`is`("1234")), Account::class.java).block()
        assertThat(acc2).isNotNull
        assertThat(acc2.iuc).isEqualTo("1234")
        assertThat(acc2.devices.size).isEqualTo(3)
        assertThat(acc2.devices[0].deviceId).isEqualTo("01")
        assertThat(acc2.devices[0].active).isEqualTo(false)
        assertThat(acc2.devices[1].deviceId).isEqualTo("02")
        assertThat(acc2.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(acc2.devices[1].active).isEqualTo(true)
        assertThat(acc2.devices[2].deviceId).isEqualTo("03")
        assertThat(acc2.devices[2].active).isEqualTo(false)
    }

    @Test
    fun `should deactivate a device for an account`() {
        // Given 2 Accounts with device 02 active
        mongoOperations.save(Account(iuc = "123", devices = listOf(
                Device(deviceId = "01", active = false, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now().minusDays(2))
        ))).block()

        mongoOperations.save(Account(iuc = "456", devices = listOf(
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "04", active = false, lastSeen = LocalDateTime.now().minusDays(2))
        ))).block()

        // When I deactivate device 02 for account 123
        val count = repository.deactivateDeviceForAccount("02", "123").block()

        // Then 1 account should have been updated
        assertThat(count).isEqualTo(1)

        // And device 02 is inactive for account 123
        val acc123 = mongoOperations.findOne(query(where("_id").`is`("123")), Account::class.java).block()
        val dev1 = getDevice(acc123, "02")
        assertThat(dev1).isNotNull
        assertThat(dev1.active).isFalse()

        // And device 02 is still active for account 456
        val acc456 = mongoOperations.findOne(query(where("_id").`is`("456")), Account::class.java).block()
        val dev2 = getDevice(acc456, "02")
        assertThat(dev2).isNotNull
        assertThat(dev2.active).isTrue()
    }

    @Test
    fun `should deactivate a device for all accounts other than the one with given UIC`() {
        // Given 3 Accounts with the device 02 active
        mongoOperations.save(Account(iuc = "123", devices = listOf(
                Device(deviceId = "01", active = false, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now().minusDays(2)),
                Device(deviceId = "03", active = true, lastSeen = LocalDateTime.now().minusDays(3))
        ))).block()

        mongoOperations.save(Account(iuc = "456", devices = listOf(
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "04", active = false, lastSeen = LocalDateTime.now().minusDays(2))
        ))).block()

        mongoOperations.save(Account(iuc = "789", devices = listOf(
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now().minusDays(1)),
                Device(deviceId = "05", active = false, lastSeen = LocalDateTime.now().minusDays(2))
        ))).block()

        // When I activate device 02 for account 789
        repository.deactivateDeviceForAccountsWhereUIDNotEqual("02", "789").block()

        // Then device 02 is deactivated for account 123 and other devices are unchanged
        val acc123 = mongoOperations.findOne(query(where("_id").`is`("123")), Account::class.java).block()
        assertThat(acc123.devices.size).isEqualTo(3)
        assertThat(getDevice(acc123, "01").active).isFalse()
        assertThat(getDevice(acc123, "02").active).isFalse()
        assertThat(getDevice(acc123, "03").active).isTrue()

        // And device 02 is deactivated for account 456 and other devices are unchanged
        val acc456 = mongoOperations.findOne(query(where("_id").`is`("456")), Account::class.java).block()
        assertThat(acc456.devices.size).isEqualTo(2)
        assertThat(getDevice(acc456, "02").active).isFalse()
        assertThat(getDevice(acc456, "04").active).isFalse()

        // And device for account 789 are unchanged
        val acc789 = mongoOperations.findOne(query(where("_id").`is`("789")), Account::class.java).block()
        assertThat(acc789.devices.size).isEqualTo(2)
        assertThat(getDevice(acc789, "02").active).isTrue()
        assertThat(getDevice(acc789, "05").active).isFalse()
    }

    @Test
    fun `should update device with maas token for an account with the given iuc`() {
        // Given an Account with 2 devices
        mongoOperations.save(Account(iuc = "123456789", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When I want to save a maasToken for device 01
        val account = repository.saveMaasTokenForDeviceAndAccount("01", "123456789", "maas01").block()

        // Device 01 should have been updated
        assertThat(account).isNotNull
        assertThat(account.iuc).isEqualTo("123456789")
        assertThat(account.devices.size).isEqualTo(2)
        assertThat(account.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isEqualTo("maas01")

        // Device 02 isn't updated
        assertThat(account.devices[1].deviceId).isEqualTo("02")
        assertThat(account.devices[1].maasToken).isNull()
    }

    @Test
    fun `should not update other accounts with same device when updating device with maas token`() {
        // Given an Account with 2 devices
        mongoOperations.save(Account(iuc = "123456789", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // And a second Account with the same devices
        mongoOperations.save(Account(iuc = "987654321", devices = listOf(
                Device(deviceId = "01", active = true, lastSeen = LocalDateTime.now()),
                Device(deviceId = "02", active = true, lastSeen = LocalDateTime.now())
        ))).block()

        // When I want to save a maasToken for device 01 of account 123456789
        val account = repository.saveMaasTokenForDeviceAndAccount("01", "123456789", "maas01").block()

        // Device 01 of account 123456789 should have been updated
        assertThat(account).isNotNull
        assertThat(account.iuc).isEqualTo("123456789")
        assertThat(account.devices.size).isEqualTo(2)
        assertThat(account.devices[0].deviceId).isEqualTo("01")
        assertThat(account.devices[0].maasToken).isEqualTo("maas01")

        // Device 01 of Account 987654321 isn't updated
        val acc2 = mongoOperations.findById("987654321", Account::class.java).block()
        assertThat(acc2).isNotNull
        assertThat(acc2.iuc).isEqualTo("987654321")
        assertThat(acc2.devices.size).isEqualTo(2)
        assertThat(acc2.devices[0].deviceId).isEqualTo("01")
        assertThat(acc2.devices[0].maasToken).isNull()
    }

    private fun getDevice(account: Account?, deviceId: String): Device {
        assertThat(deviceId).isNotNull()
        return account?.devices?.filter { it.deviceId == deviceId }!!.first()
    }
}