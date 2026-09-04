package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.BankBookEntry
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.util.*

@Composable
fun BankBookScreen(
    viewModel: TempleViewModel
) {
    val context = LocalContext.current
    val bankBookEntries by viewModel.bankBookEntries.collectAsState()
    val balances by viewModel.balances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val horizontalScrollState = rememberScrollState()

    val filteredEntries = remember(bankBookEntries, searchQuery) {
        if (searchQuery.isBlank()) bankBookEntries
        else bankBookEntries.filter {
            it.particulars.contains(searchQuery, ignoreCase = true) ||
            it.referenceOrVoucher.contains(searchQuery, ignoreCase = true) ||
            it.dateFormatted.contains(searchQuery, ignoreCase = true)
        }
    }

    val closingBank = balances.bankBalance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Summary & Export Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ബാങ്ക് ബുക്ക് (Bank Book Ledger)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "തുടക്ക ബാങ്ക്: ₹${String.format(Locale.US, "%,.2f", balances.openingBank)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ക്ലോസിംഗ് ബാങ്ക് ബാലൻസ്",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", closingBank)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BankBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter entries...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bank_book_search")
                    )

                    Button(
                        onClick = {
                            val csv = buildString {
                                append("Date,Particulars,Cheque/Ref,Credit (Deposits),Debit (Withdrawals),Balance\n")
                                filteredEntries.forEach { e ->
                                    append("\"${e.dateFormatted}\",\"${e.particulars.replace("\"", "\"\"")}\",\"${e.referenceOrVoucher}\",${e.creditAmount},${e.debitAmount},${e.balance}\n")
                                }
                            }
                            ExportUtil.exportToExcelCsv(context, "Chirayil_Temple_Bank_Book", csv)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BankBlue),
                        modifier = Modifier.testTag("bank_book_export_excel")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel")
                    }
                }
            }
        }

        // Ledger Table Header
        Surface(
            color = BankBlue.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("തീയതി (Date)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text("വിവരം (Particulars)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(180.dp))
                Text("Cheque / Ref", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                Text("Credit (നിക്ഷേപം)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                Text("Debit (പിൻവലിക്കൽ)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                Text("ബാക്കി (Balance)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            }
        }

        // Ledger Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredEntries, key = { it.id }) { entry ->
                Surface(
                    color = if (entry.id == "OPENING") TempleGoldLight.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.dateFormatted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                        Text(
                            entry.particulars,
                            fontSize = 12.sp,
                            fontWeight = if (entry.id == "OPENING") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width(180.dp)
                        )
                        Text(entry.referenceOrVoucher, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(100.dp))
                        Text(
                            if (entry.creditAmount > 0) "₹${String.format(Locale.US, "%,.0f", entry.creditAmount)}" else "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BankBlue,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            if (entry.debitAmount > 0) "₹${String.format(Locale.US, "%,.0f", entry.debitAmount)}" else "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            "₹${String.format(Locale.US, "%,.2f", entry.balance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
