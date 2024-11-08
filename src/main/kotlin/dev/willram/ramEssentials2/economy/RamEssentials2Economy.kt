package dev.willram.ramEssentials2.economy

import dev.willram.ramEssentials2.RamEssentials2
import dev.willram.ramEssentials2.data.AccountData
import dev.willram.ramcore.utils.Formatter
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

class RamEssentials2Economy(private val plugin: RamEssentials2) : Economy {
    override fun isEnabled(): Boolean {
        return true
    }

    override fun getName(): String {
        return "RamEssentials2 Economy"
    }

    override fun hasBankSupport(): Boolean {
        return false
    }

    override fun fractionalDigits(): Int {
        return -1
    }

    override fun format(amount: Double): String {
        return Formatter.formatMoney(amount)
    }

    override fun currencyNamePlural(): String {
        return currencyNameSingular()
    }

    override fun currencyNameSingular(): String {
        return "$"
    }

    @Deprecated("")
    override fun hasAccount(playerName: String): Boolean {
        val id = getIdFromName(playerName)
        return id != null && plugin.accounts.has(id)
    }

    override fun hasAccount(player: OfflinePlayer): Boolean {
        return plugin.accounts.has(player.uniqueId)
    }

    @Deprecated("")
    override fun hasAccount(playerName: String, worldName: String): Boolean {
        return hasAccount(playerName)
    }

    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean {
        return hasAccount(player)
    }

    @Deprecated("")
    override fun getBalance(playerName: String): Double {
        val id = getIdFromName(playerName) ?: return 0.0

        return plugin.accounts.get(id).capital
    }

    override fun getBalance(player: OfflinePlayer): Double {
        return plugin.accounts.get(player.uniqueId).capital
    }

    @Deprecated("")
    override fun getBalance(playerName: String, world: String): Double {
        return getBalance(playerName)
    }

    override fun getBalance(player: OfflinePlayer, world: String): Double {
        return getBalance(player)
    }

    @Deprecated("")
    override fun has(playerName: String, amount: Double): Boolean {
        val id = getIdFromName(playerName) ?: return false
        return plugin.accounts.get(id).has(amount)
    }

    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return plugin.accounts.get(player.uniqueId).has(amount)
    }

    @Deprecated("")
    override fun has(playerName: String, worldName: String, amount: Double): Boolean {
        return has(playerName, amount)
    }

    override fun has(player: OfflinePlayer, worldName: String, amount: Double): Boolean {
        return has(player, amount)
    }

    @Deprecated("")
    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        if (amount < 0) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!")
        }

        val id = getIdFromName(playerName) ?: return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not exist!")

        if (!plugin.accounts.has(id)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have an account!")
        }

        if (!plugin.accounts.get(id).has(amount)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have this amount of capital!")
        }

        plugin.accounts.get(id).withdraw(amount)
        return EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null)
    }

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!")
        }

        if (!plugin.accounts.has(player.uniqueId)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have an account!")
        }

        if (!plugin.accounts[player.uniqueId].has(amount)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have this amount of capital!")
        }

        plugin.accounts[player.uniqueId].withdraw(amount)
        return EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("")
    override fun withdrawPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(playerName, amount)
    }

    override fun withdrawPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(player, amount)
    }

    @Deprecated("")
    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        if (amount < 0) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds")
        }

        val id = getIdFromName(playerName) ?: return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not exist!")

        if (!plugin.accounts.has(id)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have an account!")
        }


        plugin.accounts[id].deposit(amount)
        return EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null)
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds")
        }

        if (!plugin.accounts.has(player.uniqueId)) {
            return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player does not have an account!")
        }

        plugin.accounts[player.uniqueId].deposit(amount)
        return EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("")
    override fun depositPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse {
        return depositPlayer(playerName, amount)
    }

    override fun depositPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse {
        return depositPlayer(player, amount)
    }

    @Deprecated("")
    override fun createBank(name: String, player: String): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun deleteBank(name: String): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun bankBalance(name: String): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun bankHas(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun bankWithdraw(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun bankDeposit(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    @Deprecated("")
    override fun isBankOwner(name: String, playerName: String): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    @Deprecated("")
    override fun isBankMember(name: String, playerName: String): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "RamEssentials does not support bank accounts!"
        )
    }

    override fun getBanks(): List<String> {
        return emptyList()
    }

    @Deprecated("")
    override fun createPlayerAccount(playerName: String): Boolean {
        if (hasAccount(playerName)) {
            return false
        }

        val id = getIdFromName(playerName) ?: return false

        plugin.accounts.add(id, AccountData())
        return true
    }

    override fun createPlayerAccount(player: OfflinePlayer): Boolean {
        if (hasAccount(player)) {
            return false
        }

        plugin.accounts.add(player.uniqueId, AccountData())
        return true
    }

    @Deprecated("")
    override fun createPlayerAccount(playerName: String, worldName: String): Boolean {
        return createPlayerAccount(playerName)
    }

    override fun createPlayerAccount(player: OfflinePlayer, worldName: String): Boolean {
        return createPlayerAccount(player)
    }

    fun getIdFromName(name: String): UUID? {
        val player = Bukkit.getPlayer(name)
        return player?.uniqueId
    }
}