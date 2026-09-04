package com.example.ui.screens

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
import com.example.data.local.entities.SponsorEntity
import com.example.data.local.entities.TransactionEntity
import com.example.model.PaymentMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsScreen(
    viewModel: TempleViewModel
) {
    val context = LocalContext.current
    val sponsors by viewModel.sponsors.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    val festivals by viewModel.festivals.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteConfirmSponsor by remember { mutableStateOf<SponsorEntity?>(null) }

    val filteredList = remember(sponsors, searchQuery) {
        if (searchQuery.isBlank()) sponsors
        else sponsors.filter {
            it.sponsorName.contains(searchQuery, ignoreCase = true) ||
            it.purpose.contains(searchQuery, ignoreCase = true) ||
            it.receiptNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isTreasurer) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TempleMaroon,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_sponsor")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Sponsor")
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
                                text = "സ്പോൺസർഷിപ്പ് & സംഭാവനകൾ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ആകെ ${filteredList.size} പേർ",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                        val total = filteredList.sumOf { it.amount }
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", total)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CashGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search sponsor, purpose, receipt...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sponsor_search_input")
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "സ്പോൺസർ വിവരങ്ങൾ ലഭ്യമല്ല" else "കണ്ടെത്തിയില്ല",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("sponsor_item_${item.id}"),
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
                                    Column {
                                        Text(
                                            text = item.sponsorName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (item.contact.isNotBlank()) {
                                            Text(
                                                text = "Ph: ${item.contact}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", item.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CashGreen
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "ഇനം: ${item.purpose.ifEmpty { "പൊതു സംഭാവന" }}${if (!item.festivalName.isNullOrBlank()) " • " + item.festivalName else ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "രസീത്: ${item.receiptNumber} • ${item.dateFormatted} • ${item.paymentMode}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                val tx = TransactionEntity(
                                                    id = item.id,
                                                    type = "INCOME",
                                                    voucherOrReceiptNo = item.receiptNumber,
                                                    date = item.date,
                                                    dateFormatted = item.dateFormatted,
                                                    category = "Sponsor",
                                                    description = item.purpose,
                                                    amount = item.amount,
                                                    paymentMode = item.paymentMode,
                                                    partyName = item.sponsorName,
                                                    festivalName = item.festivalName,
                                                    notes = item.notes
                                                )
                                                ExportUtil.generateAndShareReceiptPdf(context, tx)
                                            }
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = TempleMaroon)
                                        }

                                        if (isTreasurer) {
                                            IconButton(onClick = { deleteConfirmSponsor = item }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
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

    if (showAddDialog) {
        AddSponsorDialog(
            festivals = festivals,
            onDismiss = { showAddDialog = false },
            onSave = { name, contact, amt, mode, purpose, fest, notes ->
                viewModel.addSponsor(name, contact, amt, mode, purpose, fest, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    deleteConfirmSponsor?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirmSponsor = null },
            title = { Text("സ്പോൺസർ റെക്കോർഡ് നീക്കം ചെയ്യുക") },
            text = { Text("${item.sponsorName} (₹${item.amount}) റെക്കോർഡ് നീക്കം ചെയ്യണോ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSponsor(item.id)
                        deleteConfirmSponsor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmSponsor = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSponsorDialog(
    festivals: List<FestivalEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, PaymentMode, String, FestivalEntity?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var purpose by remember { mutableStateOf("") }
    var selectedFestival by remember { mutableStateOf<FestivalEntity?>(null) }
    var festivalExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("പുതിയ സ്പോൺസർഷിപ്പ് (New Sponsor)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("സ്പോൺസറുടെ പേര് (Sponsor Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sponsor_name_input")
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("ഫോൺ നമ്പർ (Phone Number)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("തുക (Amount ₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("sponsor_amount_input")
                )

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

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("എന്തിനുവേണ്ടി / ഇനം (Purpose / Item)") },
                    placeholder = { Text("ഉദാ: അന്നദാനം, വെടിക്കെട്ട്, പുഷ്പാലങ്കാരം") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(name, contact, amt, paymentMode, purpose, selectedFestival, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.testTag("save_sponsor_button")
            ) {
                Text("Save Sponsor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
