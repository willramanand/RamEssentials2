package dev.willram.ramEssentials2.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarpDataTest {
    @Test
    fun blankPermissionIsVisibleToEveryone() {
        val warp = WarpData()

        assertTrue(warp.visibleTo { false })
    }

    @Test
    fun permissionRestrictedWarpChecksPermission() {
        val warp = WarpData()
        warp.permission = "ramessentials.warp.vip"

        assertFalse(warp.visibleTo { false })
        assertTrue(warp.visibleTo { permission -> permission == "ramessentials.warp.vip" })
    }
}
