package dev.willram.ramEssentials2

import dev.willram.ramEssentials2.commands.EssentialsCommandModule
import dev.willram.ramEssentials2.config.EssentialsConfig
import dev.willram.ramEssentials2.data.AccountDataRepository
import dev.willram.ramEssentials2.data.LegacyDataMigrator
import dev.willram.ramEssentials2.data.MigrationReport
import dev.willram.ramEssentials2.data.PlayerDataRepository
import dev.willram.ramEssentials2.data.WarpDataRepository
import dev.willram.ramEssentials2.economy.EconomyTransactionLogger
import dev.willram.ramEssentials2.economy.RamEssentials2Economy
import dev.willram.ramEssentials2.events.Listeners
import dev.willram.ramcore.RamPlugin
import dev.willram.ramcore.commands.RamCommands
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.scheduler.Task
import dev.willram.ramcore.scheduler.TaskContext
import io.papermc.paper.command.brigadier.Commands
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.ServicePriority
import java.nio.file.Files


class RamEssentials2 : RamPlugin() {

    private lateinit var autoSaveTask: Task;
    lateinit var conf: EssentialsConfig
    lateinit var players: PlayerDataRepository
    lateinit var accounts: AccountDataRepository
    lateinit var warps: WarpDataRepository
    lateinit var transactions: EconomyTransactionLogger
    var migrationStatus: MigrationReport = MigrationReport(markerExisted = true)
    var validationWarnings: List<String> = emptyList()

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

        players = PlayerDataRepository(this)
        players.setup()

        accounts = AccountDataRepository(this)
        accounts.setup()

        warps = WarpDataRepository(this)
        warps.setup()

        transactions = EconomyTransactionLogger(this)
        migrationStatus = LegacyDataMigrator(this).migrateIfNeeded()
        refreshValidationWarnings()

        Listeners.register(this)

        this.autoSaveTask = Schedulers.runTimer(TaskContext.async(), Runnable {
            this.saveEssentials()
        }, 6000L, 6000L, this)
    }

    override fun disable() {
        this.saveEssentials()
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
        RamCommands.register(commands, EssentialsCommandModule(this))
    }

    fun reloadEssentials() {
        this.loadConf()
        if (::warps.isInitialized) {
            warps.setup()
        }
        refreshValidationWarnings()
    }

    fun saveEssentials() {
        this.saveConf()
        if (::players.isInitialized) {
            players.saveDirty()
        }
        if (::accounts.isInitialized) {
            accounts.saveDirty()
        }
        if (::warps.isInitialized) {
            warps.saveDirty()
        }
    }

    fun transactionsReady(): Boolean {
        return ::transactions.isInitialized
    }

    fun refreshValidationWarnings() {
        validationWarnings = buildList {
            if (::conf.isInitialized) {
                addAll(conf.validateRuntime())
            }
            if (::warps.isInitialized) {
                addAll(warps.validateItems())
            }
        }
    }

    private fun loadConf() {
        Files.createDirectories(this.dataFolder.toPath())
        val defaultSpawn = server.worlds.first().spawnLocation
        if (::conf.isInitialized) {
            conf.reload()
        } else {
            conf = EssentialsConfig(this.dataFolder.toPath().resolve("config.yml"), defaultSpawn)
        }
    }

    private fun saveConf() {
        if (::conf.isInitialized) {
            conf.save()
        }
    }

}
