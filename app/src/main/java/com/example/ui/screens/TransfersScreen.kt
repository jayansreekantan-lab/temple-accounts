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
import com.example.data.local.entities.TransferEntity
import com.example.model.TransferType
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransfersScreen(
    viewModel: TempleViewModel
) {
    val transfers by viewModel.transfers.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteConfirmTransfer by remember { mutableStateOf<TransferEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (isTreasurer) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TempleMaroon,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_transfer")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Transfer")
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
            // Informational header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "കാഷ് ↔ ബാങ്ക് ട്രാൻസ്ഫറുകൾ (Contra Transfers)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ട്രാൻസ്ഫറുകൾ വരുമാനമോ ചെലവോ ആയി കണക്കാക്കില്ല. ഇവ കാഷ് ബുക്കിലും ബാങ്ക് ബുക്കിലും നേരിട്ട് പ്രതിഫലിക്കുന്നു.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }

            if (transfers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ട്രാൻസ്ഫർ വിവരങ്ങൾ ഒന്നും രേഖപ്പെടുത്തിയിട്ടില്ല.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transfers, key = { it.id }) { item ->
                        val isCashToBank = item.transferType == TransferType.CASH_TO_BANK.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_item_${item.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = if (isCashToBank) BankBlue else CashGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isCashToBank) "കാഷ് ➔ ബാങ്ക് (Cash to Bank)" else "ബാങ്ക് ➔ കാഷ് (Bank to Cash)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${item.dateFormatted} • Ref: ${item.reference.ifEmpty { "-" }}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 11.sp
                                            )
                                        )
                                        if (item.notes.isNotBlank()) {
                                            Text(
                                                text = item.notes,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", item.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TempleMaroon
                                        )
                                    )
                                    if (isTreasurer) {
                                        IconButton(onClick = { deleteConfirmTransfer = item }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(20.dp)
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
    }

    if (showAddDialog) {
        AddTransferDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, date, amount, ref, notes ->
                viewModel.addTransfer(type, date, amount, ref, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    deleteConfirmTransfer?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTransfer = null },
            title = { Text("ട്രാൻസ്ഫർ നീക്കം ചെയ്യുക") },
            text = { Text("₹${item.amount} ട്രാൻസ്ഫർ ഇടപാട് സ്ഥിരമായി നീക്കം ചെയ്യണോ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransfer(item.id)
                        deleteConfirmTransfer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTransfer = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddTransferDialog(
    onDismiss: () -> Unit,
    onSave: (TransferType, Long, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(TransferType.CASH_TO_BANK) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var amountStr by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("പുതിയ ട്രാൻസ്ഫർ (New Transfer)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransferType.CASH_TO_BANK,
                        onClick = { selectedType = TransferType.CASH_TO_BANK },
                        label = { Text("കാഷ് ➔ ബാങ്ക് (Deposit)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransferType.BANK_TO_CASH,
                        onClick = { selectedType = TransferType.BANK_TO_CASH },
                        label = { Text("ബാങ്ക് ➔ കാഷ് (Withdraw)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Date
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val c = Calendar.getInstance()
                            c.timeInMillis = selectedDate
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    selectedDate = newCal.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TempleMaroon)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("തീയതി (Date)", style = MaterialTheme.typography.labelSmall)
                            Text(dateFormat.format(Date(selectedDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (തുക ₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / Cheque / Txn ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (കുറിപ്പ്)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(selectedType, selectedDate, amt, reference, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.testTag("save_transfer_button")
            ) {
                Text("Transfer Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
