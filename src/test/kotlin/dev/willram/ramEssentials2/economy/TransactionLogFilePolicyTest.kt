package dev.willram.ramEssentials2.economy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path
import java.time.Instant

class TransactionLogFilePolicyTest {
    @Test
    fun logPathRollsByDate() {
        val path = TransactionLogFilePolicy.path(
            Path.of("plugins", "RamEssentials2"),
            Instant.parse("2026-05-10T12:00:00Z")
        )

        assertEquals(Path.of("plugins", "RamEssentials2", "transactions", "2026-05-10.log"), path)
    }
}
