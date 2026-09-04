package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.CashBookEntry
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashBookScreen(
    viewModel: TempleViewModel
) {
    val context = LocalContext.current
    val cashBookEntries by viewModel.cashBookEntries.collectAsState()
    val balances by viewModel.balances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val horizontalScrollState = rememberScrollState()

    val filteredEntries = remember(cashBookEntries, searchQuery) {
        if (searchQuery.isBlank()) cashBookEntries
        else cashBookEntries.filter {
            it.particulars.contains(searchQuery, ignoreCase = true) ||
            it.referenceOrVoucher.contains(searchQuery, ignoreCase = true) ||
            it.dateFormatted.contains(searchQuery, ignoreCase = true)
        }
    }

    val closingCash = balances.cashBalance

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
                            text = "കാഷ് ബുക്ക് (Cash Book Ledger)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ഓപ്പണിംഗ്: ₹${String.format(Locale.US, "%,.2f", balances.openingCash)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ക്ലോസിംഗ് കാഷ് (Closing Cash)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", closingCash)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CashGreen
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
                            .testTag("cash_book_search")
                    )

                    Button(
                        onClick = {
                            val csv = buildString {
                                append("Date,Particulars,Ref/Voucher,Receipt (In),Payment (Out),Balance\n")
                                filteredEntries.forEach { e ->
                                    append("\"${e.dateFormatted}\",\"${e.particulars.replace("\"", "\"\"")}\",\"${e.referenceOrVoucher}\",${e.receiptAmount},${e.paymentAmount},${e.balance}\n")
                                }
                            }
                            ExportUtil.exportToExcelCsv(context, "Chirayil_Temple_Cash_Book", csv)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                        modifier = Modifier.testTag("cash_book_export_excel")
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
            color = TempleMaroon.copy(alpha = 0.9f),
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
                Text("Ref / No", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                Text("വരവ് (+)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text("ചെലവ് (-)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
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
                            if (entry.receiptAmount > 0) "₹${String.format(Locale.US, "%,.0f", entry.receiptAmount)}" else "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CashGreen,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            if (entry.paymentAmount > 0) "₹${String.format(Locale.US, "%,.0f", entry.paymentAmount)}" else "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed,
                            modifier = Modifier.width(90.dp)
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
