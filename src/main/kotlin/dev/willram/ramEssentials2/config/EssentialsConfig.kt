package dev.willram.ramEssentials2.config

import dev.willram.ramcore.config.BukkitConfig
import dev.willram.ramcore.config.ConfigKey
import org.bukkit.Location
import org.bukkit.entity.Player
import java.nio.file.Path

class EssentialsConfig(private val path: Path, defaultSpawn: Location) {
    private val configVersionKey = ConfigKey.of("config-version", Int::class.javaObjectType, 2)
    private val maxHomesKey = ConfigKey.of("homes.default-limit", Int::class.javaObjectType, 3)
        .validate({ value -> value >= 0 }, "must be >= 0")
    private val percentLostOnDeathKey = ConfigKey.of("economy.percent-lost-on-death", Int::class.javaObjectType, 10)
        .validate({ value -> value in 0..100 }, "must be between 0 and 100")
    private val commandsPerPageKey = ConfigKey.of("commands-per-page", Int::class.javaObjectType, 10)
        .validate({ value -> value > 0 }, "must be > 0")
    private val teleportWarmupKey = ConfigKey.of("teleport.warmup-seconds", Int::class.javaObjectType, 5)
        .validate({ value -> value >= 0 }, "must be >= 0")
    private val teleportSafeKey = ConfigKey.of("teleport.safe-location-search", Boolean::class.javaObjectType, true)
    private val teleportLoadChunksKey = ConfigKey.of("teleport.load-target-chunk", Boolean::class.javaObjectType, true)
    private val teleportRequestTimeoutKey = ConfigKey.of("teleport.request-timeout-seconds", Int::class.javaObjectType, 60)
        .validate({ value -> value > 0 }, "must be > 0")
    private val backStackSizeKey = ConfigKey.of("teleport.back-stack-size", Int::class.javaObjectType, 5)
        .validate({ value -> value >= 1 }, "must be >= 1")
    private val serverSpawnKey = ConfigKey.of("teleport.server-spawn", Location::class.java, defaultSpawn)
    private val chatMessageFormatKey = ConfigKey.of(
        "chat.message-format",
        String::class.java,
        "<dark_gray>[</dark_gray><white><playername></white><dark_gray>]</dark_gray>"
    )
    private val transactionLoggingKey = ConfigKey.of("economy.transaction-logging", Boolean::class.javaObjectType, true)

    private val keys = listOf(
        configVersionKey,
        maxHomesKey,
        percentLostOnDeathKey,
        commandsPerPageKey,
        teleportWarmupKey,
        teleportSafeKey,
        teleportLoadChunksKey,
        teleportRequestTimeoutKey,
        backStackSizeKey,
        serverSpawnKey,
        chatMessageFormatKey,
        transactionLoggingKey
    )

    private var config = BukkitConfig.load(path, keys)

    init {
        applyStructuredDefaults()
        save()
    }

    var maxHomes: Int
        get() = config.get(maxHomesKey)
        set(value) = set(maxHomesKey, value)

    var percentLostOnDeath: Int
        get() = config.get(percentLostOnDeathKey)
        set(value) = set(percentLostOnDeathKey, value)

    var commandsPerPage: Int
        get() = config.get(commandsPerPageKey)
        set(value) = set(commandsPerPageKey, value)

    var teleportDelay: Int
        get() = config.get(teleportWarmupKey)
        set(value) = set(teleportWarmupKey, value)

    var serverSpawn: Location
        get() = config.get(serverSpawnKey)
        set(value) = set(serverSpawnKey, value)

    var chatMessageFormat: String
        get() = config.get(chatMessageFormatKey)
        set(value) = set(chatMessageFormatKey, value)

    val safeTeleportSearch: Boolean
        get() = config.get(teleportSafeKey)

    val loadTargetChunk: Boolean
        get() = config.get(teleportLoadChunksKey)

    val teleportRequestTimeoutSeconds: Int
        get() = config.get(teleportRequestTimeoutKey)

    val backStackSize: Int
        get() = config.get(backStackSizeKey)

    val transactionLogging: Boolean
        get() = config.get(transactionLoggingKey)

    fun reload() {
        config.reload()
        applyStructuredDefaults()
        save()
    }

    fun cooldownSeconds(command: String): Int {
        return config.raw().getInt("cooldowns.$command", 0).coerceAtLeast(0)
    }

