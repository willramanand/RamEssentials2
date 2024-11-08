package dev.willram.ramEssentials2.data

import dev.willram.ramcore.data.DataItem

class AccountData : DataItem() {

    var playerName = ""
    var capital = 0.0

    fun has(amount: Double): Boolean {
        return amount <= capital
    }

    fun deposit(amount: Double) {
        this.capital += amount
    }

    fun withdraw(amount: Double) {
        this.capital -= amount
    }
}