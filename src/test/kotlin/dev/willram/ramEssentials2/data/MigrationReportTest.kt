package dev.willram.ramEssentials2.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationReportTest {
    @Test
    fun reportKnowsWhetherAnythingMigrated() {
        assertFalse(MigrationReport(markerExisted = false).migrated)
        assertTrue(MigrationReport(markerExisted = false, accounts = 2).migrated)
    }

    @Test
    fun markerTextIncludesCountsAndBackupPath() {
        val text = MigrationReport(
            markerExisted = false,
            backupPath = "backup",
            configFiles = 1,
            accounts = 2,
            warps = 3,
            players = 4
        ).toMarkerText()

        assertTrue(text.contains("backup=backup"))
        assertTrue(text.contains("accounts=2"))
        assertTrue(text.contains("players=4"))
    }
}
