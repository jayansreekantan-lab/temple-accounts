package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TempleDatabase
import com.example.data.local.entities.*
import com.example.data.remote.FirestoreSyncManager
import com.example.data.repository.TempleAccountsRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TempleViewModel(application: Application) : AndroidViewModel(application) {

    val database = TempleDatabase.getInstance(application)
    val syncManager = FirestoreSyncManager(database)
    val repository = TempleAccountsRepository(application, database, syncManager)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Current User & Role
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val isTreasurer: StateFlow<Boolean> = repository.currentUser
        .map { it?.role == UserRole.TREASURER.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Balances
    val balances: StateFlow<TempleBalances> = repository.balancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TempleBalances())

    // All data streams
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeTransactions: StateFlow<List<TransactionEntity>> = repository.getIncomeTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseTransactions: StateFlow<List<TransactionEntity>> = repository.getExpenseTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashBookEntries: StateFlow<List<CashBookEntry>> = repository.cashBookFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankBookEntries: StateFlow<List<BankBookEntry>> = repository.bankBookFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transfers: StateFlow<List<TransferEntity>> = repository.getAllTransfersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val festivals: StateFlow<List<FestivalEntity>> = repository.getAllFestivalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sponsors: StateFlow<List<SponsorEntity>> = repository.getAllSponsorsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openingBalances: StateFlow<OpeningBalanceEntity?> = repository.getOpeningBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    // UI Feedback State
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    // Login Action
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiMessage.value = "ദയവായി ഇമെയിലും പാസ്‌വേഡും നൽകുക"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.login(email.trim(), pass.trim())
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "സ്വാഗതം, ${it.name} (${it.role})"
                onResult(true)
            }.onFailure { err ->
                // Check if user exists locally for offline login or credentials failure
                val localUser = database.userDao().getUserByEmail(email.trim())
                if (localUser != null) {
                    repository.setCurrentUser(localUser)
                    _uiMessage.value = "സ്വാഗതം, ${localUser.name} (ഓഫ്‌ലൈൻ ലോഗിൻ)"
                    onResult(true)
                } else {
                    _uiMessage.value = "ലോഗിൻ പരാജയപ്പെട്ടു: ${err.localizedMessage ?: "ഇമെയിലോ പാസ്‌വേഡോ തെറ്റാണ്"}"
                    onResult(false)
                }
            }
        }
    }

    // Direct role selection for offline testing / rapid sign-in
    fun loginAsDemoUser(role: UserRole) {
        viewModelScope.launch {
            val user = if (role == UserRole.TREASURER) {
                UserEntity(
                    id = "treasurer_1",
                    name = "മുരളി കെ (Treasurer)",
                    email = "treasurer1@chirayiltemple.org",
                    role = UserRole.TREASURER.name,
                    isActive = true
                )
            } else {
                UserEntity(
                    id = "member_1",
                    name = "വിജയൻ പിള്ള (Member)",
                    email = "member@chirayiltemple.org",
                    role = UserRole.MEMBER.name,
                    isActive = true
                )
            }
            database.userDao().insert(user)
            repository.setCurrentUser(user)
            repository.recordAudit("LOGIN", user.email, user.name, user.role, details = "Logged in as ${user.role}")
            _uiMessage.value = "${user.name} ആയി ലോഗിൻ ചെയ്തു"
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiMessage.value = "ദയവായി ഇമെയിൽ വിലാസം നൽകുക"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.syncManager.sendPasswordReset(email.trim())
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "പാസ്‌വേഡ് റീസെറ്റ് ലിങ്ക് ഇമെയിലിൽ അയച്ചിട്ടുണ്ട്"
            }.onFailure {
                _uiMessage.value = "റീസെറ്റ് ലിങ്ക് അയക്കാൻ കഴിഞ്ഞില്ല: ${it.localizedMessage}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiMessage.value = "ലോഗ് ഔട്ട് ചെയ്തു"
        }
    }

    fun triggerSync() {
        repository.triggerBackgroundSync()
    }

    // Save Income
    fun addIncome(
        date: Long,
        category: String,
        description: String,
        amount: Double,
        paymentMode: PaymentMode,
        receivedFrom: String,
        festival: FestivalEntity?,
        notes: String,
        onSuccess: (TransactionEntity) -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.value = "തുക പൂജ്യത്തേക്കാൾ വലുതായിരിക്കണം"
                return@launch
            }
            if (receivedFrom.isBlank() && description.isBlank()) {
                _uiMessage.value = "ലഭിച്ച ആളുടെ പേര് അല്ലെങ്കിൽ വിവരണം നൽകുക"
                return@launch
            }
            val receiptNo = repository.generateNextReceiptNumber()
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = TransactionType.INCOME.name,
                voucherOrReceiptNo = receiptNo,
                date = date,
                dateFormatted = dateFormat.format(Date(date)),
                category = category,
                description = description,
                amount = amount,
                paymentMode = paymentMode.name,
                partyName = receivedFrom.trim(),
                festivalId = festival?.id,
                festivalName = festival?.name,
                notes = notes.trim(),
                createdBy = currentUser.value?.name ?: "Treasurer",
                createdAt = System.currentTimeMillis()
            )
            val res = repository.saveTransaction(tx)
            res.onSuccess {
                _uiMessage.value = "വരുമാനം രേഖപ്പെടുത്തി (രസീത്: $receiptNo)"
                onSuccess(tx)
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "സേവ് ചെയ്യാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    // Save Expense
    fun addExpense(
        date: Long,
        category: String,
        description: String,
        amount: Double,
        paymentMode: PaymentMode,
        paidTo: String,
        festival: FestivalEntity?,
        notes: String,
        onSuccess: (TransactionEntity) -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.value = "തുക പൂജ്യത്തേക്കാൾ വലുതായിരിക്കണം"
                return@launch
            }
            if (paidTo.isBlank() && description.isBlank()) {
                _uiMessage.value = "ആർക്ക് നൽകി അല്ലെങ്കിൽ വിവരണം നൽകുക"
                return@launch
            }
            val voucherNo = repository.generateNextVoucherNumber()
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE.name,
                voucherOrReceiptNo = voucherNo,
                date = date,
                dateFormatted = dateFormat.format(Date(date)),
                category = category,
                description = description,
                amount = amount,
                paymentMode = paymentMode.name,
                partyName = paidTo.trim(),
                festivalId = festival?.id,
                festivalName = festival?.name,
                notes = notes.trim(),
                createdBy = currentUser.value?.name ?: "Treasurer",
                createdAt = System.currentTimeMillis()
            )
            val res = repository.saveTransaction(tx)
            res.onSuccess {
                _uiMessage.value = "ചെലവ് രേഖപ്പെടുത്തി (വൗച്ചർ: $voucherNo)"
                onSuccess(tx)
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "സേവ് ചെയ്യാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val res = repository.updateTransaction(tx)
            res.onSuccess {
                _uiMessage.value = "വിജയകരമായി തിരുത്തി"
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "തിരുത്താൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val res = repository.deleteTransaction(tx)
            res.onSuccess {
                _uiMessage.value = "ഇടപാട് നീക്കം ചെയ്തു"
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "നീക്കം ചെയ്യാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    // Save Transfer
    fun addTransfer(
        transferType: TransferType,
        date: Long,
        amount: Double,
        reference: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.value = "തുക പൂജ്യത്തേക്കാൾ വലുതായിരിക്കണം"
                return@launch
            }
            val transfer = TransferEntity(
                id = UUID.randomUUID().toString(),
                transferType = transferType.name,
                date = date,
                dateFormatted = dateFormat.format(Date(date)),
                amount = amount,
                reference = reference.trim(),
                notes = notes.trim(),
                createdBy = currentUser.value?.name ?: "Treasurer",
                createdAt = System.currentTimeMillis()
            )
            val res = repository.saveTransfer(transfer)
            res.onSuccess {
                _uiMessage.value = "ട്രാൻസ്ഫർ രേഖപ്പെടുത്തി (₹$amount)"
                onSuccess()
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "ട്രാൻസ്ഫർ പരാജയപ്പെട്ടു"
            }
        }
    }

    fun deleteTransfer(id: String) {
        viewModelScope.launch {
            repository.deleteTransfer(id)
            _uiMessage.value = "ട്രാൻസ്ഫർ നീക്കം ചെയ്തു"
        }
    }

    // Festivals
    fun addFestival(
        name: String,
        startDate: Long,
        endDate: Long,
        openingBalance: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiMessage.value = "ഉത്സവത്തിന്റെ പേര് നൽകുക"
                return@launch
            }
            val festival = FestivalEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                startDate = startDate,
                endDate = endDate,
                openingBalance = openingBalance,
                notes = notes.trim(),
                createdBy = currentUser.value?.name ?: "Treasurer",
                createdAt = System.currentTimeMillis()
            )
            val res = repository.saveFestival(festival)
            res.onSuccess {
                _uiMessage.value = "ഉത്സവം ചേർത്തു: $name"
                onSuccess()
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "ഉത്സവം ചേർക്കാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    fun deleteFestival(id: String) {
        viewModelScope.launch {
            repository.deleteFestival(id)
            _uiMessage.value = "ഉത്സവ അക്കൗണ്ട് നീക്കം ചെയ്തു"
        }
    }

    // Sponsors
    fun addSponsor(
        name: String,
        contact: String,
        amount: Double,
        paymentMode: PaymentMode,
        purpose: String,
        festival: FestivalEntity?,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || amount <= 0) {
                _uiMessage.value = "ശരിയായ പേരും തുകയും നൽകുക"
                return@launch
            }
            val receiptNo = repository.generateNextReceiptNumber()
            val sponsor = SponsorEntity(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                dateFormatted = dateFormat.format(Date()),
                sponsorName = name.trim(),
                contact = contact.trim(),
                amount = amount,
                paymentMode = paymentMode.name,
                purpose = purpose.trim(),
                festivalId = festival?.id,
                festivalName = festival?.name,
                receiptNumber = receiptNo,
                notes = notes.trim(),
                createdBy = currentUser.value?.name ?: "Treasurer",
                createdAt = System.currentTimeMillis()
            )
            val res = repository.saveSponsor(sponsor)
            res.onSuccess {
                _uiMessage.value = "സ്പോൺസർഷിപ്പ് ചേർത്തു ($name: ₹$amount)"
                onSuccess()
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "ചേർക്കാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    fun deleteSponsor(id: String) {
        viewModelScope.launch {
            repository.deleteSponsor(id)
            _uiMessage.value = "സ്പോൺസർ റെക്കോർഡ് നീക്കം ചെയ്തു"
        }
    }

    // Opening Balances
    fun updateOpeningBalances(cash: Double, bank: Double) {
        viewModelScope.launch {
            val res = repository.updateOpeningBalances(cash, bank)
            res.onSuccess {
                _uiMessage.value = "ഓപ്പണിംഗ് ബാലൻസുകൾ മാറ്റി നൽകി"
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "മാറ്റാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    // Users
    fun addUser(name: String, email: String, role: UserRole) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank()) {
                _uiMessage.value = "പേരും ഇമെയിലും നൽകുക"
                return@launch
            }
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                email = email.trim().lowercase(),
                role = role.name,
                isActive = true
            )
            val res = repository.addUser(user)
            res.onSuccess {
                _uiMessage.value = "ഉപയോക്താവിനെ ചേർത്തു ($name - ${role.displayName})"
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "ഉപയോക്താവിനെ ചേർക്കാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    fun toggleUserActive(userId: String) {
        viewModelScope.launch {
            repository.toggleUserActive(userId)
        }
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            val res = repository.updateUserRole(userId, newRole.name)
            res.onSuccess {
                _uiMessage.value = "റോൾ മാറ്റി നൽകി"
            }.onFailure {
                _uiMessage.value = it.localizedMessage ?: "റോൾ മാറ്റാൻ കഴിഞ്ഞില്ല"
            }
        }
    }

    // Backup & Restore
    fun createBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createBackupJson()
            _isLoading.value = false
            res.onSuccess { json ->
                _uiMessage.value = "ബാക്കപ്പ് ഫയൽ തയ്യാറായി"
                onReady(json)
            }.onFailure {
                _uiMessage.value = "ബാക്കപ്പ് പരാജയപ്പെട്ടു: ${it.localizedMessage}"
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.restoreFromJson(jsonString)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = it
            }.onFailure {
                _uiMessage.value = "റീസ്റ്റോർ പരാജയപ്പെട്ടു: ${it.localizedMessage}"
            }
        }
    }
}
