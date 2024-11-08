package dev.willram.ramEssentials2

import dev.willram.ramEssentials2.commands.*
import dev.willram.ramEssentials2.commands.nickname.NicknameRootCommand
import dev.willram.ramEssentials2.config.EssentialsConfig
import dev.willram.ramEssentials2.data.AccountDataRepository
import dev.willram.ramEssentials2.data.PlayerDataRepository
import dev.willram.ramEssentials2.economy.RamEssentials2Economy
import dev.willram.ramEssentials2.events.Listeners
import dev.willram.ramcore.RamPlugin
import dev.willram.ramcore.config.Configs
import dev.willram.ramcore.configurate.hocon.HoconConfigurationLoader
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.Task
import io.leangen.geantyref.TypeToken
import io.papermc.paper.command.brigadier.Commands
import net.milkbowl.vault.economy.Economy
import org.bukkit.Location
import org.bukkit.plugin.ServicePriority
import java.nio.file.Path
import java.util.*


class RamEssentials2 : RamPlugin() {

    private lateinit var autoSaveTask: Task;
    lateinit var conf: EssentialsConfig
    lateinit var players: PlayerDataRepository
    lateinit var accounts: AccountDataRepository
    var warps: MutableMap<String, Location> = HashMap()

    companion object {
        private lateinit var i: RamEssentials2;

        fun get(): RamEssentials2 {
            return i;
        }
    }

    init {
        i = this
    }

    override fun enable() {
        this.loadConf()
        this.loadWarps()

        players = PlayerDataRepository(this)
        players.setup()

        accounts = AccountDataRepository(this)
        accounts.setup()

        Listeners.register()

        this.autoSaveTask = Schedulers.async().runRepeating({ _: Task ->
            this.saveConf()
            this.saveWarps()
            players.saveAll()
            accounts.saveAll()
        }, 6000L, 6000L)
    }

    override fun disable() {
        this.autoSaveTask.stop()

        this.saveConf()
        this.saveWarps()
        players.saveAll()
        accounts.saveAll()
    }

    override fun load() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy")
            server.servicesManager.register(
                Economy::class.java,
                RamEssentials2Economy(this),
                this,
                ServicePriority.Highest
            )
            log("<yellow>Vault found: <green>INTEGRATION ENABLED.")
        } catch (ignored: ClassNotFoundException) {
        }
    }

    @Suppress("UnstableApiUsage")
    override fun registerCommands(commands: Commands) {
        commands.register("back", "Return a previous location", listOf(), BackCommand(this))
        commands.register("broadcast", "Broadcast a message to the whole server", listOf(), BroadcastCommand())
        commands.register("balance", "View a players balance", listOf("bal", "money", "wallet"), BalanceCommand(this))
        commands.register("god", "Set player god mode status", listOf(), GodCommand(this))
        commands.register("donotdisturb", "Toggle your do not disturb status.", listOf("dnd"), DoNotDisturbCommand(this))
        commands.register("home", "Teleport to player home", listOf(), HomeCommand(this))
        commands.register("delhome", "Delete a player home", listOf("homedel"), DelHomeCommand(this))
        commands.register("sethome", "Set a new player home", listOf("homeset"), SetHomeCommand(this))
        commands.register("spawn", "Teleport to server or world spawn", listOf(), SpawnCommand(this))
        commands.register("setspawn", "Set the spawn of the server or a world", listOf(), SetSpawnCommand(this))
        commands.register("gamemode", "Set the gamemode of a player.", listOf("gm"), GamemodeCommand())
        commands.register("nickname", "Set your nickname", listOf("nick"), NicknameRootCommand(this))
        commands.register("message", "Send message to another player.", listOf("msg"), MessageCommand(this))
        commands.register("reply", "Reply to a player who recently sent you a message.", listOf("r"), ReplyCommand(this))
        commands.register("ignore", "Ignore another player.", listOf(), IgnoreCommand(this))
        commands.register("world", "Teleport to spawn of another world.", listOf(), WorldCommand())
        commands.register("warp", "Teleport to a warp.", listOf(), WarpCommand(this))
        commands.register("delwarp", "Delete a warp.", listOf("warpdel"), DelWarpCommand(this))
        commands.register("setwarp", "Set a warp.", listOf("warpset"), SetWarpCommand(this))
    }

    private fun loadConf() {
        val loader = HoconConfigurationLoader.builder()
            .path(Path.of("${this.dataFolder}/config.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers())}}
            .build()
        val node = loader.load(); // Load from file
        this.conf = node.get(EssentialsConfig::class.java)!!
        loader.save(node)
    }

    private fun saveConf() {
        val loader = HoconConfigurationLoader.builder()
            .path(Path.of("${this.dataFolder}/config.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers())}}
            .build()
        val node = loader.load(); // Load from file
        node.set(EssentialsConfig::class.java, conf)
        loader.save(node)
    }

    private fun loadWarps() {
        val loader = HoconConfigurationLoader.builder()
            .path(Path.of("${this.dataFolder}/warps.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers())}}
            .build()
        val node = loader.load(); // Load from file
        this.warps = node.get(object : TypeToken<MutableMap<String, Location>>() {}.type) as MutableMap<String, Location>
        loader.save(node)
    }

    private fun saveWarps() {
        val loader = HoconConfigurationLoader.builder()
            .path(Path.of("${this.dataFolder}/warps.conf"))
            .defaultOptions {opts -> opts.serializers {build -> build.registerAll(Configs.typeSerializers())}}
            .build()
        val node = loader.load(); // Load from file
        node.set(object : TypeToken<MutableMap<String, Location>>() {}.type, warps)
        loader.save(node)
    }

}
