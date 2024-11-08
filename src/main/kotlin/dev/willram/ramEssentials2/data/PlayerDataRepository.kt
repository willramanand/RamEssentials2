package dev.willram.ramEssentials2.data

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.config.Configs
import dev.willram.ramcore.configurate.hocon.HoconConfigurationLoader
import dev.willram.ramcore.data.DataRepository
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.Task
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.nio.file.Path
import java.util.*


class PlayerDataRepository(private val plugin: RamEssentials2) : DataRepository<UUID, PlayerData>() {
    override fun setup() {
        Events.subscribe(PlayerLoginEvent::class.java, EventPriority.HIGH)
            .filter { e -> e.result == PlayerLoginEvent.Result.ALLOWED }
            .handler { e ->
                run {
                    val loader = this.file(e.player.uniqueId)
                    val node = loader.load(); // Load from file
                    val data = node.get(PlayerData::class.java, PlayerData());
                    this.add(e.player.uniqueId, data);
                    node.set(PlayerData::class.java, data);
                    loader.save(node);
                    Schedulers.async().runLater({
                        if (data.nickname != null) {
                            e.player.displayName(MiniMessage.miniMessage().deserialize(data.nickname!!))
                            e.player.sendRichMessage("<yellow>Nickname has been set to: ${data.nickname}")
                        }

                        val isOp = e.player.isOp
                        if (data.godMode) {
                            if (!isOp) {
                                data.godMode = false
                                e.player.sendRichMessage("<yellow>God mode <red>disabled<yellow>.")
                            } else {
                                e.player.sendRichMessage("<yellow>God mode <green>enabled<yellow>.")
                            }
                        }

                        if (e.player.isFlying) {
                            if (!isOp) {
                                e.player.allowFlight = false
                                e.player.isFlying = false
                                e.player.sendRichMessage("<yellow>Fly mode <red>disabled<yellow>.")
                            } else {
                                e.player.sendRichMessage("<yellow>Fly mode <green>enabled<yellow>.")
                            }
                        }

                        if (e.player.gameMode != GameMode.SURVIVAL) {
                            if (!isOp) {
                                e.player.gameMode = GameMode.SURVIVAL
                            }
                            e.player.sendRichMessage("<yellow>Gamemode set to <light_purple>${e.player.gameMode.name}<yellow>.")
                        }
                    }, 20L)
                }
            }

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGH)
            .handler { e ->
                run {
                    val data = this.get(e.player.uniqueId)
                    val loader = this.file(e.player.uniqueId)
                    val node = loader.load(); // Load from file
                    node.set(PlayerData::class.java, data);
                    loader.save(node);
                    this.remove(e.player.uniqueId)
                }
            }
    }

    override fun saveAll() {
        for (id in this.registry().keys) {
            val data = this.get(id)
            if (!data.shouldNotSave() || !data.isSaving) {
                data.isSaving = true
                val loader = this.file(id)
                val node = loader.load()
                node.set(PlayerData::class.java, data);
                loader.save(node);
                data.isSaving = false
            }
        }
    }

    private fun file(id: UUID): HoconConfigurationLoader {
        return HoconConfigurationLoader.builder()
            .path(Path.of("${plugin.dataFolder}/playerdata/${id}.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers())}}
            .build()
    }
}