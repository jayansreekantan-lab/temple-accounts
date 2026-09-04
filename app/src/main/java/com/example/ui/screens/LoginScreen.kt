package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TempleConstants
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel

@Composable
fun LoginScreen(
    viewModel: TempleViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TempleMaroonDark,
                        TempleMaroon,
                        Color(0xFF2E090D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Temple Sacred Arch Logo
            Image(
                painter = painterResource(id = R.drawable.ic_temple_arch),
                contentDescription = "Temple Emblem",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .testTag("temple_login_logo")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Temple Title as required by prompt
            Text(
                text = TempleConstants.TEMPLE_NAME,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = TempleConstants.TEMPLE_LOCATION,
                style = MaterialTheme.typography.titleSmall,
                color = TempleGoldLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Login Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "സുരക്ഷിത ലോഗിൻ (Secure Login)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TempleMaroon
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (ഇമെയിൽ)") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TempleMaroon)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (പാസ്‌വേഡ്)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TempleMaroon)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login(email, password) { success ->
                                    if (success) onLoginSuccess()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                forgotEmail = email
                                showForgotPasswordDialog = true
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall.copy(color = TempleMaroon)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(email, password) { success ->
                                if (success) onLoginSuccess()
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Login (ലോഗിൻ)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Testing & Role Selection Section
                    Text(
                        text = "തത്സമയ റോൾ പരിശോധന (Direct Role Login)",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.outline)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.loginAsDemoUser(UserRole.TREASURER)
                                onLoginSuccess()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_treasurer")
                        ) {
                            Text("🛡️ Treasurer", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.loginAsDemoUser(UserRole.MEMBER)
                                onLoginSuccess()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_member")
                        ) {
                            Text("👤 Member", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = { Text("Forgot Password (പാസ്‌വേഡ് റീസെറ്റ്)") },
                text = {
                    Column {
                        Text("പാസ്‌വേഡ് മാറ്റാനുള്ള ലിങ്ക് ലഭിക്കാൻ താങ്കളുടെ ഇമെയിൽ നൽകുക:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.forgotPassword(forgotEmail)
                            showForgotPasswordDialog = false
                        }
                    ) {
                        Text("Send Reset Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
