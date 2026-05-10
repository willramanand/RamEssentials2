package dev.willram.ramEssentials2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDataTest {
    @Test
    fun depositAndWithdrawMarkAccountDirty() {
        val account = AccountData()
        account.markClean()

        account.deposit(25.0)

        assertEquals(25.0, account.capital, 0.0001)
        assertTrue(account.dirty())

        account.markClean()
        account.withdraw(5.0)

        assertEquals(20.0, account.capital, 0.0001)
        assertTrue(account.dirty())
        assertTrue(account.has(20.0))
        assertFalse(account.has(20.01))
    }
}
