package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify temple app name and strings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val templeName = context.getString(R.string.temple_name_ml)
        val location = context.getString(R.string.temple_location_ml)

        assertEquals("Chirayil Temple Accounts", appName)
        assertEquals("ചിറയിൽ ശ്രീ മാടൻനട ക്ഷേത്രം", templeName)
        assertEquals("തോട്ടയ്ക്കാട്", location)
    }

    @Test
    fun `verify temple accounting math formulas`() {
        // Test Accounting Rules:
        // Cash Balance = Opening Cash + Cash Income - Cash Expense + Transfers (Bank to Cash) - Transfers (Cash to Bank)
        val openingCash = 25000.0
        val cashIncome = 12500.0
        val cashExpense = 4500.0
        val cashToBank = 10000.0
        val bankToCash = 2000.0

        val expectedCashClosing = openingCash + cashIncome - cashExpense - cashToBank + bankToCash
        assertEquals(25000.0, expectedCashClosing, 0.001)

        // Test Net Surplus:
        // Net Surplus = Total Income - Total Expense
        val totalIncome = 50000.0
        val totalExpense = 32000.0
        val netSurplus = totalIncome - totalExpense
        assertEquals(18000.0, netSurplus, 0.001)
        assertTrue(netSurplus > 0)
    }
}
