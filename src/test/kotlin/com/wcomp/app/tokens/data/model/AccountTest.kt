package com.wcomp.app.tokens.data.model

import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@UnitTest
class AccountTest {

    @Test
    fun `doit inserer un device si celui ci n'exite pas dans la liste des devices`() {
        // Given
        val acc = Account(iuc = "123456", devices = listOf(Device(deviceId = "1", lastSeen = LocalDateTime.now().minusDays(1), active = false)))

        // When
        val newacc = acc.upsertDevice("2")

        // Then
        assertThat(newacc.devices.size).isEqualTo(2)
        assertThat(newacc.devices[0].deviceId).isEqualTo("1")
        assertThat(newacc.devices[1].deviceId).isEqualTo("2")
        assertThat(newacc.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(newacc.devices[1].active).isTrue()
    }

    @Test
    fun `doit inserer un device lors que la liste des devices est vide`() {
        // Given
        val acc = Account(iuc = "123456", devices = emptyList())

        // When
        val newacc = acc.upsertDevice("1")

        // Then
        assertThat(newacc.devices.size).isEqualTo(1)
        assertThat(newacc.devices[0].deviceId).isEqualTo("1")
        assertThat(newacc.devices[0].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(newacc.devices[0].active).isTrue()
    }

    @Test
    fun `doit mettre a jour le device lorsque celui ci est present dans la liste des devices`() {
        // Given
        val acc = Account(iuc = "123456", devices = listOf(
                Device(deviceId = "1", lastSeen = LocalDateTime.now().minusDays(1), active = false),
                Device(deviceId = "2", lastSeen = LocalDateTime.now().minusDays(2), active = false),
                Device(deviceId = "3", lastSeen = LocalDateTime.now().minusDays(2), active = false)
        ))

        // When
        val newacc = acc.upsertDevice("2")

        // Then
        assertThat(newacc.devices.size).isEqualTo(3)
        assertThat(newacc.devices[0].deviceId).isEqualTo("1")
        assertThat(newacc.devices[0].active).isFalse()
        assertThat(newacc.devices[1].deviceId).isEqualTo("2")
        assertThat(newacc.devices[1].lastSeen.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(newacc.devices[1].active).isTrue()
        assertThat(newacc.devices[2].deviceId).isEqualTo("3")
        assertThat(newacc.devices[2].active).isFalse()
    }
}