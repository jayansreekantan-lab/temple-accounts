package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entities.TransactionEntity
import com.example.ui.components.TempleNavDrawerContent
import com.example.ui.components.TempleTopAppBar
import com.example.ui.screens.*
import com.example.ui.viewmodel.TempleViewModel
import kotlinx.coroutines.launch

@Composable
fun TempleNavHost(
    viewModel: TempleViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    var currentRoute by remember { mutableStateOf(TempleNavRoute.DASHBOARD) }
    var viewingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // React to UI messages
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    // Intercept Back Press
    BackHandler(enabled = drawerState.isOpen || viewingTransaction != null || currentRoute != TempleNavRoute.DASHBOARD) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (viewingTransaction != null) {
            viewingTransaction = null
        } else if (currentRoute != TempleNavRoute.DASHBOARD) {
            currentRoute = TempleNavRoute.DASHBOARD
        }
    }

    // Unauthenticated State: Show Login Screen
    if (currentUser == null) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                currentRoute = TempleNavRoute.DASHBOARD
            }
        )
        return
    }

    // Authenticated App Shell with Navigation Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = viewingTransaction == null,
        drawerContent = {
            TempleNavDrawerContent(
                currentRoute = currentRoute,
                currentUser = currentUser,
                isTreasurer = isTreasurer,
                syncStatus = syncStatus,
                onNavigate = { route ->
                    viewingTransaction = null
                    currentRoute = route
                    scope.launch { drawerState.close() }
                },
                onSyncClick = {
                    viewModel.triggerSync()
                    scope.launch { drawerState.close() }
                },
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    viewModel.logout()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TempleTopAppBar(
                    title = if (viewingTransaction != null) {
                        if (viewingTransaction?.type == "INCOME") "രസീത് (Receipt)" else "വൗച്ചർ (Voucher)"
                    } else {
                        currentRoute.titleMalayalam
                    },
                    currentUser = currentUser,
                    isTreasurer = isTreasurer,
                    syncStatus = syncStatus,
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onSyncClick = { viewModel.triggerSync() }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                if (viewingTransaction != null) {
                    ReceiptScreen(
                        transaction = viewingTransaction!!,
                        onBack = { viewingTransaction = null }
                    )
                } else {
                    when (currentRoute) {
                        TempleNavRoute.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { route -> currentRoute = route },
                            onViewReceipt = { tx -> viewingTransaction = tx }
                        )
                        TempleNavRoute.INCOME -> IncomeScreen(
                            viewModel = viewModel,
                            onViewReceipt = { tx -> viewingTransaction = tx }
                        )
                        TempleNavRoute.EXPENSE -> ExpenseScreen(
                            viewModel = viewModel,
                            onViewVoucher = { tx -> viewingTransaction = tx }
                        )
                        TempleNavRoute.CASH_BOOK -> CashBookScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.BANK_BOOK -> BankBookScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.TRANSFERS -> TransfersScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.FESTIVALS -> FestivalsScreen(
                            viewModel = viewModel,
                            onViewReceipt = { tx -> viewingTransaction = tx }
                        )
                        TempleNavRoute.SPONSORS -> SponsorsScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.REPORTS -> ReportsScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.USERS -> UsersScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.BACKUP_RESTORE -> BackupRestoreScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.AUDIT_LOG -> AuditLogScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.SETTINGS -> SettingsScreen(
                            viewModel = viewModel
                        )
                        TempleNavRoute.LOGIN -> {
                            // Handled by currentUser == null check
                        }
                    }
                }
            }
        }
    }
}
