package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.model.TempleConstants
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import com.example.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreScreen(
    viewModel: TempleViewModel
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    var restoreJsonInput by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ഡാറ്റാ ബാക്കപ്പും പുനഃസ്ഥാപിക്കലും (Backup & Restore)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TempleMaroon)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ക്ഷേത്രത്തിന്റെ എല്ലാ അക്കൗണ്ട് ഇടപാടുകളും സുരക്ഷിതമായി മറ്റൊരു ഫയലിലേക്കോ ഡ്രൈവിലേക്കോ ഇമെയിലിലേക്കോ സൂക്ഷിക്കാം.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }

        // Section 1: Create Backup
        Card(
            modifier = Modifier.fillMaxWidth().testTag("backup_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CashGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ബാക്കപ്പ് എടുക്കുക (Export Backup)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("JSON ഫോർമാറ്റിൽ മുഴുവൻ കണക്കുകളും സുരക്ഷിതമാക്കുക", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline, fontSize = 11.sp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.createBackup { jsonString ->
                            val timestamp = dateFormat.format(Date())
                            val filename = "Chirayil_Temple_Accounts_Backup_$timestamp"
                            ExportUtil.shareText(context, filename, jsonString)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                    modifier = Modifier.fillMaxWidth().testTag("create_backup_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share Backup File (JSON)")
                    }
                }
            }
        }

        // Section 2: Restore from Backup
        Card(
            modifier = Modifier.fillMaxWidth().testTag("restore_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ബാക്കപ്പ് പുനഃസ്ഥാപിക്കുക (Restore Backup)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("മുൻപ് എടുത്ത ബാക്കപ്പ് വിവരങ്ങൾ തിരികെ എത്തിക്കുക", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline, fontSize = 11.sp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = restoreJsonInput,
                    onValueChange = { restoreJsonInput = it },
                    label = { Text("Paste JSON Backup Data") },
                    placeholder = { Text("ബാക്കപ്പ് ഫയലിലെ കോഡ് ഇവിടെ പേസ്റ്റ് ചെയ്യുക...") },
                    maxLines = 6,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("restore_json_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            showRestoreConfirmDialog = true
                        } else {
                            viewModel.showMessage("ദയവായി ബാക്കപ്പ് ഡാറ്റ പേസ്റ്റ് ചെയ്യുക")
                        }
                    },
                    enabled = !isLoading && restoreJsonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.fillMaxWidth().testTag("restore_submit_button")
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Database Now")
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("ഡാറ്റാ പുനഃസ്ഥാപിക്കൽ മുന്നറിയിപ്പ് (Restore Warning)", fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = {
                Text(
                    "ശ്രദ്ധിക്കുക: ബാക്കപ്പ് വിവരങ്ങൾ പുനഃസ്ഥാപിക്കുമ്പോൾ നിലവിലെ ഡാറ്റാബേസിലെ വിവരങ്ങളിലേക്ക് ഇവ സംയോജിപ്പിക്കപ്പെടും. ഈ പ്രവർത്തനം ഓഡിറ്റ് ലോഗിൽ രേഖപ്പെടുത്തുന്നതാണ്. മുന്നോട്ട് പോകണോ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(restoreJsonInput)
                        showRestoreConfirmDialog = false
                        restoreJsonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("അതെ, പുനഃസ്ഥാപിക്കുക (Yes, Restore)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("റദ്ദാക്കുക (Cancel)")
                }
            }
        )
    }
}
