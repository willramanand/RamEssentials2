package dev.willram.ramEssentials2.data

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.data.DataKeyCodec
import dev.willram.ramcore.data.FileDataRepository
import dev.willram.ramcore.data.GsonDataSerializer
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.scheduler.Schedulers
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class PlayerDataRepository(private val plugin: RamEssentials2) : FileDataRepository<UUID, PlayerData>(
    plugin.dataFolder.toPath().resolve("playerdata"),
    DataKeyCodec.uuidKeys(),
    GsonDataSerializer.pretty(PlayerData::class.java),
    Schedulers.async()
) {
    override fun setup() {
        migrateTo(2) { data, _ ->
            if (data.backStack.isEmpty() && data.lastLocation != null) {
                data.backStack.add(data.lastLocation!!.clone())
            }
            data
        }
        super.setup()

        plugin.bind(Events.subscribe(PlayerLoginEvent::class.java, EventPriority.HIGH)
            .filter { e -> e.result == PlayerLoginEvent.Result.ALLOWED }
            .handler { e ->
                val data = this.get(e.player.uniqueId) ?: PlayerData().also {
                    it.markDirty()
                    add(e.player.uniqueId, it)
                    queueSave(e.player.uniqueId)
                }

                Schedulers.runLater(e.player, {
                    data.nickname?.let { nickname ->
                        e.player.displayName(MiniMessage.miniMessage().deserialize(nickname))
                        e.player.sendRichMessage(plugin.conf.message("login-nickname", mapOf("nickname" to nickname)))
                    }

                    val isOp = e.player.isOp
                    if (data.godMode) {
                        if (!isOp) {
                            data.godMode = false
                            data.markDirty()
                            e.player.sendRichMessage(plugin.conf.message("login-god-disabled"))
                        } else {
                            e.player.sendRichMessage(plugin.conf.message("login-god-enabled"))
                        }
                    }

                    if (e.player.isFlying) {
                        if (!isOp) {
                            e.player.allowFlight = false
                            e.player.isFlying = false
                            e.player.sendRichMessage(plugin.conf.message("login-fly-disabled"))
                        } else {
                            e.player.sendRichMessage(plugin.conf.message("login-fly-enabled"))
                        }
                    }

                    if (e.player.gameMode != GameMode.SURVIVAL) {
                        if (!isOp) {
                            e.player.gameMode = GameMode.SURVIVAL
                        }
                        e.player.sendRichMessage(plugin.conf.message("login-gamemode", mapOf("gamemode" to e.player.gameMode.name)))
                    }
                }, 20L, plugin)
            })

        plugin.bind(Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGH)
            .handler { e ->
                if (this.get(e.player.uniqueId)?.dirty() == true) {
                    this.save(e.player.uniqueId)
                }
                this.remove(e.player.uniqueId)
            })
    }
}