    fun maxHomes(player: Player): Int {
        if (player.hasPermission("ramessentials.homes.unlimited")) {
            return Int.MAX_VALUE
        }
        val section = config.raw().getConfigurationSection("homes.permission-limits") ?: return maxHomes
        val permissionLimits = section.getKeys(false).associateWith { section.getInt(it, maxHomes) }
        return resolveMaxHomes(maxHomes, permissionLimits) { permission -> player.hasPermission(permission) }
    }

    fun message(key: String): String {
        return config.raw().getString("messages.$key") ?: DEFAULT_MESSAGES.getValue(key)
    }

    fun message(key: String, placeholders: Map<String, String>): String {
        return renderMessage(message(key), placeholders)
    }

    fun validateRuntime(): List<String> {
        val warnings = mutableListOf<String>()
        config.raw().getConfigurationSection("cooldowns")?.let { section ->
            section.getKeys(false).forEach { key ->
                if (section.getInt(key, 0) < 0) {
                    warnings.add("cooldowns.$key must be >= 0")
                }
            }
        }
        config.raw().getConfigurationSection("homes.permission-limits")?.let { section ->
            section.getKeys(false).forEach { key ->
                if (section.getInt(key, 0) < 0) {
                    warnings.add("homes.permission-limits.$key must be >= 0")
                }
            }
        }
        REQUIRED_MESSAGE_PLACEHOLDERS.forEach { (key, placeholders) ->
            val value = message(key)
            placeholders.forEach { placeholder ->
                if (!value.contains("<$placeholder>")) {
                    warnings.add("messages.$key should include <$placeholder>")
                }
            }
        }
        return warnings
    }

    private fun applyStructuredDefaults() {
        val raw = config.raw()
        DEFAULT_COOLDOWNS.forEach { (key, value) -> raw.addDefault("cooldowns.$key", value) }
        DEFAULT_MESSAGES.forEach { (key, value) -> raw.addDefault("messages.$key", value) }
        raw.addDefault("homes.permission-limits.vip", 5)
        raw.addDefault("homes.permission-limits.staff", 10)
        raw.options().copyDefaults(true)
    }

    private fun <T> set(key: ConfigKey<T>, value: T) {
        config.raw().set(key.path(), value)
        save()
    }

    fun save() {
        config.raw().save(path.toFile())
    }

