package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMode
import com.example.model.TempleConstants
import com.example.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod(val label: String) {
    TODAY("ഇന്ന് (Today)"),
    THIS_MONTH("ഈ മാസം (This Month)"),
    THIS_YEAR("ഈ വർഷം (This Year)"),
    ALL_TIME("എല്ലാ കാലയളവും (All Time)"),
    CUSTOM("തിരഞ്ഞെടുത്തത് (Custom)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TempleViewModel
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val festivals by viewModel.festivals.collectAsState()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    val calendar = Calendar.getInstance()
    var customStartDate by remember { mutableStateOf(calendar.timeInMillis - (30L * 86400000L)) }
    var customEndDate by remember { mutableStateOf(calendar.timeInMillis) }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    // Calculate Date Range
    val (startDate, endDate) = remember(selectedPeriod, customStartDate, customEndDate) {
        val now = Calendar.getInstance()
        when (selectedPeriod) {
            ReportPeriod.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                start to end
            }
            ReportPeriod.THIS_MONTH -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                start to end
            }
            ReportPeriod.THIS_YEAR -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                start to end
            }
            ReportPeriod.ALL_TIME -> 0L to Long.MAX_VALUE
            ReportPeriod.CUSTOM -> customStartDate to customEndDate
        }
    }

    // Filter transactions by period
    val periodTransactions = remember(allTransactions, startDate, endDate) {
        allTransactions.filter { it.date in startDate..endDate }
    }

    val periodIncomeList = remember(periodTransactions) {
        periodTransactions.filter { it.type == TransactionType.INCOME.name }
    }
    val periodExpenseList = remember(periodTransactions) {
        periodTransactions.filter { it.type == TransactionType.EXPENSE.name }
    }

    val totalIncome = remember(periodIncomeList) { periodIncomeList.sumOf { it.amount } }
    val totalExpense = remember(periodExpenseList) { periodExpenseList.sumOf { it.amount } }
    val netSurplus = totalIncome - totalExpense

    // Category aggregations
    val incomeByCategory = remember(periodIncomeList) {
        periodIncomeList.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
    }
    val expenseByCategory = remember(periodExpenseList) {
        periodExpenseList.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
    }

    // Helper text generation for share / export
    fun buildReportSummaryText(): String = buildString {
        append("========================================\n")
        append("${TempleConstants.TEMPLE_NAME}\n")
        append("${TempleConstants.TEMPLE_LOCATION}\n")
        append("സാമ്പത്തിക റിപ്പോർട്ട് (Financial Statement)\n")
        append("കാലയളവ്: ${if (startDate == 0L) "All Time" else dateFormat.format(Date(startDate))} മുതൽ ${if (endDate == Long.MAX_VALUE) "Present" else dateFormat.format(Date(endDate))} വരെ\n")
        append("തീയതി: ${dateFormat.format(Date())}\n")
        append("========================================\n\n")

        append("ആകെ വരുമാനം (Total Income): ₹${String.format(Locale.US, "%,.2f", totalIncome)}\n")
        append("ആകെ ചെലവ് (Total Expense): ₹${String.format(Locale.US, "%,.2f", totalExpense)}\n")
        append("നീക്കിയിരിപ്പ് (Net Surplus/Deficit): ₹${String.format(Locale.US, "%,.2f", netSurplus)}\n")
        append("നിലവിലെ കാഷ് ബാലൻസ്: ₹${String.format(Locale.US, "%,.2f", balances.cashBalance)}\n")
        append("നിലവിലെ ബാങ്ക് ബാലൻസ്: ₹${String.format(Locale.US, "%,.2f", balances.bankBalance)}\n\n")

        append("----- വരുമാന വിഭാഗങ്ങൾ (Income Breakdown) -----\n")
        incomeByCategory.forEach { (cat, amt) ->
            append("• $cat: ₹${String.format(Locale.US, "%,.2f", amt)}\n")
        }
        append("\n----- ചെലവ് വിഭാഗങ്ങൾ (Expense Breakdown) -----\n")
        expenseByCategory.forEach { (cat, amt) ->
            append("• $cat: ₹${String.format(Locale.US, "%,.2f", amt)}\n")
        }

        append("\n\nതയ്യാറാക്കിയത്: ട്രഷറർ\nഅംഗീകരിച്ചത്: പ്രസിഡന്റ് / സെക്രട്ടറി\n")
        append("========================================\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector
        Card(
            modifier = Modifier.fillMaxWidth().testTag("report_period_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "റിപ്പോർട്ട് കാലയളവ് (Report Period)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReportPeriod.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label, fontSize = 10.sp) }
                        )
                    }
                }

                if (selectedPeriod == ReportPeriod.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = customStartDate }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newC = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                    customStartDate = newC.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("From: ${dateFormat.format(Date(customStartDate))}", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = customEndDate }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newC = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                    customEndDate = newC.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To: ${dateFormat.format(Date(customEndDate))}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Action Buttons: Share WhatsApp, Export Excel CSV, Share Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    ExportUtil.shareText(context, "${TempleConstants.TEMPLE_NAME} Accounts Report", buildReportSummaryText())
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.weight(1f).testTag("report_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Report", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val csv = buildString {
                        append("Type,Number,Date,Category,Party/Description,Payment Mode,Amount\n")
                        periodTransactions.forEach { tx ->
                            append("\"${tx.type}\",\"${tx.voucherOrReceiptNo}\",\"${tx.dateFormatted}\",\"${tx.category}\",\"${tx.partyName.ifEmpty { tx.description }.replace("\"", "\"\"")}\",\"${tx.paymentMode}\",${tx.amount}\n")
                        }
                    }
                    ExportUtil.exportToExcelCsv(context, "Chirayil_Temple_Accounts_${selectedPeriod.name}", csv)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                modifier = Modifier.weight(1f).testTag("report_export_excel_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Excel Export", fontSize = 12.sp)
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "സാമ്പത്തിക സംഗ്രഹം (Period Summary)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TempleMaroon)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ആകെ വരുമാനം (Income):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", totalIncome)}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = CashGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ആകെ ചെലവ് (Expense):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", totalExpense)}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (netSurplus >= 0) "നീക്കിയിരിപ്പ് ലാഭം (Surplus):" else "കമ്മി (Deficit):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", netSurplus)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (netSurplus >= 0) CashGreen else ExpenseRed
                        )
                    )
                }
            }
        }

        // Category-wise Income Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "വരുമാന വിഭാഗങ്ങൾ (Income by Category)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CashGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (incomeByCategory.isEmpty()) {
                    Text("ഈ കാലയളവിൽ വരുമാന രേഖകൾ ലഭ്യമല്ല.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                } else {
                    incomeByCategory.forEach { (cat, amt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${String.format(Locale.US, "%,.2f", amt)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Category-wise Expense Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ചെലവ് വിഭാഗങ്ങൾ (Expense by Category)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (expenseByCategory.isEmpty()) {
                    Text("ഈ കാലയളവിൽ ചെലവ് രേഖകൾ ലഭ്യമല്ല.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                } else {
                    expenseByCategory.forEach { (cat, amt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${String.format(Locale.US, "%,.2f", amt)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
