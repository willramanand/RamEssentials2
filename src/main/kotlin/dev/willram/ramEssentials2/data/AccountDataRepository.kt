package dev.willram.ramEssentials2.data

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.data.DataKeyCodec
import dev.willram.ramcore.data.FileDataRepository
import dev.willram.ramcore.data.GsonDataSerializer
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.scheduler.Schedulers
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent
import java.util.UUID

class AccountDataRepository(private val plugin: RamEssentials2) : FileDataRepository<UUID, AccountData>(
    plugin.dataFolder.toPath().resolve("accounts"),
    DataKeyCodec.uuidKeys(),
    GsonDataSerializer.pretty(AccountData::class.java),
    Schedulers.async()
) {
    override fun setup() {
        super.setup()

        plugin.bind(Events.subscribe(PlayerLoginEvent::class.java, EventPriority.HIGH)
            .filter { e -> e.result == PlayerLoginEvent.Result.ALLOWED }
            .handler { e ->
                val account = this.get(e.player.uniqueId) ?: AccountData().also {
                    it.playerName = e.player.name
                    it.markDirty()
                    this.add(e.player.uniqueId, it)
                    this.queueSave(e.player.uniqueId)
                }

                if (account.playerName != e.player.name) {
                    account.playerName = e.player.name
                    account.markDirty()
                }
            })
    }
}
