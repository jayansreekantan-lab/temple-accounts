package com.example.model

data class TempleBalances(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val openingCash: Double = 0.0,
    val openingBank: Double = 0.0,
    val currentFestivalBalance: Double = 0.0,
    val currentFestivalName: String = ""
)

data class CashBookEntry(
    val id: String,
    val date: Long,
    val dateFormatted: String,
    val particulars: String,
    val referenceOrVoucher: String,
    val receiptAmount: Double = 0.0, // Cash in
    val paymentAmount: Double = 0.0, // Cash out
    val balance: Double = 0.0
)

data class BankBookEntry(
    val id: String,
    val date: Long,
    val dateFormatted: String,
    val particulars: String,
    val referenceOrVoucher: String,
    val creditAmount: Double = 0.0, // Bank in
    val debitAmount: Double = 0.0,  // Bank out
    val balance: Double = 0.0
)

data class FestivalSummary(
    val id: String,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val openingBalance: Double,
    val income: Double,
    val expense: Double,
    val closingBalance: Double
)

data class FinancialReport(
    val fromDate: Long,
    val toDate: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSurplus: Double,
    val cashBalance: Double,
    val bankBalance: Double,
    val categoryWiseIncome: Map<String, Double>,
    val categoryWiseExpense: Map<String, Double>,
    val paymentModeIncome: Map<String, Double>,
    val paymentModeExpense: Map<String, Double>
)
