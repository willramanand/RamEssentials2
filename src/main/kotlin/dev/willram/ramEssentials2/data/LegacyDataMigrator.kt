package dev.willram.ramEssentials2.data

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramcore.config.Configs
import dev.willram.ramcore.libs.configurate.objectmapping.ConfigSerializable
import dev.willram.ramcore.libs.configurate.hocon.HoconConfigurationLoader
import io.leangen.geantyref.TypeToken
import org.bukkit.Location
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyDataMigrator(private val plugin: RamEssentials2) {
    private val marker = plugin.dataFolder.toPath().resolve("migration-2.0.done")

    fun migrateIfNeeded(): MigrationReport {
        if (Files.exists(marker)) {
            return MigrationReport(markerExisted = true)
        }

        val backup = createBackup()
        val configFiles = migrateConfig(backup)
        val accounts = migrateAccounts(backup)
        val warps = migrateWarps(backup)
        val players = migratePlayers(backup)
        val report = MigrationReport(
            markerExisted = false,
            backupPath = backup?.toString(),
            configFiles = configFiles,
            accounts = accounts,
            warps = warps,
            players = players
        )
        if (report.migrated) {
            plugin.saveEssentials()
        }

        Files.createDirectories(marker.parent)
        Files.writeString(marker, report.toMarkerText())
        return report
    }

    private fun migrateConfig(backup: Path?): Int {
        val file = plugin.dataFolder.toPath().resolve("config.conf")
        if (Files.notExists(file)) {
            return 0
        }

        backup(file, backup)
        val legacy = loader(file).load().get(LegacyConfig::class.java) ?: return 0
        plugin.conf.maxHomes = legacy.maxHomes
        plugin.conf.percentLostOnDeath = legacy.percentLostOnDeath
        plugin.conf.commandsPerPage = legacy.commandsPerPage
        plugin.conf.teleportDelay = legacy.teleportDelay
        plugin.conf.serverSpawn = legacy.serverSpawn ?: plugin.conf.serverSpawn
        plugin.conf.chatMessageFormat = legacy.chatMessageFormat
        plugin.conf.save()
        archive(file)
        return 1
    }

    private fun migrateAccounts(backup: Path?): Int {
        val file = plugin.dataFolder.toPath().resolve("accounts.conf")
        if (Files.notExists(file)) {
            return 0
        }

        backup(file, backup)
        val type = object : TypeToken<Map<UUID, LegacyAccountData>>() {}.type
        val accounts = loader(file).load().get(type) as? Map<UUID, LegacyAccountData> ?: emptyMap()
        accounts.forEach { (id, legacy) ->
            if (!plugin.accounts.has(id)) {
                val account = AccountData()
                account.playerName = legacy.playerName
                account.capital = legacy.capital
                account.dataVersion(2)
                account.markDirty()
                plugin.accounts.add(id, account)
            }
        }
        plugin.accounts.saveDirty()
        archive(file)
        return accounts.size
    }

    private fun migrateWarps(backup: Path?): Int {
        val file = plugin.dataFolder.toPath().resolve("warps.conf")
        if (Files.notExists(file)) {
            return 0
        }

        backup(file, backup)
        val type = object : TypeToken<Map<String, Location>>() {}.type
        val warps = loader(file).load().get(type) as? Map<String, Location> ?: emptyMap()
        warps.forEach { (name, location) ->
            if (plugin.warps.getIgnoreCase(name) == null) {
                val warp = WarpData()
                warp.name = name
                warp.displayName = name
                warp.location = location
                warp.dataVersion(2)
                warp.markDirty()
                plugin.warps.add(name, warp)
            }
        }
        plugin.warps.saveDirty()
        archive(file)
        return warps.size
    }

    private fun migratePlayers(backup: Path?): Int {
        val directory = plugin.dataFolder.toPath().resolve("playerdata")
        if (!Files.isDirectory(directory)) {
            return 0
        }

        var migrated = 0
        Files.list(directory).use { stream ->
            stream.filter { path -> path.fileName.toString().endsWith(".conf") }.forEach { path ->
                val id = runCatching { UUID.fromString(path.fileName.toString().removeSuffix(".conf")) }.getOrNull()
                    ?: return@forEach
                backup(path, backup)
                val legacy = loader(path).load().get(LegacyPlayerData::class.java) ?: return@forEach
                if (!plugin.players.has(id)) {
                    val data = PlayerData()
                    data.godMode = legacy.godMode
                    data.doNotDisturb = legacy.doNotDisturb
                    data.muted = legacy.muted
                    data.muteReason = legacy.muteReason
                    data.lastLocation = legacy.lastLocation
                    data.backStack = legacy.lastLocation?.let { mutableListOf(it) } ?: mutableListOf()
                    data.homes.putAll(legacy.homes)
                    data.ignoredPlayers = legacy.ignoredPlayers
                    data.lastReceivedPlayer = legacy.lastReceivedPlayer
                    data.nickname = legacy.nickname
                    data.dataVersion(2)
                    data.markDirty()
                    plugin.players.add(id, data)
                    migrated++
                }
                archive(path)
            }
        }
        plugin.players.saveDirty()
        return migrated
    }

    private fun loader(path: Path): HoconConfigurationLoader {
        return HoconConfigurationLoader.builder()
            .path(path)
            .defaultOptions { opts -> opts.serializers { build -> build.registerAll(Configs.typeSerializers()) } }
            .build()
    }

    private fun archive(path: Path) {
        var archive = path.resolveSibling(path.fileName.toString() + ".legacy")
        if (Files.exists(archive)) {
            archive = path.resolveSibling(path.fileName.toString() + ".legacy.${System.currentTimeMillis()}")
        }
        Files.move(path, archive, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun createBackup(): Path? {
        val files = listOf(
            plugin.dataFolder.toPath().resolve("config.conf"),
            plugin.dataFolder.toPath().resolve("accounts.conf"),
            plugin.dataFolder.toPath().resolve("warps.conf")
        )
        val hasLegacyFiles = files.any { Files.exists(it) } ||
            Files.isDirectory(plugin.dataFolder.toPath().resolve("playerdata")) &&
            Files.list(plugin.dataFolder.toPath().resolve("playerdata")).use { stream ->
                stream.anyMatch { it.fileName.toString().endsWith(".conf") }
            }
        if (!hasLegacyFiles) {
            return null
        }

        val backup = plugin.dataFolder.toPath().resolve("legacy-backup-${DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-')}")
        Files.createDirectories(backup)
        return backup
    }

    private fun backup(path: Path, backupRoot: Path?) {
        if (backupRoot == null || Files.notExists(path)) {
            return
        }
        val relative = plugin.dataFolder.toPath().relativize(path)
        val target = backupRoot.resolve(relative)
        Files.createDirectories(target.parent)
        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
    }

    @ConfigSerializable
    private class LegacyConfig {
        var maxHomes = 3
        var percentLostOnDeath = 10
        var commandsPerPage = 10
        var teleportDelay = 5
        var serverSpawn: Location? = null
        var chatMessageFormat = "<dark_gray>[</dark_gray><white><playername></white><dark_gray>]</dark_gray>"
    }

    @ConfigSerializable
    private class LegacyAccountData {
        var playerName = ""
        var capital = 0.0
    }

    @ConfigSerializable
    private class LegacyPlayerData {
        var godMode = false
        var doNotDisturb = false
        var muted = false
        var muteReason: String? = null
        var lastLocation: Location? = null
        val homes: MutableMap<String, Location> = HashMap()
        var ignoredPlayers: List<UUID> = mutableListOf()
        var lastReceivedPlayer: UUID? = null
        var nickname: String? = null
    }
}

data class MigrationReport(
    val markerExisted: Boolean,
    val backupPath: String? = null,
    val configFiles: Int = 0,
    val accounts: Int = 0,
    val warps: Int = 0,
    val players: Int = 0
) {
    val migrated: Boolean
        get() = configFiles + accounts + warps + players > 0

    fun toMarkerText(): String {
        return """
            Migrated legacy RamEssentials data to RamCore 2.0 repositories.
            backup=$backupPath
            configFiles=$configFiles
            accounts=$accounts
            warps=$warps
            players=$players
        """.trimIndent() + "\n"
    }
}
