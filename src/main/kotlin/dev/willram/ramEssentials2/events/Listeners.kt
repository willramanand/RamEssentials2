package dev.willram.ramEssentials2.events

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.utils.Formatter
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class Listeners {

    companion object {
        fun register(plugin: RamEssentials2) {
            plugin.bind(Events.subscribe(AsyncChatEvent::class.java, EventPriority.HIGHEST)
                .handler { e ->
                    e.renderer { source, sourceDisplayName, message, viewer ->
                        MiniMessage.miniMessage()
                            .deserialize("${RamEssentials2.get().conf.chatMessageFormat} <message>",
                                Placeholder.component("playername", sourceDisplayName),
                                Placeholder.component("message", message))
                    }
                })

            plugin.bind(Events.subscribe(EntityDamageEvent::class.java, EventPriority.HIGHEST)
                .handler { e ->
                    if (e.entity !is Player) return@handler
                    val data = RamEssentials2.get().players.get((e.entity as Player).uniqueId)
                    if (data == null || !data.godMode) return@handler
                    e.isCancelled = true
                })

            plugin.bind(Events.subscribe(PlayerDeathEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    val data = RamEssentials2.get().players.require(e.player.uniqueId)
                    data.pushBackLocation(e.player.location, RamEssentials2.get().conf.backStackSize)

                    val basePercentage = RamEssentials2.get().conf.percentLostOnDeath
                    val account = RamEssentials2.get().accounts.require(e.player.uniqueId)
                    val percentage = basePercentage / 100.0
                    val subtractAmount = account.capital * percentage
                    account.withdraw(subtractAmount)
                    account.markDirty()
                    if (RamEssentials2.get().transactionsReady()) {
                        RamEssentials2.get().transactions.log("death-loss", e.player.uniqueId, null, subtractAmount, account.capital)
                    }

                    if (e.entity.killer == null) {
                        e.player.sendRichMessage(RamEssentials2.get().conf.message("death-loss", mapOf("percent" to basePercentage.toString())))
                        return@handler
                    }

                    val killer = e.entity.killer
                    if (killer !is Player) return@handler
                    val other = RamEssentials2.get().accounts.require(killer.uniqueId)
                    other.deposit(subtractAmount)
                    other.markDirty()
                    if (RamEssentials2.get().transactionsReady()) {
                        RamEssentials2.get().transactions.log("death-robbery", e.player.uniqueId, killer.uniqueId, subtractAmount, other.capital)
                    }
                    e.player.sendRichMessage(
                        RamEssentials2.get().conf.message(
                            "death-robbed",
                            mapOf("amount" to Formatter.formatMoney(subtractAmount), "player" to killer.name)
                        )
                    )
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                        RamEssentials2.get().conf.message("death-robbery-broadcast", mapOf("player" to e.player.name))
                    ))
                })

            plugin.bind(Events.subscribe(PlayerMoveEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (e.isCancelled) return@handler
                    if (!e.hasChangedBlock()) {
                        return@handler
                    }
                    if (TeleportUtil.hasTPTask(e.player)) {
                        TeleportUtil.clearTPTask(e.player)
                    }
                })

            plugin.bind(Events.subscribe(EntityDamageEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (e.isCancelled) return@handler
                    if (e.entity !is Player) return@handler
                    if (TeleportUtil.hasTPTask(e.entity as Player)) {
                        TeleportUtil.clearTPTask(e.entity as Player)
                    }
                })

            plugin.bind(Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (TeleportUtil.hasTPTask(e.player)) {
                        TeleportUtil.clearTPTask(e.player, false)
                    }
                })
        }
    }
}
