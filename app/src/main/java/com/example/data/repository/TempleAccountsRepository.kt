package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.TempleDatabase
import com.example.data.local.entities.*
import com.example.data.remote.FirestoreSyncManager
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TempleAccountsRepository(
    private val context: Context,
    private val database: TempleDatabase,
    val syncManager: FirestoreSyncManager
) {
    private val tag = "TempleRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    // Current active logged in user state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        scope.launch {
            seedInitialDataIfNeeded()
        }
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val users = database.userDao().getAllUsersFlow().first()
        if (users.isEmpty()) {
            // Seed two initial authorized Treasurers
            val treasurer1 = UserEntity(
                id = "treasurer_1",
                name = "മുരളി കെ (Treasurer 1)",
                email = "treasurer1@chirayiltemple.org",
                role = UserRole.TREASURER.name,
                isActive = true
            )
            val treasurer2 = UserEntity(
                id = "treasurer_2",
                name = "സുരേഷ് ബാബു (Treasurer 2)",
                email = "treasurer2@chirayiltemple.org",
                role = UserRole.TREASURER.name,
                isActive = true
            )
            val member1 = UserEntity(
                id = "member_1",
                name = "വിജയൻ പിള്ള (Member)",
                email = "member@chirayiltemple.org",
                role = UserRole.MEMBER.name,
                isActive = true
            )
            database.userDao().insertAll(listOf(treasurer1, treasurer2, member1))

            // Default Opening Balance
            val opening = OpeningBalanceEntity(
                id = "MAIN_BALANCE",
                cashOpening = 25000.0,
                bankOpening = 150000.0,
                updatedBy = "System Initializer",
                updatedAt = System.currentTimeMillis(),
                syncStatus = "SYNCED"
            )
            database.openingBalanceDao().insertOrUpdate(opening)

            // Seed example festival as required: "തൃക്കൊടിയേറ്റ് മകരവിളക്ക് മഹോത്സവം 2027"
            val cal = Calendar.getInstance()
            cal.set(2027, Calendar.JANUARY, 5)
            val start = cal.timeInMillis
            cal.set(2027, Calendar.JANUARY, 15)
            val end = cal.timeInMillis
            val festival = FestivalEntity(
                id = "fest_makaravilakku_2027",
                name = "തൃക്കൊടിയേറ്റ് മകരവിളക്ക് മഹോത്സവം 2027",
                startDate = start,
                endDate = end,
                openingBalance = 50000.0,
                notes = "പ്രധാന വാർഷിക ഉത്സവം (Demo Initial Record)",
                createdBy = "മുരളി കെ",
                createdAt = System.currentTimeMillis(),
                syncStatus = "SYNCED"
            )
            database.festivalDao().insert(festival)

            // Initial Audit Log
            recordAudit(
                action = "INITIAL_SETUP",
                userEmail = "system@chirayiltemple.org",
                userName = "System",
                userRole = "SYSTEM",
                details = "Initialized Chirayil Temple Accounts database with opening balances and treasurers."
            )
        }
    }

    fun setCurrentUser(user: UserEntity?) {
        _currentUser.value = user
    }

    suspend fun login(email: String, pass: String): Result<UserEntity> {
        val result = syncManager.login(email, pass)
        result.onSuccess { user ->
            _currentUser.value = user
            recordAudit("LOGIN", user.email, user.name, user.role, details = "User logged in successfully")
            triggerBackgroundSync()
        }
        return result
    }

    suspend fun logout() {
        val user = _currentUser.value
        if (user != null) {
            recordAudit("LOGOUT", user.email, user.name, user.role, details = "User logged out")
        }
        syncManager.logout()
        _currentUser.value = null
    }

    fun triggerBackgroundSync() {
        scope.launch {
            _syncStatus.value = SyncStatus.PENDING_SYNC
            val result = syncManager.syncAll()
            _syncStatus.value = if (result.isSuccess) SyncStatus.SYNCED else SyncStatus.SYNC_FAILED
        }
    }

    // Transactions Flow
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactionsFlow()

    fun getIncomeTransactionsFlow(): Flow<List<TransactionEntity>> =
        database.transactionDao().getTransactionsByTypeFlow(TransactionType.INCOME.name)

    fun getExpenseTransactionsFlow(): Flow<List<TransactionEntity>> =
        database.transactionDao().getTransactionsByTypeFlow(TransactionType.EXPENSE.name)

    fun getAllTransfersFlow(): Flow<List<TransferEntity>> = database.transferDao().getAllTransfersFlow()

    fun getAllFestivalsFlow(): Flow<List<FestivalEntity>> = database.festivalDao().getAllFestivalsFlow()

    fun getAllSponsorsFlow(): Flow<List<SponsorEntity>> = database.sponsorDao().getAllSponsorsFlow()

    fun getOpeningBalanceFlow(): Flow<OpeningBalanceEntity?> = database.openingBalanceDao().getOpeningBalancesFlow()

    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>> = database.auditLogDao().getAllLogsFlow()

    fun getAllUsersFlow(): Flow<List<UserEntity>> = database.userDao().getAllUsersFlow()

    // Calculated Temple Balances
    val balancesFlow: Flow<TempleBalances> = combine(
        database.transactionDao().getAllTransactionsFlow(),
        database.transferDao().getAllTransfersFlow(),
        database.openingBalanceDao().getOpeningBalancesFlow(),
        database.festivalDao().getAllFestivalsFlow()
    ) { transactions, transfers, opening, festivals ->
        val openingCash = opening?.cashOpening ?: 0.0
        val openingBank = opening?.bankOpening ?: 0.0

        var totalIncome = 0.0
        var totalExpense = 0.0
        var cashIncome = 0.0
        var bankIncome = 0.0
        var cashExpense = 0.0
        var bankExpense = 0.0

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME.name) {
                totalIncome += tx.amount
                if (tx.paymentMode == PaymentMode.CASH.name) cashIncome += tx.amount else bankIncome += tx.amount
            } else if (tx.type == TransactionType.EXPENSE.name) {
                totalExpense += tx.amount
                if (tx.paymentMode == PaymentMode.CASH.name) cashExpense += tx.amount else bankExpense += tx.amount
            }
        }

        // Cash <-> Bank Transfers (NEVER counted as income or expense!)
        var transferCashDelta = 0.0
        var transferBankDelta = 0.0
        for (tr in transfers) {
            if (tr.transferType == TransferType.CASH_TO_BANK.name) {
                transferCashDelta -= tr.amount
                transferBankDelta += tr.amount
            } else if (tr.transferType == TransferType.BANK_TO_CASH.name) {
                transferCashDelta += tr.amount
                transferBankDelta -= tr.amount
            }
        }

        val cashBalance = openingCash + cashIncome - cashExpense + transferCashDelta
        val bankBalance = openingBank + bankIncome - bankExpense + transferBankDelta
        val netBalance = (cashBalance + bankBalance)

        // Latest or current festival balance
        val latestFestival = festivals.firstOrNull()
        var festBalance = 0.0
        var festName = ""
        if (latestFestival != null) {
            festName = latestFestival.name
            val festTxs = transactions.filter { it.festivalId == latestFestival.id }
            val fIncome = festTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val fExpense = festTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            festBalance = latestFestival.openingBalance + fIncome - fExpense
        }

        TempleBalances(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            cashBalance = cashBalance,
            bankBalance = bankBalance,
            openingCash = openingCash,
            openingBank = openingBank,
            currentFestivalBalance = festBalance,
            currentFestivalName = festName
        )
    }

    // Cash Book calculation with running balances
    val cashBookFlow: Flow<List<CashBookEntry>> = combine(
        database.transactionDao().getAllTransactionsFlow(),
        database.transferDao().getAllTransfersFlow(),
        database.openingBalanceDao().getOpeningBalancesFlow()
    ) { transactions, transfers, opening ->
        val openingCash = opening?.cashOpening ?: 0.0
        val entries = mutableListOf<CashBookEntry>()

        // Opening Cash Entry
        entries.add(
            CashBookEntry(
                id = "OPENING",
                date = 0L,
                dateFormatted = "Opening Balance",
                particulars = "Opening Cash Balance (തുടക്കത്തിലെ കാഷ്)",
                referenceOrVoucher = "-",
                receiptAmount = openingCash,
                paymentAmount = 0.0,
                balance = openingCash
            )
        )

        // Merge Cash Income, Cash Expense, and Cash Transfers
        val events = mutableListOf<Pair<Long, CashBookEntry>>()

        transactions.filter { it.paymentMode == PaymentMode.CASH.name }.forEach { tx ->
            val isIncome = tx.type == TransactionType.INCOME.name
            val entry = CashBookEntry(
                id = tx.id,
                date = tx.date,
                dateFormatted = tx.dateFormatted,
                particulars = "${tx.category} - ${tx.partyName.ifEmpty { tx.description }}",
                referenceOrVoucher = tx.voucherOrReceiptNo,
                receiptAmount = if (isIncome) tx.amount else 0.0,
                paymentAmount = if (!isIncome) tx.amount else 0.0,
                balance = 0.0
            )
            events.add(tx.date to entry)
        }

        transfers.forEach { tr ->
            val isCashToBank = tr.transferType == TransferType.CASH_TO_BANK.name
            val entry = CashBookEntry(
                id = tr.id,
                date = tr.date,
                dateFormatted = tr.dateFormatted,
                particulars = if (isCashToBank) "ബാങ്കിലേക്ക് അടച്ചത് (Cash deposited to Bank)" else "ബാങ്കിൽ നിന്നും പിൻവലിച്ചത് (Cash withdrawn from Bank)",
                referenceOrVoucher = tr.reference.ifEmpty { "TRF" },
                receiptAmount = if (!isCashToBank) tr.amount else 0.0,
                paymentAmount = if (isCashToBank) tr.amount else 0.0,
                balance = 0.0
            )
            events.add(tr.date to entry)
        }

        events.sortBy { it.first }

        var runningCash = openingCash
        val result = mutableListOf<CashBookEntry>()
        result.add(entries[0])

        events.forEach { (_, entry) ->
            runningCash = runningCash + entry.receiptAmount - entry.paymentAmount
            result.add(entry.copy(balance = runningCash))
        }

        result
    }

    // Bank Book calculation with running balances
    val bankBookFlow: Flow<List<BankBookEntry>> = combine(
        database.transactionDao().getAllTransactionsFlow(),
        database.transferDao().getAllTransfersFlow(),
        database.openingBalanceDao().getOpeningBalancesFlow()
    ) { transactions, transfers, opening ->
        val openingBank = opening?.bankOpening ?: 0.0
        val entries = mutableListOf<BankBookEntry>()

        entries.add(
            BankBookEntry(
                id = "OPENING",
                date = 0L,
                dateFormatted = "Opening Balance",
                particulars = "Opening Bank Balance (തുടക്കത്തിലെ ബാങ്ക് ബാലൻസ്)",
                referenceOrVoucher = "-",
                creditAmount = openingBank,
                debitAmount = 0.0,
                balance = openingBank
            )
        )

        val events = mutableListOf<Pair<Long, BankBookEntry>>()

        transactions.filter { it.paymentMode == PaymentMode.BANK.name }.forEach { tx ->
            val isIncome = tx.type == TransactionType.INCOME.name
            val entry = BankBookEntry(
                id = tx.id,
                date = tx.date,
                dateFormatted = tx.dateFormatted,
                particulars = "${tx.category} - ${tx.partyName.ifEmpty { tx.description }}",
                referenceOrVoucher = tx.voucherOrReceiptNo,
                creditAmount = if (isIncome) tx.amount else 0.0,
                debitAmount = if (!isIncome) tx.amount else 0.0,
                balance = 0.0
            )
            events.add(tx.date to entry)
        }

        transfers.forEach { tr ->
            val isCashToBank = tr.transferType == TransferType.CASH_TO_BANK.name
            val entry = BankBookEntry(
                id = tr.id,
                date = tr.date,
                dateFormatted = tr.dateFormatted,
                particulars = if (isCashToBank) "കാഷ് ബാങ്കിൽ നിക്ഷേപിച്ചത് (Cash deposited)" else "ബാങ്കിൽ നിന്നും കാഷ് എടുത്തത് (Cash withdrawn)",
                referenceOrVoucher = tr.reference.ifEmpty { "TRF" },
                creditAmount = if (isCashToBank) tr.amount else 0.0,
                debitAmount = if (!isCashToBank) tr.amount else 0.0,
                balance = 0.0
            )
            events.add(tr.date to entry)
        }

        events.sortBy { it.first }

        var runningBank = openingBank
        val result = mutableListOf<BankBookEntry>()
        result.add(entries[0])

        events.forEach { (_, entry) ->
            runningBank = runningBank + entry.creditAmount - entry.debitAmount
            result.add(entry.copy(balance = runningBank))
        }

        result
    }

    // Auto-generate Sequential Unique Receipt Number: R-2026-000001
    suspend fun generateNextReceiptNumber(): String = withContext(Dispatchers.IO) {
        val currentYear = yearFormat.format(Date())
        val prefix = "R-$currentYear-"
        val pattern = "$prefix%"
        val latest = database.transactionDao().getLatestNumberForPattern(TransactionType.INCOME.name, pattern)
        val nextSeq = if (latest != null && latest.startsWith(prefix)) {
            val numStr = latest.removePrefix(prefix)
            (numStr.toIntOrNull() ?: 0) + 1
        } else {
            1
        }
        String.format(Locale.US, "R-%s-%06d", currentYear, nextSeq)
    }

    // Auto-generate Sequential Unique Voucher Number: V-2026-000001
    suspend fun generateNextVoucherNumber(): String = withContext(Dispatchers.IO) {
        val currentYear = yearFormat.format(Date())
        val prefix = "V-$currentYear-"
        val pattern = "$prefix%"
        val latest = database.transactionDao().getLatestNumberForPattern(TransactionType.EXPENSE.name, pattern)
        val nextSeq = if (latest != null && latest.startsWith(prefix)) {
            val numStr = latest.removePrefix(prefix)
            (numStr.toIntOrNull() ?: 0) + 1
        } else {
            1
        }
        String.format(Locale.US, "V-%s-%06d", currentYear, nextSeq)
    }

    // Transaction actions (Income & Expense)
    suspend fun saveTransaction(tx: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (tx.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("തുക പൂജ്യത്തേക്കാൾ വലുതായിരിക്കണം (Amount must be greater than zero)"))
            }
            if (tx.partyName.isBlank() && tx.description.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("ആരിൽ നിന്ന് ലഭിച്ചു / ആർക്ക് നൽകി എന്നത് നിർബന്ധമാണ്"))
            }

            database.transactionDao().insert(tx)

            // If Sponsor category, also create or update sponsor record automatically
            if (tx.type == TransactionType.INCOME.name && tx.category.contains("Sponsor", ignoreCase = true)) {
                val sponsor = SponsorEntity(
                    id = UUID.randomUUID().toString(),
                    date = tx.date,
                    dateFormatted = tx.dateFormatted,
                    sponsorName = tx.partyName,
                    contact = "",
                    amount = tx.amount,
                    paymentMode = tx.paymentMode,
                    purpose = tx.description,
                    festivalId = tx.festivalId,
                    festivalName = tx.festivalName,
                    receiptNumber = tx.voucherOrReceiptNo,
                    notes = tx.notes,
                    createdBy = tx.createdBy,
                    createdAt = tx.createdAt,
                    syncStatus = "PENDING_SYNC"
                )
                database.sponsorDao().insert(sponsor)
            }

            val actionName = if (tx.type == TransactionType.INCOME.name) "ADD_INCOME" else "ADD_EXPENSE"
            recordAudit(
                action = actionName,
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = tx.id,
                details = "${tx.voucherOrReceiptNo}: ₹${tx.amount} (${tx.category}) - ${tx.partyName}"
            )

            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save transaction", e)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(tx: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionDao().update(tx.copy(updatedAt = System.currentTimeMillis(), syncStatus = "PENDING_SYNC"))
            recordAudit(
                action = if (tx.type == TransactionType.INCOME.name) "EDIT_INCOME" else "EDIT_EXPENSE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = tx.id,
                details = "Updated ${tx.voucherOrReceiptNo}: ₹${tx.amount} (${tx.category})"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionDao().delete(tx)
            recordAudit(
                action = if (tx.type == TransactionType.INCOME.name) "DELETE_INCOME" else "DELETE_EXPENSE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = tx.id,
                details = "Deleted ${tx.voucherOrReceiptNo}: ₹${tx.amount} (${tx.category})"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Transfers
    suspend fun saveTransfer(transfer: TransferEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (transfer.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("തുക പൂജ്യത്തേക്കാൾ വലുതായിരിക്കണം"))
            }
            database.transferDao().insert(transfer)
            recordAudit(
                action = "TRANSFER",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = transfer.id,
                details = "${transfer.transferType}: ₹${transfer.amount} Ref:${transfer.reference}"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransfer(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transferDao().deleteById(id)
            recordAudit(
                action = "DELETE_TRANSFER",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = id,
                details = "Deleted transfer id $id"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Festivals
    suspend fun saveFestival(festival: FestivalEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (festival.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("ഉത്സവത്തിന്റെ പേര് നൽകുക"))
            }
            database.festivalDao().insert(festival)
            recordAudit(
                action = "FESTIVAL_CREATE_UPDATE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = festival.id,
                details = "Festival: ${festival.name}, Opening: ₹${festival.openingBalance}"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFestival(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.festivalDao().deleteById(id)
            recordAudit(
                action = "DELETE_FESTIVAL",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = id,
                details = "Deleted festival id $id"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sponsors
    suspend fun saveSponsor(sponsor: SponsorEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (sponsor.sponsorName.isBlank() || sponsor.amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("ശരിയായ പേരും തുകയും നൽകുക"))
            }
            database.sponsorDao().insert(sponsor)
            recordAudit(
                action = "ADD_SPONSOR",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = sponsor.id,
                details = "Sponsor: ${sponsor.sponsorName}, ₹${sponsor.amount}"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSponsor(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.sponsorDao().deleteById(id)
            recordAudit(
                action = "DELETE_SPONSOR",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                recordId = id,
                details = "Deleted sponsor id $id"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Opening Balances
    suspend fun updateOpeningBalances(cash: Double, bank: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value
            val entity = OpeningBalanceEntity(
                id = "MAIN_BALANCE",
                cashOpening = cash,
                bankOpening = bank,
                updatedBy = user?.name ?: "Treasurer",
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING_SYNC"
            )
            database.openingBalanceDao().insertOrUpdate(entity)
            recordAudit(
                action = "OPENING_BALANCE_CHANGE",
                userEmail = user?.email ?: "",
                userName = user?.name ?: "",
                userRole = user?.role ?: "",
                details = "Updated Opening Balances - Cash: ₹$cash, Bank: ₹$bank"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User Management (Treasurer Only, enforce max 2 Treasurers!)
    suspend fun addUser(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (user.role == UserRole.TREASURER.name) {
                val activeTreasurers = database.userDao().getActiveTreasurersCount()
                if (activeTreasurers >= TempleConstants.MAX_TREASURERS) {
                    return@withContext Result.failure(
                        IllegalStateException("കൃത്യമായി 2 ട്രഷറർ അക്കൗണ്ടുകൾ മാത്രമേ അനുവദിക്കൂ (Maximum 2 Treasurers allowed)")
                    )
                }
            }
            database.userDao().insert(user)
            recordAudit(
                action = "USER_ADD",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                details = "Added user ${user.name} (${user.email}) as ${user.role}"
            )
            triggerBackgroundSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = database.userDao().getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
            if (newRole == UserRole.TREASURER.name && existing.role != UserRole.TREASURER.name) {
                val activeTreasurers = database.userDao().getActiveTreasurersCount()
                if (activeTreasurers >= TempleConstants.MAX_TREASURERS) {
                    return@withContext Result.failure(
                        IllegalStateException("2 ട്രഷറർമാർ ഇതിനകം ഉണ്ട്. മൂന്നാമതൊരാളെ ട്രഷറർ ആക്കാൻ കഴിയില്ല.")
                    )
                }
            }
            database.userDao().update(existing.copy(role = newRole, updatedAt = System.currentTimeMillis()))
            recordAudit(
                action = "USER_ROLE_CHANGE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                details = "Changed role of ${existing.name} to $newRole"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleUserActive(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = database.userDao().getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
            database.userDao().update(user.copy(isActive = !user.isActive, updatedAt = System.currentTimeMillis()))
            recordAudit(
                action = "USER_STATUS_CHANGE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                details = "Set user ${user.name} active to ${!user.isActive}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Audit Log recording
    suspend fun recordAudit(
        action: String,
        userEmail: String,
        userName: String,
        userRole: String,
        recordId: String? = null,
        details: String = ""
    ) {
        try {
            val log = AuditLogEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                action = action,
                userEmail = userEmail,
                userName = userName,
                userRole = userRole,
                recordId = recordId,
                details = details,
                syncStatus = "PENDING_SYNC"
            )
            database.auditLogDao().insert(log)
        } catch (e: Exception) {
            Log.e(tag, "Failed to record audit log", e)
        }
    }

    // Backup: Export entire database into JSON
    suspend fun createBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val txs = database.transactionDao().getAllTransactionsFlow().first()
            val trs = database.transferDao().getAllTransfersFlow().first()
            val fests = database.festivalDao().getAllFestivalsFlow().first()
            val sps = database.sponsorDao().getAllSponsorsFlow().first()
            val opening = database.openingBalanceDao().getOpeningBalances()

            val root = JSONObject()
            root.put("appName", TempleConstants.APP_NAME)
            root.put("templeName", TempleConstants.TEMPLE_NAME)
            root.put("backupVersion", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("backupDate", dateFormat.format(Date()))
            root.put("exportedBy", _currentUser.value?.name ?: "Treasurer")

            // Opening balances
            val opObj = JSONObject()
            opObj.put("cashOpening", opening?.cashOpening ?: 0.0)
            opObj.put("bankOpening", opening?.bankOpening ?: 0.0)
            root.put("openingBalances", opObj)

            // Transactions
            val txArray = JSONArray()
            txs.forEach { tx ->
                val o = JSONObject()
                o.put("id", tx.id)
                o.put("type", tx.type)
                o.put("voucherOrReceiptNo", tx.voucherOrReceiptNo)
                o.put("date", tx.date)
                o.put("dateFormatted", tx.dateFormatted)
                o.put("category", tx.category)
                o.put("description", tx.description)
                o.put("amount", tx.amount)
                o.put("paymentMode", tx.paymentMode)
                o.put("partyName", tx.partyName)
                o.put("festivalId", tx.festivalId ?: "")
                o.put("festivalName", tx.festivalName ?: "")
                o.put("notes", tx.notes)
                o.put("createdBy", tx.createdBy)
                o.put("createdAt", tx.createdAt)
                txArray.put(o)
            }
            root.put("transactions", txArray)

            // Transfers
            val trArray = JSONArray()
            trs.forEach { tr ->
                val o = JSONObject()
                o.put("id", tr.id)
                o.put("transferType", tr.transferType)
                o.put("date", tr.date)
                o.put("dateFormatted", tr.dateFormatted)
                o.put("amount", tr.amount)
                o.put("reference", tr.reference)
                o.put("notes", tr.notes)
                o.put("createdBy", tr.createdBy)
                o.put("createdAt", tr.createdAt)
                trArray.put(o)
            }
            root.put("transfers", trArray)

            // Festivals
            val festArray = JSONArray()
            fests.forEach { f ->
                val o = JSONObject()
                o.put("id", f.id)
                o.put("name", f.name)
                o.put("startDate", f.startDate)
                o.put("endDate", f.endDate)
                o.put("openingBalance", f.openingBalance)
                o.put("notes", f.notes)
                o.put("createdBy", f.createdBy)
                o.put("createdAt", f.createdAt)
                festArray.put(o)
            }
            root.put("festivals", festArray)

            // Sponsors
            val spArray = JSONArray()
            sps.forEach { s ->
                val o = JSONObject()
                o.put("id", s.id)
                o.put("date", s.date)
                o.put("dateFormatted", s.dateFormatted)
                o.put("sponsorName", s.sponsorName)
                o.put("contact", s.contact)
                o.put("amount", s.amount)
                o.put("paymentMode", s.paymentMode)
                o.put("purpose", s.purpose)
                o.put("festivalId", s.festivalId ?: "")
                o.put("festivalName", s.festivalName ?: "")
                o.put("receiptNumber", s.receiptNumber)
                o.put("notes", s.notes)
                spArray.put(o)
            }
            root.put("sponsors", spArray)

            val jsonString = root.toString(2)
            recordAudit(
                action = "BACKUP",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                details = "Exported accounting database backup (${txs.size} txs, ${trs.size} transfers)"
            )

            Result.success(jsonString)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create backup", e)
            Result.failure(e)
        }
    }

    // Restore from JSON
    suspend fun restoreFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("appName") || !root.has("transactions")) {
                return@withContext Result.failure(IllegalArgumentException("സാധുവായ ബാക്കപ്പ് ഫയലല്ല (Invalid backup file)"))
            }

            // Restore opening balance
            if (root.has("openingBalances")) {
                val op = root.getJSONObject("openingBalances")
                database.openingBalanceDao().insertOrUpdate(
                    OpeningBalanceEntity(
                        id = "MAIN_BALANCE",
                        cashOpening = op.optDouble("cashOpening", 0.0),
                        bankOpening = op.optDouble("bankOpening", 0.0),
                        updatedBy = _currentUser.value?.name ?: "Restored Backup",
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = "PENDING_SYNC"
                    )
                )
            }

            // Restore transactions
            val txArray = root.getJSONArray("transactions")
            val restoredTxs = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val o = txArray.getJSONObject(i)
                restoredTxs.add(
                    TransactionEntity(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        voucherOrReceiptNo = o.getString("voucherOrReceiptNo"),
                        date = o.getLong("date"),
                        dateFormatted = o.getString("dateFormatted"),
                        category = o.getString("category"),
                        description = o.optString("description", ""),
                        amount = o.getDouble("amount"),
                        paymentMode = o.getString("paymentMode"),
                        partyName = o.optString("partyName", ""),
                        festivalId = o.optString("festivalId").ifEmpty { null },
                        festivalName = o.optString("festivalName").ifEmpty { null },
                        notes = o.optString("notes", ""),
                        createdBy = o.optString("createdBy", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        syncStatus = "PENDING_SYNC"
                    )
                )
            }
            database.transactionDao().insertAll(restoredTxs)

            // Restore transfers
            if (root.has("transfers")) {
                val trArray = root.getJSONArray("transfers")
                val restoredTrs = mutableListOf<TransferEntity>()
                for (i in 0 until trArray.length()) {
                    val o = trArray.getJSONObject(i)
                    restoredTrs.add(
                        TransferEntity(
                            id = o.getString("id"),
                            transferType = o.getString("transferType"),
                            date = o.getLong("date"),
                            dateFormatted = o.getString("dateFormatted"),
                            amount = o.getDouble("amount"),
                            reference = o.optString("reference", ""),
                            notes = o.optString("notes", ""),
                            createdBy = o.optString("createdBy", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            syncStatus = "PENDING_SYNC"
                        )
                    )
                }
                database.transferDao().insertAll(restoredTrs)
            }

            // Restore festivals
            if (root.has("festivals")) {
                val festArray = root.getJSONArray("festivals")
                val restoredFests = mutableListOf<FestivalEntity>()
                for (i in 0 until festArray.length()) {
                    val o = festArray.getJSONObject(i)
                    restoredFests.add(
                        FestivalEntity(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            startDate = o.getLong("startDate"),
                            endDate = o.getLong("endDate"),
                            openingBalance = o.optDouble("openingBalance", 0.0),
                            notes = o.optString("notes", ""),
                            createdBy = o.optString("createdBy", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            syncStatus = "PENDING_SYNC"
                        )
                    )
                }
                database.festivalDao().insertAll(restoredFests)
            }

            recordAudit(
                action = "RESTORE",
                userEmail = _currentUser.value?.email ?: "",
                userName = _currentUser.value?.name ?: "",
                userRole = _currentUser.value?.role ?: "",
                details = "Restored database from backup (${restoredTxs.size} transactions)"
            )

            triggerBackgroundSync()
            Result.success("വിജയകരമായി പുനഃസ്ഥാപിച്ചു (${restoredTxs.size} ഇടപാടുകൾ)")
        } catch (e: Exception) {
            Log.e(tag, "Restore error", e)
            Result.failure(e)
        }
    }
}