    companion object {
        fun renderMessage(template: String, placeholders: Map<String, String>): String {
            var value = template
            placeholders.forEach { (placeholder, replacement) ->
                value = value.replace("<$placeholder>", replacement)
            }
            return value
        }

        fun resolveMaxHomes(
            defaultLimit: Int,
            permissionLimits: Map<String, Int>,
            hasPermission: (String) -> Boolean
        ): Int {
            var limit = defaultLimit
            permissionLimits.forEach { (key, value) ->
                if (hasPermission("ramessentials.homes.limit.$key")) {
                    limit = maxOf(limit, value)
                }
            }
            return limit
        }

        private val DEFAULT_COOLDOWNS = mapOf(
            "back" to 5,
            "home" to 5,
            "spawn" to 5,
            "warp" to 5,
            "world" to 5,
            "tpa" to 15,
            "tpahere" to 15,
            "heal" to 30
        )

        private val DEFAULT_MESSAGES = mapOf(
            "cooldown" to "<red>Please wait <white><seconds>s <red>before using that again.",
            "already-teleporting" to "<red>You are already waiting to teleport.",
            "teleport-warmup" to "<yellow>Teleporting in <light_purple><seconds> <yellow>seconds. Do not move or take damage.",
            "teleport-cancelled" to "<red>Teleport was cancelled.",
            "teleport-unsafe" to "<red>No safe teleport location was found.",
            "teleport-complete" to "<yellow>Teleport complete.",
            "tpa-expired" to "<red>Your teleport request expired.",
            "reload" to "<green>RamEssentials configuration reloaded.",
            "save" to "<green>RamEssentials data saved.",
            "diagnostics-header" to "<gold>RamEssentials Diagnostics",
            "diagnostics-players" to "<gray>Players loaded: <white><count>",
            "diagnostics-accounts" to "<gray>Accounts loaded: <white><count>",
            "diagnostics-warps" to "<gray>Warps loaded: <white><count>",
            "diagnostics-pending-teleports" to "<gray>Pending teleports: <white><count>",
            "diagnostics-pending-tpa" to "<gray>Pending TPA requests: <white><count>",
            "diagnostics-warnings" to "<gray>Validation warnings: <white><count>",
            "migration-status-header" to "<gold>Migration Status",
            "migration-status-marker" to "<gray>Marker: <white><status>",
            "migration-status-backup" to "<gray>Backup: <white><path>",
            "migration-status-counts" to "<gray>Migrated config=<config>, accounts=<accounts>, warps=<warps>, players=<players>",
            "broadcast" to "<rainbow>[Broadcast]</rainbow> <white><message>",
            "state-enabled" to "<green>enabled",
            "state-disabled" to "<red>disabled",
            "back-missing" to "<red>You do not have a saved previous location.",
            "back-start" to "<yellow>Returning to saved previous location.",
            "home-missing" to "<red>You do not have a home named <light_purple><home><red>.",
            "home-start" to "<yellow>Teleporting to home <light_purple><home><yellow>.",
            "homes-empty" to "<yellow>You do not have any homes set.",
            "homes-header" to "<gold>Homes <gray>(<count>/<limit>)",
            "homes-line" to "<yellow><homes>",
            "home-exists" to "<red>A home named <light_purple><home> <red>already exists.",
            "home-renamed" to "<yellow>Renamed home <light_purple><old> <yellow>to <light_purple><new><yellow>.",
            "home-deleted" to "<yellow>Removed home <light_purple><home><yellow>.",
            "home-delete-missing" to "<red>A home named <light_purple><home> <red>does not exist.",
            "home-limit" to "<red>You cannot set another home. Your limit is <white><limit><red>.",
            "home-set" to "<yellow>Set home <light_purple><home><yellow> at your current location.",
            "spawn-start" to "<yellow>Teleporting to <light_purple>server spawn<yellow>.",
            "world-spawn-start" to "<yellow>Teleporting to spawn of world <light_purple><world><yellow>.",
            "server-spawn-set" to "<yellow>Set the server spawn.",
            "world-spawn-set" to "<yellow>Set spawn for world <light_purple><world><yellow>.",
            "warp-missing" to "<red>No warp named <light_purple><warp> <red>exists.",
            "warp-no-permission" to "<red>You do not have permission to use this warp.",
            "warp-no-location" to "<red>Warp <light_purple><warp> <red>has no location.",
            "warp-start" to "<yellow>Teleporting to warp <light_purple><warp><yellow>.",
            "warps-empty" to "<yellow>No warps are available.",
            "warps-header" to "<gold>Warps <gray>(<page>/<pages>)",
            "warps-line" to "<light_purple><warp> <gray>[<category>] <dark_gray><created>",
            "warp-deleted" to "<yellow>Deleted warp <light_purple><warp><yellow>.",
            "warp-exists" to "<red>A warp named <light_purple><warp> <red>already exists.",
            "warp-set" to "<yellow>Set warp <light_purple><warp><yellow> at your current location.",
            "warp-permission-set" to "<yellow>Set warp <light_purple><warp> <yellow>permission to <white><permission><yellow>.",
            "warp-category-set" to "<yellow>Set warp <light_purple><warp> <yellow>category to <white><category><yellow>.",
            "warp-icon-invalid" to "<red>Unknown material <white><icon><red>.",
            "warp-icon-set" to "<yellow>Set warp <light_purple><warp> <yellow>icon to <white><icon><yellow>.",
            "warp-info-header" to "<gold>Warp: <light_purple><warp>",
            "warp-info-category" to "<gray>Category: <white><category>",
            "warp-info-icon" to "<gray>Icon: <white><icon>",
            "warp-info-permission" to "<gray>Permission: <white><permission>",
            "warp-info-created" to "<gray>Created by <white><creator> <gray>on <white><created>",
            "account-missing" to "<red>This player does not have an account.",
            "balance-header" to "<gold>Accounts",
            "balance-player" to "<gray>Player: <white><player>",
            "balance-wallet" to "<green>Wallet Balance: <gold><balance>",
            "balance-top-empty" to "<yellow>No accounts have balances yet.",
            "balance-top-header" to "<gold>Balance Top <gray>(<page>/<pages>)",
            "balance-top-line" to "<gray><rank>. <light_purple><player> <gold><balance>",
            "pay-self" to "<red>You cannot pay yourself.",
            "pay-insufficient" to "<red>You do not have that much money.",
            "pay-sent" to "<yellow>Paid <light_purple><amount> <yellow>to <light_purple><player><yellow>.",
            "pay-received" to "<light_purple><player> <yellow>paid you <light_purple><amount><yellow>.",
            "eco-updated" to "<yellow><player>'s balance is now <gold><balance><yellow>.",
            "god-toggled" to "<yellow>God mode set to <state><yellow>.",
            "dnd-toggled" to "<yellow>Do not disturb <state><yellow>.",
            "nickname-set" to "<yellow>Nickname set to: <nickname>",
            "nickname-cleared" to "<yellow>Nickname cleared.",
            "ignore-added" to "<yellow>Added <light_purple><player><yellow> to your ignore list.",
            "ignore-removed" to "<yellow>Removed <light_purple><player><yellow> from your ignore list.",
            "world-start" to "<yellow>Teleporting to world <light_purple><world><yellow>.",
            "flyspeed-set" to "<yellow>Fly speed set to <light_purple><speed><yellow>.",
            "flight-toggled" to "<yellow>Flight for <light_purple><player><yellow> set to <state><yellow>.",
            "player-offline" to "<red>Player <white><player></white> is not online.",
            "gamemode-self" to "<yellow>Your gamemode has been set to <light_purple><gamemode><yellow>.",
            "gamemode-other" to "<yellow>Set <light_purple><player>'s <yellow>gamemode to <light_purple><gamemode><yellow>.",
            "reply-none" to "<red>You have no players who have recently messaged you.",
            "reply-offline" to "<red>That player is not online.",
            "message-self" to "<red>You cannot send a message to yourself.",
            "message-dnd-self" to "<red>You cannot message other players while do not disturb is enabled.",
            "message-ignored-self" to "<red>You cannot message a player you are ignoring.",
            "message-dnd-other" to "<red>This player is do not disturb and cannot be messaged.",
            "message-ignored-other" to "<red>This player is ignoring you.",
            "message-received" to "<light_purple>FROM <gold><player><light_purple>:<white> <message>",
            "message-sent" to "<light_purple>TO <gold><player><light_purple>:<white> <message>",
            "heal-self" to "<yellow>You have been healed.",
            "heal-other" to "<yellow>Healed <light_purple><player><yellow>.",
            "heal-received" to "<yellow>You have been healed.",
            "tpa-self" to "<red>You cannot request to teleport to yourself.",
            "tpa-blocked" to "<red>That player is not accepting teleport requests.",
            "tpa-sent" to "<yellow>Teleport request sent to <light_purple><player><yellow>.",
            "tpa-received" to "<light_purple><player> <yellow>wants to teleport to you.",
            "tpahere-sent" to "<yellow>Teleport-here request sent to <light_purple><player><yellow>.",
            "tpahere-received" to "<light_purple><player> <yellow>wants you to teleport to them.",
            "tpa-none" to "<red>You do not have a pending teleport request.",
            "tpa-requester-offline" to "<red>The requesting player is no longer online.",
            "tpa-accepted" to "<yellow>Accepted teleport request from <light_purple><player><yellow>.",
            "tpa-accepted-other" to "<light_purple><player> <yellow>accepted your teleport request.",
            "tpa-denied" to "<yellow>Denied teleport request.",
            "tpa-denied-other" to "<light_purple><player> <red>denied your teleport request.",
            "death-loss" to "<yellow>You have lost <light_purple><percent>% <yellow>of your net worth!",
            "death-robbed" to "<yellow>You have been robbed of <light_purple><amount> <yellow>by <light_purple><player><yellow>!",
            "death-robbery-broadcast" to "<light_purple><player> <yellow>has been robbed!",
            "login-nickname" to "<yellow>Nickname has been set to: <nickname>",
            "login-god-disabled" to "<yellow>God mode <red>disabled<yellow>.",
            "login-god-enabled" to "<yellow>God mode <green>enabled<yellow>.",
            "login-fly-disabled" to "<yellow>Fly mode <red>disabled<yellow>.",
            "login-fly-enabled" to "<yellow>Fly mode <green>enabled<yellow>.",
            "login-gamemode" to "<yellow>Gamemode set to <light_purple><gamemode><yellow>."
        )

        private val REQUIRED_MESSAGE_PLACEHOLDERS = mapOf(
            "cooldown" to setOf("seconds"),
            "home-missing" to setOf("home"),
            "home-renamed" to setOf("old", "new"),
            "warp-info-created" to setOf("creator", "created"),
            "balance-top-header" to setOf("page", "pages"),
            "balance-top-line" to setOf("rank", "player", "balance"),
            "pay-sent" to setOf("amount", "player"),
            "message-received" to setOf("player", "message"),
            "message-sent" to setOf("player", "message"),
            "death-loss" to setOf("percent"),
            "death-robbed" to setOf("amount", "player")
        )
    }
}
