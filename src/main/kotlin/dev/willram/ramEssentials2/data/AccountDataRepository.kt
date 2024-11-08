package dev.willram.ramEssentials2.data

import com.google.common.collect.BiMap
import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.data.serializers.AccountDataSerializer
import dev.willram.ramcore.config.Configs
import dev.willram.ramcore.configurate.hocon.HoconConfigurationLoader
import dev.willram.ramcore.data.DataRepository
import dev.willram.ramcore.event.Events
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.Task
import io.leangen.geantyref.TypeToken
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent
import java.nio.file.Path
import java.util.*

class AccountDataRepository(private val plugin: RamEssentials2) : DataRepository<UUID, AccountData>() {
    override fun setup() {
        val loader = this.file()
        val node = loader.load()
        val map = node.get(object : TypeToken<Map<UUID, AccountData>>() {}.type) as Map<UUID, AccountData>
        this.registry = map
        node.set(object : TypeToken<Map<UUID, AccountData>>() {}.type, this.registry());
        loader.save(node)

        Events.subscribe(PlayerLoginEvent::class.java, EventPriority.HIGH)
            .filter { e -> e.result == PlayerLoginEvent.Result.ALLOWED }
            .handler { e ->
                run {
                    if (!this.registry().containsKey(e.player.uniqueId)) {
                        val account = AccountData()
                        account.playerName = e.player.name
                        this.add(e.player.uniqueId, account)
                    }
                }
            }

    }

    override fun saveAll() {
        val loader = this.file()
        val node = loader.load()
        node.set(object : TypeToken<Map<UUID, AccountData>>() {}.type, this.registry());
        loader.save(node);
    }

    private fun file(): HoconConfigurationLoader {
        return HoconConfigurationLoader.builder()
            .path(Path.of("${plugin.dataFolder}/accounts.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers()).register(AccountData::class.java, AccountDataSerializer.INSTANCE)}}
            .build()
    }
}