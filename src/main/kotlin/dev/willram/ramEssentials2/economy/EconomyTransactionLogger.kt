package dev.willram.ramEssentials2.economy

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.scheduler.Schedulers
import org.bukkit.Bukkit
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.UUID

class EconomyTransactionLogger(private val plugin: RamEssentials2) {
    fun log(action: String, actor: UUID?, target: UUID?, amount: Double, balance: Double, note: String = "") {
        if (!plugin.conf.transactionLogging) {
            return
        }

        val now = Instant.now()
        val actorName = actor?.let { Bukkit.getOfflinePlayer(it).name ?: it.toString() } ?: "system"
        val targetName = target?.let { Bukkit.getOfflinePlayer(it).name ?: it.toString() } ?: "-"
        val line = "$now action=$action actor=$actorName target=$targetName amount=$amount balance=$balance note=\"$note\""

        Schedulers.runAsync {
            synchronized(this) {
                val path = TransactionLogFilePolicy.path(plugin.dataFolder.toPath(), now)
                Files.createDirectories(path.parent)
                Files.writeString(
                    path,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                )
            }
        }
    }
}
