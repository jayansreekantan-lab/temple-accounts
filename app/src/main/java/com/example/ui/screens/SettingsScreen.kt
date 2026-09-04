package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TempleConstants
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: TempleViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    val openingBalances by viewModel.openingBalances.collectAsState()

    var cashOpeningStr by remember(openingBalances) {
        mutableStateOf(openingBalances?.cashOpening?.toString() ?: "25000")
    }
    var bankOpeningStr by remember(openingBalances) {
        mutableStateOf(openingBalances?.bankOpening?.toString() ?: "150000")
    }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Temple Info Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("temple_info_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_temple_arch),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = TempleConstants.TEMPLE_NAME,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TempleMaroon)
                    )
                    Text(
                        text = "സ്ഥാനം: ${TempleConstants.TEMPLE_LOCATION}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = TempleConstants.APP_NAME,
                        style = MaterialTheme.typography.labelSmall.copy(color = TempleGoldDark)
                    )
                }
            }
        }

        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ഉപയോക്തൃ വിവരങ്ങൾ (Logged-in User)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("പേര്: ${currentUser?.name ?: "Guest"}", style = MaterialTheme.typography.bodyMedium)
                Text("ഇമെയിൽ: ${currentUser?.email ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                Text("റോൾ: ${if (isTreasurer) "ട്രഷറർ (Treasurer - പൂർണ്ണാനുമതി)" else "അംഗം (Member - വായനാനുമതി മാത്രം)"}", style = MaterialTheme.typography.bodyMedium.copy(color = if (isTreasurer) TempleGoldDark else MaterialTheme.colorScheme.onSurface))

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        currentUser?.email?.let { viewModel.forgotPassword(it) }
                    }
                ) {
                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("പാസ്‌വേഡ് മാറ്റുക (Send Reset Email)")
                }
            }
        }

        // Opening Balances Configuration (Treasurer Only)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("opening_balances_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "തുടക്ക ബാലൻസുകൾ (Opening Balances)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isTreasurer) "ട്രഷറർക്ക് മാത്രമേ ഇത് മാറ്റാൻ കഴിയൂ" else "വായിക്കാൻ മാത്രം അനുമതി",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }

                    if (isTreasurer) {
                        IconButton(onClick = { showOpeningBalanceDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Opening Balances", tint = TempleMaroon)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("കാഷ് ഇൻ ഹാൻഡ് (Opening Cash):", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${String.format(Locale.US, "%,.2f", openingBalances?.cashOpening ?: 0.0)}", fontWeight = FontWeight.Bold, color = CashGreen)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ബാങ്ക് ബാലൻസ് (Opening Bank):", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${String.format(Locale.US, "%,.2f", openingBalances?.bankOpening ?: 0.0)}", fontWeight = FontWeight.Bold, color = BankBlue)
                }
            }
        }

        // Cloud Synchronization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ക്ലൗഡ് സിൻക്രോണൈസേഷൻ (Cloud Firestore Sync)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ഓഫ്‌ലൈനായി രേഖപ്പെടുത്തിയ ഇടപാടുകൾ തത്സമയം ക്ലൗഡ് ഫയർസ്റ്റോറിലേക്ക് അയക്കാനും മറ്റ് ട്രഷറർമാരുടെ എൻട്രികൾ ഡൗൺലോഡ് ചെയ്യാനും താഴെ ക്ലിക്ക് ചെയ്യുക.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.triggerSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync With Cloud Now")
                }
            }
        }

        // App Information & Version
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("പതിപ്പ് (App Version): 1.0.0 Production Native APK", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
                Text("ആർക്കിടെക്ചർ: MVVM + Repository + Room + Firestore", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
                Text("മലയാളം യൂണികോഡ് ഫോണ്ട് സപ്പോർട്ട് സജ്ജീകരിച്ചിരിക്കുന്നു.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
            }
        }
    }

    if (showOpeningBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showOpeningBalanceDialog = false },
            title = { Text("തുടക്ക ബാലൻസ് തിരുത്തുക (Edit Opening Balances)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cashOpeningStr,
                        onValueChange = { cashOpeningStr = it },
                        label = { Text("തുടക്ക കാഷ് ബാലൻസ് (Cash ₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bankOpeningStr,
                        onValueChange = { bankOpeningStr = it },
                        label = { Text("തുടക്ക ബാങ്ക് ബാലൻസ് (Bank ₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = cashOpeningStr.toDoubleOrNull() ?: 0.0
                        val b = bankOpeningStr.toDoubleOrNull() ?: 0.0
                        viewModel.updateOpeningBalances(c, b)
                        showOpeningBalanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon)
                ) {
                    Text("Save Balances")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }
}
