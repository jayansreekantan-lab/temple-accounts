package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FestivalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.model.PaymentMode
import com.example.model.TempleCategories
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: TempleViewModel,
    onViewReceipt: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val incomeList by viewModel.incomeTransactions.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    val festivals by viewModel.festivals.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteConfirmTx by remember { mutableStateOf<TransactionEntity?>(null) }

    // Filtered list
    val filteredList = remember(incomeList, searchQuery, selectedCategoryFilter) {
        incomeList.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.partyName.contains(searchQuery, ignoreCase = true) ||
                    tx.description.contains(searchQuery, ignoreCase = true) ||
                    tx.voucherOrReceiptNo.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "All" || tx.category == selectedCategoryFilter
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isTreasurer) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TempleMaroon,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_income")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Income")
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
            // Search Bar & Stats Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Total income banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "വരുമാനം (Income Entries)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ആകെ ${filteredList.size} രസീതുകൾ",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                        val totalAmount = filteredList.sumOf { it.amount }
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CashGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, receipt no, category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("income_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category filter chips
                    ScrollableTabRow(
                        selectedTabIndex = if (selectedCategoryFilter == "All") 0 else (TempleCategories.incomeCategories.indexOf(selectedCategoryFilter) + 1).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedCategoryFilter == "All",
                            onClick = { selectedCategoryFilter = "All" },
                            text = { Text("എല്ലാം (All)") }
                        )
                        TempleCategories.incomeCategories.forEach { cat ->
                            Tab(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                text = { Text(cat, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // Income List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "വരുമാന ഇടപാടുകൾ ലഭ്യമല്ല" else "തിരഞ്ഞ വിവരങ്ങൾ കണ്ടെത്തിയില്ല",
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
                    items(filteredList, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewReceipt(tx) }
                                .testTag("income_item_${tx.voucherOrReceiptNo}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = CashGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tx.voucherOrReceiptNo,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CashGreen,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tx.dateFormatted,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    Text(
                                        text = "+₹${String.format(Locale.US, "%,.2f", tx.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CashGreen
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = tx.partyName.ifEmpty { tx.description },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tx.category} • ${tx.paymentMode}${if (!tx.festivalName.isNullOrBlank()) " • " + tx.festivalName else ""}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )

                                    // Action buttons for receipt / treasurer
                                    Row {
                                        IconButton(
                                            onClick = { ExportUtil.generateAndShareReceiptPdf(context, tx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share PDF Receipt",
                                                tint = TempleMaroon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isTreasurer) {
                                            IconButton(
                                                onClick = { editingTx = tx },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Income",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { deleteConfirmTx = tx },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Income",
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
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
    }

    // Add Income Dialog
    if (showAddDialog) {
        AddEditIncomeDialog(
            festivals = festivals,
            onDismiss = { showAddDialog = false },
            onSave = { date, category, desc, amount, mode, party, fest, notes ->
                viewModel.addIncome(date, category, desc, amount, mode, party, fest, notes) { createdTx ->
                    showAddDialog = false
                    onViewReceipt(createdTx)
                }
            }
        )
    }

    // Edit Income Dialog
    editingTx?.let { tx ->
        EditIncomeDialog(
            transaction = tx,
            festivals = festivals,
            onDismiss = { editingTx = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                editingTx = null
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTx = null },
            title = { Text("വരുമാന രസീത് നീക്കം ചെയ്യുക") },
            text = {
                Text("രസീത് നമ്പർ ${tx.voucherOrReceiptNo} (₹${tx.amount}) സ്ഥിരമായി നീക്കം ചെയ്യണമെന്ന് ഉറപ്പാണോ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        deleteConfirmTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeDialog(
    festivals: List<FestivalEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Double, PaymentMode, String, FestivalEntity?, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedCategory by remember { mutableStateOf(TempleCategories.incomeCategories.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var partyName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var selectedFestival by remember { mutableStateOf<FestivalEntity?>(null) }
    var festivalExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("വരുമാനം രേഖപ്പെടുത്തുക (Add Income)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Picker field
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
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TempleMaroon)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("തീയതി (Date)", style = MaterialTheme.typography.labelSmall)
                            Text(dateFormat.format(Date(selectedDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (വിഭാഗം)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        TempleCategories.incomeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Received From
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Received From (ലഭിച്ച ആൾ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("income_party_input")
                )

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (തുക ₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("income_amount_input")
                )

                // Payment Mode Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentMode == PaymentMode.CASH,
                        onClick = { paymentMode = PaymentMode.CASH },
                        label = { Text("കാഷ് (Cash)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentMode == PaymentMode.BANK,
                        onClick = { paymentMode = PaymentMode.BANK },
                        label = { Text("ബാങ്ക് (Bank)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Optional Festival Link
                if (festivals.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = festivalExpanded,
                        onExpandedChange = { festivalExpanded = !festivalExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFestival?.name ?: "സാധാരണ അക്കൗണ്ട് (General)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ഉത്സവം (Festival - Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = festivalExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = festivalExpanded,
                            onDismissRequest = { festivalExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("സാധാരണ അക്കൗണ്ട് (General)") },
                                onClick = {
                                    selectedFestival = null
                                    festivalExpanded = false
                                }
                            )
                            festivals.forEach { fest ->
                                DropdownMenuItem(
                                    text = { Text(fest.name) },
                                    onClick = {
                                        selectedFestival = fest
                                        festivalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Description / Purpose
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("വിവരണം (Description / Purpose)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("കുറിപ്പ് (Notes - Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(selectedDate, selectedCategory, description, amt, paymentMode, partyName, selectedFestival, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.testTag("save_income_button")
            ) {
                Text("Save & Generate Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeDialog(
    transaction: TransactionEntity,
    festivals: List<FestivalEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var partyName by remember { mutableStateOf(transaction.partyName) }
    var description by remember { mutableStateOf(transaction.description) }
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(transaction.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("രസീത് തിരുത്തുക: ${transaction.voucherOrReceiptNo}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Received From") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        TempleCategories.incomeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: transaction.amount
                    onSave(
                        transaction.copy(
                            partyName = partyName,
                            amount = amt,
                            category = selectedCategory,
                            description = description,
                            notes = notes
                        )
                    )
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
