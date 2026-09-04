package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FestivalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FestivalsScreen(
    viewModel: TempleViewModel,
    onViewReceipt: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val festivals by viewModel.festivals.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFestivalId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmFestival by remember { mutableStateOf<FestivalEntity?>(null) }

    val activeFestival = remember(festivals, selectedFestivalId) {
        if (selectedFestivalId != null) {
            festivals.firstOrNull { it.id == selectedFestivalId } ?: festivals.firstOrNull()
        } else {
            festivals.firstOrNull()
        }
    }

    val festivalTransactions = remember(allTransactions, activeFestival) {
        if (activeFestival == null) emptyList()
        else allTransactions.filter { it.festivalId == activeFestival.id }
    }

    val fIncome = remember(festivalTransactions) {
        festivalTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    }
    val fExpense = remember(festivalTransactions) {
        festivalTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    }
    val closingBalance = remember(activeFestival, fIncome, fExpense) {
        (activeFestival?.openingBalance ?: 0.0) + fIncome - fExpense
    }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    Scaffold(
        floatingActionButton = {
            if (isTreasurer) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TempleMaroon,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_festival")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Festival")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (festivals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ഉത്സവ അക്കൗണ്ടുകൾ ഒന്നും ചേർത്തിട്ടില്ല. 'Add Festival' ക്ലിക്ക് ചെയ്യുക.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                // Festival Tabs
                ScrollableTabRow(
                    selectedTabIndex = festivals.indexOf(activeFestival).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    festivals.forEach { fest ->
                        Tab(
                            selected = fest.id == activeFestival?.id,
                            onClick = { selectedFestivalId = fest.id },
                            text = { Text(fest.name, maxLines = 1) }
                        )
                    }
                }

                // Active Festival Summary Card
                activeFestival?.let { fest ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fest.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TempleMaroon
                                    )
                                    Text(
                                        text = "${dateFormat.format(Date(fest.startDate))} മുതൽ ${dateFormat.format(Date(fest.endDate))} വരെ",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            val reportText = buildString {
                                                append("${fest.name} - കണക്ക് വിവരങ്ങൾ\n")
                                                append("--------------------------------------\n")
                                                append("ഓപ്പണിംഗ് ബാലൻസ്: ₹${String.format(Locale.US, "%,.2f", fest.openingBalance)}\n")
                                                append("ആകെ വരുമാനം: ₹${String.format(Locale.US, "%,.2f", fIncome)}\n")
                                                append("ആകെ ചെലവ്: ₹${String.format(Locale.US, "%,.2f", fExpense)}\n")
                                                append("ബാക്കി (Closing Balance): ₹${String.format(Locale.US, "%,.2f", closingBalance)}\n\n")
                                                append("ഇടപാടുകൾ (${festivalTransactions.size}):\n")
                                                festivalTransactions.forEach { tx ->
                                                    append("${tx.dateFormatted} | ${tx.voucherOrReceiptNo} | ${tx.type} | ₹${tx.amount} | ${tx.partyName.ifEmpty { tx.description }}\n")
                                                }
                                            }
                                            ExportUtil.shareText(context, "${fest.name} Balance Sheet", reportText)
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Balance Sheet", tint = TempleMaroon)
                                    }

                                    if (isTreasurer) {
                                        IconButton(onClick = { deleteConfirmFestival = fest }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Breakdown Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricColumn("തുടക്ക ബാലൻസ്", fest.openingBalance, MaterialTheme.colorScheme.onSurface)
                                MetricColumn("വരവ് (Income)", fIncome, CashGreen)
                                MetricColumn("ചെലവ് (Expense)", fExpense, ExpenseRed)
                                MetricColumn("നീക്കിരിപ്പ് (Net)", closingBalance, TempleMaroon)
                            }
                        }
                    }

                    // Festival Transactions Header
                    Text(
                        text = "ഉത്സവ ഇടപാടുകൾ (${festivalTransactions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Transactions List
                    if (festivalTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ഈ ഉത്സവത്തിൽ ഇടപാടുകൾ ഒന്നും രേഖപ്പെടുത്തിയിട്ടില്ല.", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(festivalTransactions, key = { it.id }) { tx ->
                                val isIncome = tx.type == "INCOME"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewReceipt(tx) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = tx.partyName.ifEmpty { tx.description },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${tx.category} • ${tx.voucherOrReceiptNo} • ${tx.dateFormatted}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                            )
                                        }

                                        Text(
                                            text = "${if (isIncome) "+" else "-"}₹${String.format(Locale.US, "%,.2f", tx.amount)}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIncome) CashGreen else ExpenseRed
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFestivalDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, start, end, opening, notes ->
                viewModel.addFestival(name, start, end, opening, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    deleteConfirmFestival?.let { fest ->
        AlertDialog(
            onDismissRequest = { deleteConfirmFestival = null },
            title = { Text("ഉത്സവ അക്കൗണ്ട് നീക്കം ചെയ്യുക") },
            text = { Text("${fest.name} അക്കൗണ്ട് സ്ഥിരമായി നീക്കം ചെയ്യണോ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFestival(fest.id)
                        deleteConfirmFestival = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmFestival = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.outline))
        Spacer(modifier = Modifier.height(2.dp))
        Text("₹${String.format(Locale.US, "%,.0f", amount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
    }
}

@Composable
fun AddFestivalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Double, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis() + 864000000L) }
    var openingBalanceStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("പുതിയ ഉത്സവ അക്കൗണ്ട് (New Festival)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ഉത്സവത്തിന്റെ പേര് (Festival Name)") },
                    placeholder = { Text("ഉദാ: തൃക്കൊടിയേറ്റ് മഹോത്സവം 2027") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("festival_name_input")
                )

                // Start Date
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val c = Calendar.getInstance()
                            c.timeInMillis = startDate
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    startDate = newCal.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TempleMaroon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ആരംഭ തീയതി: ${dateFormat.format(Date(startDate))}", fontSize = 12.sp)
                    }
                }

                // End Date
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val c = Calendar.getInstance()
                            c.timeInMillis = endDate
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    endDate = newCal.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = TempleMaroon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("സമാപന തീയതി: ${dateFormat.format(Date(endDate))}", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = openingBalanceStr,
                    onValueChange = { openingBalanceStr = it },
                    label = { Text("തുടക്ക ബാലൻസ് (Opening Balance ₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("കുറിപ്പ് (Notes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opening = openingBalanceStr.toDoubleOrNull() ?: 0.0
                    onSave(name, startDate, endDate, opening, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.testTag("save_festival_button")
            ) {
                Text("Save Festival")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
