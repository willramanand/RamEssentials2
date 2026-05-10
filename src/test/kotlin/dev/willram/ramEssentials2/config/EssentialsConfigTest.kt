package dev.willram.ramEssentials2.config

import org.junit.Assert.assertEquals
import org.junit.Test

class EssentialsConfigTest {
    @Test
    fun renderMessageReplacesNamedPlaceholders() {
        val rendered = EssentialsConfig.renderMessage(
            "<yellow><player> paid <amount>",
            mapOf("player" to "Will", "amount" to "$10")
        )

        assertEquals("<yellow>Will paid $10", rendered)
    }

    @Test
    fun resolveMaxHomesUsesHighestMatchingPermissionLimit() {
        val limit = EssentialsConfig.resolveMaxHomes(
            defaultLimit = 3,
            permissionLimits = mapOf("vip" to 5, "staff" to 10)
        ) { permission -> permission == "ramessentials.homes.limit.staff" }

        assertEquals(10, limit)
    }
}
