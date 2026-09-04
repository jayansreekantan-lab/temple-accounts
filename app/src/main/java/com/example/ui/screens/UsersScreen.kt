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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserEntity
import com.example.model.TempleConstants
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel

@Composable
fun UsersScreen(
    viewModel: TempleViewModel
) {
    val users by viewModel.users.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val activeTreasurersCount = users.count { it.role == UserRole.TREASURER.name && it.isActive }

    Scaffold(
        floatingActionButton = {
            if (isTreasurer) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TempleMaroon,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_user")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
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
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ഉപയോക്തൃ നിയന്ത്രണം (User Management)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "സുരക്ഷാ നിയമം: പരമാവധി ${TempleConstants.MAX_TREASURERS} ട്രഷറർമാർക്ക് മാത്രമേ പൂർണ്ണാനുമതി നൽകാൻ കഴിയൂ. നിലവിൽ $activeTreasurersCount / ${TempleConstants.MAX_TREASURERS} ട്രഷറർമാർ.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    val isUserTreasurer = user.role == UserRole.TREASURER.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_item_${user.email}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!user.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
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
                                    imageVector = if (isUserTreasurer) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isUserTreasurer) TempleGoldDark else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (!user.isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = ExpenseRed.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "നിഷ്‌ക്രിയം (Inactive)",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = ExpenseRed, fontSize = 9.sp),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                    )
                                    Text(
                                        text = if (isUserTreasurer) "റോൾ: ട്രഷറർ (Full Access)" else "റോൾ: അംഗം (Read Only)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isUserTreasurer) TempleGoldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            // Role toggle and Active toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle Role button
                                TextButton(
                                    onClick = {
                                        val newRole = if (isUserTreasurer) UserRole.MEMBER else UserRole.TREASURER
                                        viewModel.updateUserRole(user.id, newRole)
                                    }
                                ) {
                                    Text(if (isUserTreasurer) "Make Member" else "Make Treasurer", fontSize = 11.sp)
                                }

                                // Toggle Active status
                                Switch(
                                    checked = user.isActive,
                                    onCheckedChange = { viewModel.toggleUserActive(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            activeTreasurersCount = activeTreasurersCount,
            onDismiss = { showAddDialog = false },
            onSave = { name, email, role ->
                viewModel.addUser(name, email, role)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddUserDialog(
    activeTreasurersCount: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.MEMBER) }

    val canBeTreasurer = activeTreasurersCount < TempleConstants.MAX_TREASURERS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("പുതിയ ഉപയോക്താവ് (Add User)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("പേര് (Full Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_user_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ഇമെയിൽ വിലാസം (Email)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_user_email")
                )

                Text("റോൾ തിരഞ്ഞെടുക്കുക (Role):", style = MaterialTheme.typography.labelSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = role == UserRole.MEMBER,
                        onClick = { role = UserRole.MEMBER },
                        label = { Text("അംഗം (Member)") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = role == UserRole.TREASURER,
                        onClick = { if (canBeTreasurer) role = UserRole.TREASURER },
                        enabled = canBeTreasurer,
                        label = { Text("ട്രഷറർ (${activeTreasurersCount}/2)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!canBeTreasurer) {
                    Text(
                        text = "പരമാവധി 2 ട്രഷറർമാർ നിലവിലുണ്ട്. പുതിയ ഉപയോക്താവിനെ അംഗമായി മാത്രമേ ചേർക്കാൻ സാധിക്കൂ.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ExpenseRed, fontSize = 11.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email, role) },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.testTag("save_user_button")
            ) {
                Text("Add User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
