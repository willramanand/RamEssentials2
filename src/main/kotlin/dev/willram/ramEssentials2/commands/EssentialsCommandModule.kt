package dev.willram.ramEssentials2.commands

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.data.AccountData
import dev.willram.ramEssentials2.data.WarpData
import dev.willram.ramEssentials2.utils.TeleportUtil
import dev.willram.ramcore.commands.CommandContext
import dev.willram.ramcore.commands.CommandModule
import dev.willram.ramcore.commands.CommandSpec
import dev.willram.ramcore.commands.CommandSuggestions
import dev.willram.ramcore.commands.RamArguments
import dev.willram.ramcore.commands.RamCommands
import dev.willram.ramcore.scheduler.Schedulers
import dev.willram.ramcore.utils.Formatter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class EssentialsCommandModule(private val plugin: RamEssentials2) : CommandModule {
    private val pendingTeleportRequests = mutableMapOf<UUID, TeleportRequest>()
    private val cooldowns = mutableMapOf<UUID, MutableMap<String, Long>>()

    override fun commands(): Collection<CommandSpec> = listOf(
        back(),
        broadcast(),
        balance(),
        balanceTop(),
        god(),
        doNotDisturb(),
        home(),
        homes(),
        delHome(),
        setHome(),
        spawn(),
        setSpawn(),
        gamemode(),
        nickname(),
        message(),
        reply(),
        ignore(),
        world(),
        warp(),
        warps(),
        delWarp(),
        setWarp(),
        fly(),
        flySpeed(),
        heal(),
        pay(),
        economy(),
        teleportRequest(),
        teleportHere(),
        teleportAccept(),
        teleportDeny(),
        root()
    )

    private fun root(): CommandSpec {
        val spec = RamCommands.command("ramessentials")
            .description("RamEssentials administration commands.")
            .alias("re")
            .permission("ramessentials.admin")
            .withHelp()

        spec.executes { context -> context.usage(spec) }
        spec.literal("reload") { reload ->
            reload.description("Reload configuration and saved warps.")
                .executes { context ->
                    plugin.reloadEssentials()
                    context.reply(msg("reload"))
                }
        }
        spec.literal("save") { save ->
            save.description("Save all RamEssentials data.")
                .executes { context ->
                    plugin.saveEssentials()
                    context.reply(msg("save"))
                }
        }
        spec.literal("diagnostics") { diagnostics ->
            diagnostics.description("Show loaded data counts.")
                .executes { context ->
                    context.reply(msg("diagnostics-header"))
                    context.reply(msg("diagnostics-players", "count" to plugin.players.size().toString()))
                    context.reply(msg("diagnostics-accounts", "count" to plugin.accounts.size().toString()))
                    context.reply(msg("diagnostics-warps", "count" to plugin.warps.size().toString()))
                    context.reply(msg("diagnostics-pending-teleports", "count" to TeleportUtil.pendingCount().toString()))
                    context.reply(msg("diagnostics-pending-tpa", "count" to pendingTeleportRequests.size.toString()))
                    context.reply(msg("diagnostics-warnings", "count" to plugin.validationWarnings.size.toString()))
                }
        }
        spec.literal("migrate") { migrate ->
            migrate.description("Show migration status.")
                .literal("status") { status ->
                    status.description("Show legacy data migration status.")
                        .executes { context ->
                            val report = plugin.migrationStatus
                            context.reply(msg("migration-status-header"))
                            context.reply(msg("migration-status-marker", "status" to if (report.markerExisted) "existing" else "created"))
                            context.reply(msg("migration-status-backup", "path" to (report.backupPath ?: "none")))
                            context.reply(msg("migration-status-counts", "config" to report.configFiles.toString(), "accounts" to report.accounts.toString(), "warps" to report.warps.toString(), "players" to report.players.toString()))
                        }
                }
        }
        return spec
    }

    private fun back(): CommandSpec = RamCommands.command("back")
        .description("Return to your previous location.")
        .permission("ramessentials.back")
        .playerOnly()
        .executes { context ->
            val player = context.requirePlayer()
            checkCooldown(context, player, "back")
            val data = plugin.players.require(player.uniqueId)
            val location = data.popBackLocation() ?: throw context.fail(msg("back-missing"))
            context.reply(msg("back-start"))
            TeleportUtil.teleport(player, location)
        }

    private fun broadcast(): CommandSpec = RamCommands.command("broadcast")
        .description("Broadcast a message to the whole server.")
        .permission("ramessentials.broadcast")
        .argument(RamArguments.greedyString("message")) { message ->
            message.description("Message to broadcast.")
                .executes { context ->
                    val raw = context.get("message", String::class.java)
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg("broadcast", "message" to raw)))
                }
        }

    private fun balance(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val spec = RamCommands.command("balance")
            .description("View a player's balance.")
            .aliases("bal", "money", "wallet")
            .permission("ramessentials.balance")

        spec.executes { context ->
            val player = context.requirePlayer()
            sendBalance(context, player.name, plugin.accounts.require(player.uniqueId))
        }
        spec.argument(playerArg) { arg ->
            arg.suggests(CommandSuggestions.onlinePlayers())
                .description("Player name.")
                .executes { context ->
                    val name = context.get(playerArg)
                    val entry = accountByName(name)
                        ?: throw context.fail(msg("account-missing"))
                    sendBalance(context, entry.first, entry.second)
                }
        }
        return spec
    }

    private fun balanceTop(): CommandSpec {
        val pageArg = RamArguments.integer("page", 1)
        val spec = RamCommands.command("balancetop")
            .description("Show richest accounts.")
            .alias("baltop")
            .permission("ramessentials.balancetop")

        spec.executes { context -> sendBalanceTop(context, 1) }
        spec.argument(pageArg) { page ->
            page.description("Page number.")
                .executes { context -> sendBalanceTop(context, context.get(pageArg)) }
        }
        return spec
    }

    private fun god(): CommandSpec = RamCommands.command("god")
        .description("Toggle god mode.")
        .permission("ramessentials.god")
        .playerOnly()
        .executes { context ->
            val player = context.requirePlayer()
            val data = plugin.players.require(player.uniqueId)
            data.godMode = !data.godMode
            data.markDirty()
            context.reply(msg("god-toggled", "state" to enabledText(data.godMode)))
        }

    private fun doNotDisturb(): CommandSpec = RamCommands.command("donotdisturb")
        .description("Toggle do-not-disturb mode.")
        .alias("dnd")
        .permission("ramessentials.donotdisturb")
        .playerOnly()
        .executes { context ->
            val player = context.requirePlayer()
            val data = plugin.players.require(player.uniqueId)
            data.doNotDisturb = !data.doNotDisturb
            data.markDirty()
            context.reply(msg("dnd-toggled", "state" to enabledText(data.doNotDisturb)))
        }

    private fun home(): CommandSpec {
        val nameArg = RamArguments.word("name")
        val oldNameArg = RamArguments.word("old")
        val newNameArg = RamArguments.word("new")
        val spec = RamCommands.command("home")
            .description("Teleport to a home.")
            .permission("ramessentials.home")
            .playerOnly()

        spec.literal("rename") { rename ->
            rename.description("Rename a home.")
                .permission("ramessentials.home.rename")
                .argument(oldNameArg) { old ->
                    old.suggests(homeSuggestions())
                        .argument(newNameArg) { new ->
                            new.executes { context ->
                                val player = context.requirePlayer()
                                val oldName = context.get(oldNameArg)
                                val newName = context.get(newNameArg)
                                val data = plugin.players.require(player.uniqueId)
                                val location = data.homes.remove(oldName)
                                    ?: throw context.fail(msg("home-missing", "home" to oldName))
                                if (data.homes.containsKey(newName)) {
                                    data.homes[oldName] = location
                                    throw context.fail(msg("home-exists", "home" to newName))
                                }
                                data.homes[newName] = location
                                data.markDirty()
                                context.reply(msg("home-renamed", "old" to oldName, "new" to newName))
                            }
                        }
                }
        }

        spec.argument(nameArg) { name ->
            name.suggests(homeSuggestions())
                .description("Home name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    checkCooldown(context, player, "home")
                    val homeName = context.get(nameArg)
                    val location = plugin.players.require(player.uniqueId).homes[homeName]
                        ?: throw context.fail(msg("home-missing", "home" to homeName))
                    context.reply(msg("home-start", "home" to homeName))
                    TeleportUtil.teleport(player, location)
                }
        }
        return spec
    }

    private fun homes(): CommandSpec = RamCommands.command("homes")
        .description("List your homes.")
        .permission("ramessentials.homes")
        .playerOnly()
        .executes { context ->
            val player = context.requirePlayer()
            val data = plugin.players.require(player.uniqueId)
            if (data.homes.isEmpty()) {
                context.reply(msg("homes-empty"))
                return@executes
            }
            context.reply(msg("homes-header", "count" to data.homes.size.toString(), "limit" to homeLimitText(player)))
            context.reply(msg("homes-line", "homes" to data.homes.keys.sorted().joinToString("<gray>, <light_purple>")))
        }

    private fun delHome(): CommandSpec = RamCommands.command("delhome")
        .description("Delete a home.")
        .alias("homedel")
        .permission("ramessentials.delhome")
        .playerOnly()
        .argument(RamArguments.word("name")) { name ->
            name.suggests(homeSuggestions())
                .description("Home name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    val homeName = context.get("name", String::class.java)
                    val data = plugin.players.require(player.uniqueId)
                    if (data.homes.remove(homeName) == null) {
                        throw context.fail(msg("home-delete-missing", "home" to homeName))
                    }
                    data.markDirty()
                    context.reply(msg("home-deleted", "home" to homeName))
                }
        }

    private fun setHome(): CommandSpec = RamCommands.command("sethome")
        .description("Set a home at your current location.")
        .alias("homeset")
        .permission("ramessentials.sethome")
        .playerOnly()
        .argument(RamArguments.word("name")) { name ->
            name.description("Home name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    val homeName = context.get("name", String::class.java)
                    val data = plugin.players.require(player.uniqueId)
                    val limit = plugin.conf.maxHomes(player)
                    if (!data.homes.containsKey(homeName) && data.homes.size >= limit) {
                        throw context.fail(msg("home-limit", "limit" to homeLimitText(player)))
                    }
                    data.homes[homeName] = player.location
                    data.markDirty()
                    context.reply(msg("home-set", "home" to homeName))
                }
        }

    private fun spawn(): CommandSpec {
        val worldArg = RamArguments.world("world")
        val spec = RamCommands.command("spawn")
            .description("Teleport to the server or world spawn.")
            .permission("ramessentials.spawn")
            .playerOnly()

        spec.executes { context ->
            val player = context.requirePlayer()
            checkCooldown(context, player, "spawn")
            context.reply(msg("spawn-start"))
            TeleportUtil.teleport(player, plugin.conf.serverSpawn)
        }
        spec.argument(worldArg) { arg ->
            arg.suggests(CommandSuggestions.worldsWithEnvironments())
                .description("World name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    checkCooldown(context, player, "spawn")
                    val world = context.get(worldArg)
                    context.reply(msg("world-spawn-start", "world" to world.name))
                    TeleportUtil.teleport(player, world.spawnLocation)
                }
        }
        return spec
    }

    private fun setSpawn(): CommandSpec {
        val worldArg = RamArguments.world("world")
        val spec = RamCommands.command("setspawn")
            .description("Set the server or world spawn.")
            .permission("ramessentials.setspawn")
            .playerOnly()

        spec.executes { context ->
            val player = context.requirePlayer()
            plugin.conf.serverSpawn = player.location
            context.reply(msg("server-spawn-set"))
        }
        spec.argument(worldArg) { arg ->
            arg.suggests(CommandSuggestions.worldsWithEnvironments())
                .description("World name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    val world = context.get(worldArg)
                    world.spawnLocation = player.location
                    context.reply(msg("world-spawn-set", "world" to world.name))
                }
        }
        return spec
    }

    private fun gamemode(): CommandSpec {
        val modeArg = RamArguments.gameMode("mode")
        val playerArg = RamArguments.word("player")
        val spec = RamCommands.command("gamemode")
            .description("Set a gamemode.")
            .alias("gm")
            .permission("ramessentials.gamemode")

        spec.argument(modeArg) { mode ->
            mode.description("Gamemode.")
                .executes { context ->
                    val player = context.requirePlayer()
                    val gameMode = context.get(modeArg)
                    setGameMode(context, player, gameMode, true)
                }
                .argument(playerArg) { player ->
                    player.suggests(CommandSuggestions.onlinePlayers())
                        .description("Player name.")
                        .permission("ramessentials.gamemode.others")
                        .executes { context ->
                            val target = onlinePlayer(context, context.get(playerArg))
                            setGameMode(context, target, context.get(modeArg), false)
                        }
                }
        }
        return spec
    }

    private fun nickname(): CommandSpec {
        val nicknameArg = RamArguments.greedyString("nickname")
        val spec = RamCommands.command("nickname")
            .description("Manage your nickname.")
            .alias("nick")
            .permission("ramessentials.nickname")
            .playerOnly()
            .withHelp()

        spec.executes { context -> context.usage(spec) }
        listOf("set", "s").forEach { literal ->
            spec.literal(literal) { set ->
                set.description("Set your nickname.")
                    .argument(nicknameArg) { nickname ->
                        nickname.description("MiniMessage nickname.")
                            .executes { context ->
                                val player = context.requirePlayer()
                                val raw = context.get(nicknameArg)
                                val data = plugin.players.require(player.uniqueId)
                                data.nickname = raw
                                data.markDirty()
                                player.displayName(MiniMessage.miniMessage().deserialize(raw))
                                context.reply(msg("nickname-set", "nickname" to raw))
                            }
                    }
            }
        }
        listOf("clear", "c").forEach { literal ->
            spec.literal(literal) { clear ->
                clear.description("Clear your nickname.")
                    .executes { context ->
                        val player = context.requirePlayer()
                        val data = plugin.players.require(player.uniqueId)
                        data.nickname = null
                        data.markDirty()
                        player.displayName(player.name())
                        context.reply(msg("nickname-cleared"))
                    }
            }
        }
        return spec
    }

    private fun message(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val messageArg = RamArguments.greedyString("message")
        return RamCommands.command("message")
            .description("Send a private message.")
            .alias("msg")
            .permission("ramessentials.message")
            .playerOnly()
            .argument(playerArg) { player ->
                player.suggests(CommandSuggestions.onlinePlayers())
                    .description("Recipient.")
                    .argument(messageArg) { message ->
                        message.description("Message.")
                            .executes { context ->
                                val sender = context.requirePlayer()
                                val other = onlinePlayer(context, context.get(playerArg))
                                sendPrivateMessage(context, sender, other, context.get(messageArg))
                            }
                    }
            }
    }

    private fun reply(): CommandSpec = RamCommands.command("reply")
        .description("Reply to your last private message.")
        .alias("r")
        .permission("ramessentials.reply")
        .playerOnly()
        .argument(RamArguments.greedyString("message")) { message ->
            message.description("Message.")
                .executes { context ->
                    val sender = context.requirePlayer()
                    val data = plugin.players.require(sender.uniqueId)
                    val otherId = data.lastReceivedPlayer
                        ?: throw context.fail(msg("reply-none"))
                    val other = Bukkit.getPlayer(otherId)
                        ?: throw context.fail(msg("reply-offline"))
                    sendPrivateMessage(context, sender, other, context.get("message", String::class.java))
                }
        }

    private fun ignore(): CommandSpec = RamCommands.command("ignore")
        .description("Toggle ignoring another player.")
        .permission("ramessentials.ignore")
        .playerOnly()
        .argument(RamArguments.word("player")) { player ->
            player.suggests(CommandSuggestions.onlinePlayers())
                .description("Player name.")
                .executes { context ->
                    val sender = context.requirePlayer()
                    val target = onlinePlayer(context, context.get("player", String::class.java))
                    val data = plugin.players.require(sender.uniqueId)
                    if (data.ignoredPlayers.contains(target.uniqueId)) {
                        data.ignoredPlayers = data.ignoredPlayers - target.uniqueId
                        context.reply(msg("ignore-removed", "player" to target.name))
                    } else {
                        data.ignoredPlayers = data.ignoredPlayers + target.uniqueId
                        context.reply(msg("ignore-added", "player" to target.name))
                    }
                    data.markDirty()
                }
        }

    private fun world(): CommandSpec = RamCommands.command("world")
        .description("Teleport to another world's spawn.")
        .permission("ramessentials.world")
        .playerOnly()
        .argument(RamArguments.world("world")) { world ->
            world.suggests(CommandSuggestions.worldsWithEnvironments())
                .description("World name.")
                .executes { context ->
                    val targetWorld = context.get("world", World::class.java)
                    checkCooldown(context, context.requirePlayer(), "world")
                    context.reply(msg("world-start", "world" to targetWorld.name))
                    TeleportUtil.teleport(context.requirePlayer(), targetWorld.spawnLocation)
                }
        }

    private fun warp(): CommandSpec {
        val nameArg = RamArguments.word("name")
        val valueArg = RamArguments.word("value")
        val spec = RamCommands.command("warp")
            .description("Teleport to a warp.")
            .permission("ramessentials.warp")
            .playerOnly()

        spec.literal("info") { info ->
            info.description("Show warp metadata.")
                .permission("ramessentials.warp.info")
                .argument(nameArg) { name ->
                    name.suggests(warpSuggestions())
                        .executes { context ->
                            val warpName = context.get(nameArg)
                            val warp = plugin.warps.getIgnoreCase(warpName)
                                ?: throw context.fail(msg("warp-missing", "warp" to warpName))
                            sendWarpInfo(context, warp)
                        }
                }
        }
        spec.literal("setpermission") { permission ->
            permission.description("Set or clear a warp permission.")
                .permission("ramessentials.warp.admin")
                .argument(nameArg) { name ->
                    name.suggests(warpSuggestions())
                        .argument(valueArg) { value ->
                            value.executes { context ->
                                val warpName = context.get(nameArg)
                                val raw = context.get(valueArg)
                                val warp = plugin.warps.getIgnoreCase(warpName)
                                    ?: throw context.fail(msg("warp-missing", "warp" to warpName))
                                warp.permission = raw.takeUnless { it.equals("none", ignoreCase = true) || it.equals("clear", ignoreCase = true) }
                                warp.markDirty()
                                plugin.warps.queueSave(warp.name)
                                context.reply(msg("warp-permission-set", "warp" to warp.visibleName(), "permission" to (warp.permission ?: "none")))
                            }
                        }
                }
        }
        spec.literal("category") { category ->
            category.description("Set a warp category.")
                .permission("ramessentials.warp.admin")
                .argument(nameArg) { name ->
                    name.suggests(warpSuggestions())
                        .argument(valueArg) { value ->
                            value.executes { context ->
                                val warpName = context.get(nameArg)
                                val warp = plugin.warps.getIgnoreCase(warpName)
                                    ?: throw context.fail(msg("warp-missing", "warp" to warpName))
                                warp.category = context.get(valueArg)
                                warp.markDirty()
                                plugin.warps.queueSave(warp.name)
                                context.reply(msg("warp-category-set", "warp" to warp.visibleName(), "category" to warp.category))
                            }
                        }
                }
        }
        spec.literal("icon") { icon ->
            icon.description("Set a warp icon material.")
                .permission("ramessentials.warp.admin")
                .argument(nameArg) { name ->
                    name.suggests(warpSuggestions())
                        .argument(valueArg) { value ->
                            value.executes { context ->
                                val warpName = context.get(nameArg)
                                val iconName = context.get(valueArg).uppercase()
                                val material = Material.matchMaterial(iconName)
                                    ?: throw context.fail(msg("warp-icon-invalid", "icon" to iconName))
                                val warp = plugin.warps.getIgnoreCase(warpName)
                                    ?: throw context.fail(msg("warp-missing", "warp" to warpName))
                                warp.icon = material.name
                                warp.markDirty()
                                plugin.warps.queueSave(warp.name)
                                context.reply(msg("warp-icon-set", "warp" to warp.visibleName(), "icon" to warp.icon))
                            }
                        }
                }
        }
        spec.argument(nameArg) { name ->
            name.suggests(warpSuggestions())
                .description("Warp name.")
                .executes { context ->
                    val player = context.requirePlayer()
                    checkCooldown(context, player, "warp")
                    val warpName = context.get(nameArg)
                    val warp = plugin.warps.getIgnoreCase(warpName)
                        ?: throw context.fail(msg("warp-missing", "warp" to warpName))
                    val permission = warp.permission
                    if (!warp.visibleTo { player.hasPermission(it) }) {
                        throw context.fail(msg("warp-no-permission"))
                    }
                    val location = warp.location ?: throw context.fail(msg("warp-no-location", "warp" to warpName))
                    context.reply(msg("warp-start", "warp" to warp.visibleName()))
                    TeleportUtil.teleport(player, location)
                }
        }
        return spec
    }

    private fun warps(): CommandSpec {
        val pageArg = RamArguments.integer("page", 1)
        val spec = RamCommands.command("warps")
            .description("List warps.")
            .permission("ramessentials.warps")
            .playerOnly()

        spec.executes { context -> sendWarps(context, 1) }
        spec.argument(pageArg) { page ->
            page.description("Page number.")
                .executes { context -> sendWarps(context, context.get(pageArg)) }
        }
        return spec
    }

    private fun delWarp(): CommandSpec = RamCommands.command("delwarp")
        .description("Delete a warp.")
        .alias("warpdel")
        .permission("ramessentials.delwarp")
        .playerOnly()
        .argument(RamArguments.word("name")) { name ->
            name.suggests(warpSuggestions())
                .description("Warp name.")
                .executes { context ->
                    val warpName = context.get("name", String::class.java)
                    val existingKey = plugin.warps.registry().keys.firstOrNull { it.equals(warpName, ignoreCase = true) }
                    if (existingKey == null) {
                        throw context.fail(msg("warp-missing", "warp" to warpName))
                    }
                    plugin.warps.delete(existingKey)
                    context.reply(msg("warp-deleted", "warp" to warpName))
                }
        }

    private fun setWarp(): CommandSpec = RamCommands.command("setwarp")
        .description("Set a warp at your current location.")
        .alias("warpset")
        .permission("ramessentials.setwarp")
        .playerOnly()
        .argument(RamArguments.word("name")) { name ->
            name.description("Warp name.")
                .executes { context ->
                    val warpName = context.get("name", String::class.java)
                    if (plugin.warps.getIgnoreCase(warpName) != null) {
                        throw context.fail(msg("warp-exists", "warp" to warpName))
                    }
                    val player = context.requirePlayer()
                    val warp = WarpData()
                    warp.name = warpName
                    warp.displayName = warpName
                    warp.location = player.location
                    warp.createdBy = player.uniqueId
                    warp.createdAt = System.currentTimeMillis()
                    warp.markDirty()
                    plugin.warps.add(warpName, warp)
                    plugin.warps.queueSave(warpName)
                    context.reply(msg("warp-set", "warp" to warpName))
                }
        }

    private fun fly(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val spec = RamCommands.command("fly")
            .description("Toggle flight.")
            .permission("ramessentials.fly")
            .playerOnly()

        spec.executes { context -> toggleFlight(context, context.requirePlayer()) }
        spec.argument(playerArg) { player ->
            player.suggests(CommandSuggestions.onlinePlayers())
                .description("Player name.")
                .permission("ramessentials.fly.others")
                .executes { context -> toggleFlight(context, onlinePlayer(context, context.get(playerArg))) }
        }
        return spec
    }

    private fun flySpeed(): CommandSpec {
        val speedArg = RamArguments.floatArg("speed", 0.0f, 10.0f)
        return RamCommands.command("flyspeed")
            .description("Set your fly speed from 0 to 10.")
            .permission("ramessentials.flyspeed")
            .playerOnly()
            .argument(speedArg) { speed ->
                speed.description("Speed from 0 to 10.")
                    .executes { context ->
                        val raw = context.get(speedArg)
                        val normalized = (raw / 10.0f).coerceIn(0.0f, 1.0f)
                        val player = context.requirePlayer()
                        player.flySpeed = normalized
                        context.reply(msg("flyspeed-set", "speed" to raw.toString()))
                    }
            }
    }

    private fun heal(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val spec = RamCommands.command("heal")
            .description("Heal yourself or another player.")
            .permission("ramessentials.heal")
            .playerOnly()

        spec.executes { context ->
            val player = context.requirePlayer()
            checkCooldown(context, player, "heal")
            healPlayer(context, player, true)
        }
        spec.argument(playerArg) { player ->
            player.suggests(CommandSuggestions.onlinePlayers())
                .permission("ramessentials.heal.others")
                .description("Player name.")
                .executes { context ->
                    checkCooldown(context, context.requirePlayer(), "heal")
                    healPlayer(context, onlinePlayer(context, context.get(playerArg)), false)
                }
        }
        return spec
    }

    private fun pay(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val amountArg = RamArguments.doubleArg("amount", 0.01)
        return RamCommands.command("pay")
            .description("Pay another player.")
            .permission("ramessentials.pay")
            .playerOnly()
            .argument(playerArg) { player ->
                player.suggests(CommandSuggestions.onlinePlayers())
                    .description("Recipient.")
                    .argument(amountArg) { amount ->
                        amount.description("Amount.")
                            .executes { context ->
                                val sender = context.requirePlayer()
                                val target = onlinePlayer(context, context.get(playerArg))
                                val value = context.get(amountArg)
                                if (sender.uniqueId == target.uniqueId) {
                                    throw context.fail(msg("pay-self"))
                                }
                                val senderAccount = plugin.accounts.require(sender.uniqueId)
                                if (!senderAccount.has(value)) {
                                    throw context.fail(msg("pay-insufficient"))
                                }
                                val targetAccount = plugin.accounts.require(target.uniqueId)
                                senderAccount.withdraw(value)
                                targetAccount.deposit(value)
                                plugin.transactions.log("pay", sender.uniqueId, target.uniqueId, value, senderAccount.capital)
                                context.reply(msg("pay-sent", "amount" to Formatter.formatMoney(value), "player" to target.name))
                                context.msgOther(target, msg("pay-received", "amount" to Formatter.formatMoney(value), "player" to sender.name))
                            }
                    }
            }
    }

    private fun economy(): CommandSpec {
        val playerArg = RamArguments.word("player")
        val amountArg = RamArguments.doubleArg("amount", 0.0)
        val spec = RamCommands.command("economy")
            .description("Admin economy commands.")
            .aliases("eco", "moneyadmin")
            .permission("ramessentials.economy.admin")
            .withHelp()

        spec.executes { context -> context.usage(spec) }
        spec.literal("give") { give ->
            give.description("Give money to a player.")
                .argument(playerArg) { player ->
                    player.suggests(accountSuggestions())
                        .argument(amountArg) { amount ->
                            amount.executes { context ->
                                modifyAccount(context, context.get(playerArg), context.get(amountArg), "eco-give") { account, value ->
                                    account.deposit(value)
                                }
                            }
                        }
                }
        }
        spec.literal("take") { take ->
            take.description("Take money from a player.")
                .argument(playerArg) { player ->
                    player.suggests(accountSuggestions())
                        .argument(amountArg) { amount ->
                            amount.executes { context ->
                                modifyAccount(context, context.get(playerArg), context.get(amountArg), "eco-take") { account, value ->
                                    account.withdraw(value.coerceAtMost(account.capital))
                                }
                            }
                        }
                }
        }
        spec.literal("set") { set ->
            set.description("Set a player's balance.")
                .argument(playerArg) { player ->
                    player.suggests(accountSuggestions())
                        .argument(amountArg) { amount ->
                            amount.executes { context ->
                                modifyAccount(context, context.get(playerArg), context.get(amountArg), "eco-set") { account, value ->
                                    account.capital = value
                                    account.markDirty()
                                }
                            }
                        }
                }
        }
        spec.literal("reset") { reset ->
            reset.description("Reset a player's balance to zero.")
                .argument(playerArg) { player ->
                    player.suggests(accountSuggestions())
                        .executes { context ->
                            modifyAccount(context, context.get(playerArg), 0.0, "eco-reset") { account, _ ->
                                account.capital = 0.0
                                account.markDirty()
                            }
                        }
                }
        }
        spec.literal("top") { top ->
            top.description("Show richest accounts.")
                .executes { context -> sendBalanceTop(context, 1) }
        }
        return spec
    }

    private fun teleportRequest(): CommandSpec = RamCommands.command("tpa")
        .description("Request to teleport to another player.")
        .permission("ramessentials.tpa")
        .playerOnly()
        .argument(RamArguments.word("player")) { player ->
            player.suggests(CommandSuggestions.onlinePlayers())
                .description("Target player.")
                .executes { context ->
                    val sender = context.requirePlayer()
                    checkCooldown(context, sender, "tpa")
                    val target = onlinePlayer(context, context.get("player", String::class.java))
                    if (sender.uniqueId == target.uniqueId) {
                        throw context.fail(msg("tpa-self"))
                    }
                    val targetData = plugin.players.require(target.uniqueId)
                    if (targetData.doNotDisturb || targetData.ignoredPlayers.contains(sender.uniqueId)) {
                        throw context.fail(msg("tpa-blocked"))
                    }
                    putTeleportRequest(target, TeleportRequest(sender.uniqueId, target.uniqueId, false, expiresAt()))
                    context.reply(msg("tpa-sent", "player" to target.name))
                    sendTeleportRequestMessage(target, msg("tpa-received", "player" to sender.name))
                }
        }

    private fun teleportHere(): CommandSpec = RamCommands.command("tpahere")
        .description("Request another player teleport to you.")
        .permission("ramessentials.tpahere")
        .playerOnly()
        .argument(RamArguments.word("player")) { player ->
            player.suggests(CommandSuggestions.onlinePlayers())
                .description("Target player.")
                .executes { context ->
                    val sender = context.requirePlayer()
                    checkCooldown(context, sender, "tpahere")
                    val target = onlinePlayer(context, context.get("player", String::class.java))
                    if (sender.uniqueId == target.uniqueId) {
                        throw context.fail(msg("tpa-self"))
                    }
                    val targetData = plugin.players.require(target.uniqueId)
                    if (targetData.doNotDisturb || targetData.ignoredPlayers.contains(sender.uniqueId)) {
                        throw context.fail(msg("tpa-blocked"))
                    }
                    putTeleportRequest(target, TeleportRequest(sender.uniqueId, target.uniqueId, true, expiresAt()))
                    context.reply(msg("tpahere-sent", "player" to target.name))
                    sendTeleportRequestMessage(target, msg("tpahere-received", "player" to sender.name))
                }
        }

    private fun teleportAccept(): CommandSpec = RamCommands.command("tpaccept")
        .description("Accept your latest teleport request.")
        .permission("ramessentials.tpaccept")
        .playerOnly()
        .executes { context ->
            val target = context.requirePlayer()
            val request = pendingTeleportRequests.remove(target.uniqueId)
                ?: throw context.fail(msg("tpa-none"))
            if (request.isExpired()) {
                throw context.fail(msg("tpa-expired"))
            }
            val requester = Bukkit.getPlayer(request.requester)
                ?: throw context.fail(msg("tpa-requester-offline"))
            context.reply(msg("tpa-accepted", "player" to requester.name))
            context.msgOther(requester, msg("tpa-accepted-other", "player" to target.name))
            if (request.here) {
                TeleportUtil.teleport(target, requester.location)
            } else {
                TeleportUtil.teleport(requester, target.location)
            }
        }

    private fun teleportDeny(): CommandSpec = RamCommands.command("tpdeny")
        .description("Deny your latest teleport request.")
        .permission("ramessentials.tpdeny")
        .playerOnly()
        .executes { context ->
            val target = context.requirePlayer()
            val request = pendingTeleportRequests.remove(target.uniqueId)
                ?: throw context.fail(msg("tpa-none"))
            val requester = Bukkit.getPlayer(request.requester)
            context.reply(msg("tpa-denied"))
            if (requester != null) {
                context.msgOther(requester, msg("tpa-denied-other", "player" to target.name))
            }
        }

    private fun sendBalance(context: CommandContext, name: String, account: AccountData) {
        context.reply(msg("balance-header"))
        context.reply(msg("balance-player", "player" to name))
        context.reply(msg("balance-wallet", "balance" to Formatter.formatMoney(account.capital)))
    }

    private fun sendBalanceTop(context: CommandContext, page: Int) {
        val accounts = plugin.accounts.registry().values
            .sortedByDescending { it.capital }
        if (accounts.isEmpty()) {
            context.reply(msg("balance-top-empty"))
            return
        }

        val perPage = plugin.conf.commandsPerPage
        val maxPage = ((accounts.size - 1) / perPage) + 1
        val safePage = page.coerceIn(1, maxPage)
        context.reply(msg("balance-top-header", "page" to safePage.toString(), "pages" to maxPage.toString()))
        accounts.drop((safePage - 1) * perPage).take(perPage).forEachIndexed { index, account ->
            val rank = (safePage - 1) * perPage + index + 1
            context.reply(msg("balance-top-line", "rank" to rank.toString(), "player" to account.playerName.ifBlank { "unknown" }, "balance" to Formatter.formatMoney(account.capital)))
        }
    }

    private fun accountByName(name: String): Pair<String, AccountData>? {
        val online = Bukkit.getPlayerExact(name)
        if (online != null && plugin.accounts.has(online.uniqueId)) {
            return online.name to plugin.accounts.require(online.uniqueId)
        }
        val entry = plugin.accounts.registry().entries.firstOrNull { (_, account) ->
            account.playerName.equals(name, ignoreCase = true)
        } ?: return null
        return entry.value.playerName to entry.value
    }

    private fun accountSuggestions() = CommandSuggestions.strings { _, _ ->
        plugin.accounts.registry().values.mapNotNull { account -> account.playerName.takeIf { it.isNotBlank() } }
    }

    private fun homeSuggestions() = CommandSuggestions.strings { context, _ ->
        val player = context.player() ?: return@strings emptyList()
        plugin.players.get(player.uniqueId)?.homes?.keys ?: emptyList()
    }

    private fun warpSuggestions() = CommandSuggestions.dynamic { plugin.warps.registry().keys }

    private fun onlinePlayer(context: CommandContext, name: String): Player =
        Bukkit.getPlayerExact(name) ?: throw context.fail(msg("player-offline", "player" to name))

    private fun setGameMode(context: CommandContext, target: Player, gameMode: GameMode, self: Boolean) {
        target.gameMode = gameMode
        if (self) {
            context.reply(msg("gamemode-self", "gamemode" to gameMode.name.lowercase()))
        } else {
            context.reply(msg("gamemode-other", "player" to target.name, "gamemode" to gameMode.name.lowercase()))
            context.msgOther(target, msg("gamemode-self", "gamemode" to gameMode.name.lowercase()))
        }
    }

    private fun sendPrivateMessage(context: CommandContext, sender: Player, other: Player, message: String) {
        if (sender.uniqueId == other.uniqueId) {
            throw context.fail(msg("message-self"))
        }

        val senderData = plugin.players.require(sender.uniqueId)
        if (senderData.doNotDisturb) {
            throw context.fail(msg("message-dnd-self"))
        }
        if (senderData.ignoredPlayers.contains(other.uniqueId)) {
            throw context.fail(msg("message-ignored-self"))
        }

        val otherData = plugin.players.require(other.uniqueId)
        if (otherData.doNotDisturb) {
            throw context.fail(msg("message-dnd-other"))
        }
        if (otherData.ignoredPlayers.contains(sender.uniqueId)) {
            throw context.fail(msg("message-ignored-other"))
        }

        otherData.lastReceivedPlayer = sender.uniqueId
        otherData.markDirty()
        context.msgOther(other, msg("message-received", "player" to sender.name, "message" to message))
        context.reply(msg("message-sent", "player" to other.name, "message" to message))
    }

    private fun toggleFlight(context: CommandContext, player: Player) {
        player.allowFlight = !player.allowFlight
        if (!player.allowFlight) {
            player.isFlying = false
        }
        context.reply(msg("flight-toggled", "player" to player.name, "state" to enabledText(player.allowFlight)))
    }

    private fun healPlayer(context: CommandContext, player: Player, self: Boolean) {
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        player.health = maxHealth
        player.foodLevel = 20
        player.saturation = 20.0f
        player.fireTicks = 0
        if (self) {
            context.reply(msg("heal-self"))
        } else {
            context.reply(msg("heal-other", "player" to player.name))
            context.msgOther(player, msg("heal-received"))
        }
    }

    private fun checkCooldown(context: CommandContext, player: Player, key: String) {
        if (player.hasPermission("ramessentials.cooldown.bypass") || player.hasPermission("ramessentials.$key.bypass-cooldown")) {
            return
        }

        val seconds = plugin.conf.cooldownSeconds(key)
        if (seconds <= 0) {
            return
        }

        val now = System.currentTimeMillis()
        val playerCooldowns = cooldowns.getOrPut(player.uniqueId) { mutableMapOf() }
        val availableAt = playerCooldowns[key] ?: 0L
        if (availableAt > now) {
            val remaining = ((availableAt - now) / 1000L + 1L).toString()
            throw context.fail(msg("cooldown", "seconds" to remaining))
        }
        playerCooldowns[key] = now + seconds * 1000L
    }

    private fun homeLimitText(player: Player): String {
        val limit = plugin.conf.maxHomes(player)
        return if (limit == Int.MAX_VALUE) "unlimited" else limit.toString()
    }

    private fun sendWarps(context: CommandContext, page: Int) {
        val player = context.requirePlayer()
        val visible = plugin.warps.registry().values
            .filter { warp -> warp.visibleTo { permission -> player.hasPermission(permission) } }
            .sortedWith(compareBy<WarpData> { it.category }.thenBy { it.name.lowercase() })

        if (visible.isEmpty()) {
            context.reply(msg("warps-empty"))
            return
        }

        val perPage = plugin.conf.commandsPerPage
        val maxPage = ((visible.size - 1) / perPage) + 1
        val safePage = page.coerceIn(1, maxPage)
        context.reply(msg("warps-header", "page" to safePage.toString(), "pages" to maxPage.toString()))
        visible.drop((safePage - 1) * perPage).take(perPage).forEach { warp ->
            val date = FORMATTER.format(Instant.ofEpochMilli(warp.createdAt))
            context.reply(msg("warps-line", "warp" to warp.visibleName(), "category" to warp.category, "created" to date))
        }
    }

    private fun sendWarpInfo(context: CommandContext, warp: WarpData) {
        val created = FORMATTER.format(Instant.ofEpochMilli(warp.createdAt))
        val creator = warp.createdBy?.let { Bukkit.getOfflinePlayer(it).name ?: it.toString() } ?: "unknown"
        context.reply(msg("warp-info-header", "warp" to warp.visibleName()))
        context.reply(msg("warp-info-category", "category" to warp.category))
        context.reply(msg("warp-info-icon", "icon" to warp.icon))
        context.reply(msg("warp-info-permission", "permission" to (warp.permission ?: "none")))
        context.reply(msg("warp-info-created", "creator" to creator, "created" to created))
    }

    private fun modifyAccount(
        context: CommandContext,
        playerName: String,
        amount: Double,
        action: String,
        change: (AccountData, Double) -> Unit
    ) {
        val entry = accountByName(playerName)
            ?: throw context.fail(msg("account-missing"))
        change(entry.second, amount)
        plugin.transactions.log(action, context.player()?.uniqueId, null, amount, entry.second.capital, entry.first)
        context.reply(msg("eco-updated", "player" to entry.first, "balance" to Formatter.formatMoney(entry.second.capital)))
    }

    private fun expiresAt(): Long {
        return System.currentTimeMillis() + plugin.conf.teleportRequestTimeoutSeconds * 1000L
    }

    private fun putTeleportRequest(target: Player, request: TeleportRequest) {
        pendingTeleportRequests[target.uniqueId] = request
        Schedulers.runLater(target, {
            val current = pendingTeleportRequests[target.uniqueId]
            if (current == request) {
                pendingTeleportRequests.remove(target.uniqueId)
                target.sendRichMessage(plugin.conf.message("tpa-expired"))
                Bukkit.getPlayer(request.requester)?.sendRichMessage(plugin.conf.message("tpa-expired"))
            }
        }, plugin.conf.teleportRequestTimeoutSeconds * 20L, plugin)
    }

    private fun sendTeleportRequestMessage(target: Player, line: String) {
        val message = MiniMessage.miniMessage().deserialize("$line <gray>(")
            .append(Component.text("ACCEPT").clickEvent(ClickEvent.runCommand("/tpaccept")))
            .append(MiniMessage.miniMessage().deserialize("<gray> / "))
            .append(Component.text("DENY").clickEvent(ClickEvent.runCommand("/tpdeny")))
            .append(MiniMessage.miniMessage().deserialize("<gray>)"))
        target.sendMessage(message)
    }

    private fun msg(key: String, vararg placeholders: Pair<String, String>): String {
        return plugin.conf.message(key, placeholders.toMap())
    }

    private fun enabledText(enabled: Boolean): String {
        return if (enabled) msg("state-enabled") else msg("state-disabled")
    }

    private data class TeleportRequest(
        val requester: UUID,
        val target: UUID,
        val here: Boolean,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
    }
}
