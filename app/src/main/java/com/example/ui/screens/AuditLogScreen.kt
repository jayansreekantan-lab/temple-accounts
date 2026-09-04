package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditLogScreen(
    viewModel: TempleViewModel
) {
    val logs by viewModel.auditLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter {
            it.action.contains(searchQuery, ignoreCase = true) ||
            it.userName.contains(searchQuery, ignoreCase = true) ||
            it.userEmail.contains(searchQuery, ignoreCase = true) ||
            it.details.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "സുരക്ഷാ ഓഡിറ്റ് ലോഗ് (Security Audit Trail)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "എല്ലാ പ്രധാന പ്രവർത്തനങ്ങളും തീയതി, സമയം, ഉപയോക്താവ് സഹിതം മാറ്റമില്ലാതെ രേഖപ്പെടുത്തുന്നു.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter audit logs by user, action, details...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("audit_search_input")
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("ഓഡിറ്റ് രേഖകൾ ലഭ്യമല്ല", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("audit_item_${log.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = when {
                                        log.action.contains("DELETE") -> ExpenseRed.copy(alpha = 0.15f)
                                        log.action.contains("ADD") || log.action.contains("INITIAL") -> CashGreen.copy(alpha = 0.15f)
                                        else -> BankBlue.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                log.action.contains("DELETE") -> ExpenseRed
                                                log.action.contains("ADD") || log.action.contains("INITIAL") -> CashGreen
                                                else -> BankBlue
                                            }
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = log.details.ifEmpty { "Activity recorded" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "By: ${log.userName} (${log.userEmail}) • Role: ${log.userRole}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
