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

class Listeners {

    companion object {
        fun register() {
            Events.subscribe(AsyncChatEvent::class.java, EventPriority.HIGHEST)
                .handler { e ->
                    e.renderer { source, sourceDisplayName, message, viewer ->
                        MiniMessage.miniMessage()
                            .deserialize("${RamEssentials2.get().conf.chatMessageFormat} <message>",
                                Placeholder.component("playername", sourceDisplayName),
                                Placeholder.component("message", message))
                    }
                }

            Events.subscribe(EntityDamageEvent::class.java, EventPriority.HIGHEST)
                .handler { e ->
                    if (e.entity !is Player) return@handler
                    val data = RamEssentials2.get().players.get((e.entity as Player).uniqueId)
                    if (!data.godMode) return@handler
                    e.isCancelled = true
                }

            Events.subscribe(PlayerDeathEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (e.isCancelled) return@handler
                    val data = RamEssentials2.get().players.get(e.player.uniqueId)
                    data.lastLocation = e.player.location


                    val basePercentage = RamEssentials2.get().conf.percentLostOnDeath
                    val account = RamEssentials2.get().accounts.get(e.player.uniqueId)
                    val percentage = basePercentage / 100.0
                    val subtractAmount = account.capital * percentage
                    e.player.sendMessage(subtractAmount.toString())
                    account.withdraw(subtractAmount)

                    if (e.entity.killer == null) {
                        e.player.sendRichMessage("<yellow>You have lost <light_purple>$basePercentage% <yellow>of your net worth!");
                        return@handler
                    }

                    val killer = e.entity.killer
                    if (killer !is Player) return@handler
                    val other = RamEssentials2.get().accounts.get(killer.uniqueId)
                    other.deposit(subtractAmount)
                    e.player.sendRichMessage("<yellow>You have been robbed of <light_purple>${Formatter.formatMoney(subtractAmount)} <yellow>by <light_purple>${killer.name}<yellow>!");
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<light_purple>${e.player.name} <yellow>has been robbed!"))
                }

            Events.subscribe(PlayerMoveEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (e.isCancelled) return@handler
                    if (TeleportUtil.hasTPTask(e.player)) {
                        TeleportUtil.clearTPTask(e.player);
                        e.player.sendRichMessage("<red>Teleport was cancelled!");
                    }
                }

            Events.subscribe(EntityDamageEvent::class.java, EventPriority.HIGH)
                .handler { e ->
                    if (e.isCancelled) return@handler
                    if (e.entity !is Player) return@handler
                    if (TeleportUtil.hasTPTask(e.entity as Player)) {
                        TeleportUtil.clearTPTask(e.entity as Player);
                        (e.entity as Player).sendRichMessage("<red>Teleport was cancelled!");
                    }
                }
        }
    }
}