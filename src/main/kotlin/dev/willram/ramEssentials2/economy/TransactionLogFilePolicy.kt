package dev.willram.ramEssentials2.economy

import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TransactionLogFilePolicy {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    fun path(dataFolder: Path, instant: Instant): Path {
        return dataFolder.resolve("transactions").resolve("${formatter.format(instant)}.log")
    }
}
