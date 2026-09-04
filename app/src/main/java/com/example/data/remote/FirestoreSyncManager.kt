package com.example.data.remote

import android.util.Log
import com.example.data.local.TempleDatabase
import com.example.data.local.entities.*
import com.example.model.SyncStatus
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager(
    private val database: TempleDatabase
) {
    private val tag = "FirestoreSyncManager"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // User authentication
    suspend fun login(email: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Authentication returned no user id")
            
            // Fetch user profile from Firestore or local
            var userEntity = database.userDao().getUserById(uid)
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val roleStr = doc.getString("role") ?: UserRole.MEMBER.name
                    val name = doc.getString("name") ?: email.substringBefore("@")
                    val isActive = doc.getBoolean("isActive") ?: true
                    userEntity = UserEntity(
                        id = uid,
                        name = name,
                        email = email,
                        role = roleStr,
                        isActive = isActive
                    )
                    database.userDao().insert(userEntity)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to fetch remote user profile, using local or fallback", e)
            }

            if (userEntity == null) {
                // If this is one of the initial treasurers or default users
                val defaultRole = if (email.contains("treasurer", ignoreCase = true) || email.contains("admin", ignoreCase = true)) {
                    UserRole.TREASURER.name
                } else {
                    UserRole.MEMBER.name
                }
                userEntity = UserEntity(
                    id = uid,
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    role = defaultRole,
                    isActive = true
                )
                database.userDao().insert(userEntity)
            }

            Result.success(userEntity)
        } catch (e: Exception) {
            Log.e(tag, "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Logout error", e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Two-way synchronization between Room and Firestore
    suspend fun syncAll(): Result<SyncStatus> = withContext(Dispatchers.IO) {
        try {
            // 1. Push pending local transactions
            val pendingTx = database.transactionDao().getPendingSync()
            for (tx in pendingTx) {
                try {
                    val map = hashMapOf(
                        "id" to tx.id,
                        "type" to tx.type,
                        "voucherOrReceiptNo" to tx.voucherOrReceiptNo,
                        "date" to tx.date,
                        "dateFormatted" to tx.dateFormatted,
                        "category" to tx.category,
                        "description" to tx.description,
                        "amount" to tx.amount,
                        "paymentMode" to tx.paymentMode,
                        "partyName" to tx.partyName,
                        "festivalId" to (tx.festivalId ?: ""),
                        "festivalName" to (tx.festivalName ?: ""),
                        "notes" to tx.notes,
                        "createdBy" to tx.createdBy,
                        "createdAt" to tx.createdAt,
                        "updatedBy" to tx.updatedBy,
                        "updatedAt" to tx.updatedAt
                    )
                    firestore.collection("transactions").document(tx.id).set(map, SetOptions.merge()).await()
                    database.transactionDao().update(tx.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing tx ${tx.id}", e)
                }
            }

            // 2. Push pending transfers
            val pendingTransfers = database.transferDao().getPendingSync()
            for (tr in pendingTransfers) {
                try {
                    val map = hashMapOf(
                        "id" to tr.id,
                        "transferType" to tr.transferType,
                        "date" to tr.date,
                        "dateFormatted" to tr.dateFormatted,
                        "amount" to tr.amount,
                        "reference" to tr.reference,
                        "notes" to tr.notes,
                        "createdBy" to tr.createdBy,
                        "createdAt" to tr.createdAt
                    )
                    firestore.collection("transfers").document(tr.id).set(map, SetOptions.merge()).await()
                    database.transferDao().insert(tr.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing transfer ${tr.id}", e)
                }
            }

            // 3. Push pending festivals
            val pendingFestivals = database.festivalDao().getPendingSync()
            for (fest in pendingFestivals) {
                try {
                    val map = hashMapOf(
                        "id" to fest.id,
                        "name" to fest.name,
                        "startDate" to fest.startDate,
                        "endDate" to fest.endDate,
                        "openingBalance" to fest.openingBalance,
                        "notes" to fest.notes,
                        "createdBy" to fest.createdBy,
                        "createdAt" to fest.createdAt
                    )
                    firestore.collection("festivals").document(fest.id).set(map, SetOptions.merge()).await()
                    database.festivalDao().insert(fest.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing festival ${fest.id}", e)
                }
            }

            // 4. Push pending sponsors
            val pendingSponsors = database.sponsorDao().getPendingSync()
            for (sp in pendingSponsors) {
                try {
                    val map = hashMapOf(
                        "id" to sp.id,
                        "date" to sp.date,
                        "dateFormatted" to sp.dateFormatted,
                        "sponsorName" to sp.sponsorName,
                        "contact" to sp.contact,
                        "amount" to sp.amount,
                        "paymentMode" to sp.paymentMode,
                        "purpose" to sp.purpose,
                        "festivalId" to (sp.festivalId ?: ""),
                        "festivalName" to (sp.festivalName ?: ""),
                        "receiptNumber" to sp.receiptNumber,
                        "notes" to sp.notes,
                        "createdBy" to sp.createdBy,
                        "createdAt" to sp.createdAt
                    )
                    firestore.collection("sponsors").document(sp.id).set(map, SetOptions.merge()).await()
                    database.sponsorDao().insert(sp.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing sponsor ${sp.id}", e)
                }
            }

            // 5. Push opening balance if pending
            val opening = database.openingBalanceDao().getOpeningBalances()
            if (opening != null && opening.syncStatus != "SYNCED") {
                try {
                    val map = hashMapOf(
                        "id" to opening.id,
                        "cashOpening" to opening.cashOpening,
                        "bankOpening" to opening.bankOpening,
                        "updatedBy" to opening.updatedBy,
                        "updatedAt" to opening.updatedAt
                    )
                    firestore.collection("opening_balances").document(opening.id).set(map, SetOptions.merge()).await()
                    database.openingBalanceDao().insertOrUpdate(opening.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing opening balance", e)
                }
            }

            // 6. Push pending audit logs
            val pendingLogs = database.auditLogDao().getPendingSync()
            for (logItem in pendingLogs) {
                try {
                    val map = hashMapOf(
                        "id" to logItem.id,
                        "timestamp" to logItem.timestamp,
                        "action" to logItem.action,
                        "userEmail" to logItem.userEmail,
                        "userName" to logItem.userName,
                        "userRole" to logItem.userRole,
                        "recordId" to (logItem.recordId ?: ""),
                        "details" to logItem.details
                    )
                    firestore.collection("audit_logs").document(logItem.id).set(map, SetOptions.merge()).await()
                    database.auditLogDao().insert(logItem.copy(syncStatus = "SYNCED"))
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing audit log ${logItem.id}", e)
                }
            }

            // 7. Pull remote transactions from Firestore
            try {
                val txSnapshot = firestore.collection("transactions").limit(1000).get().await()
                for (doc in txSnapshot.documents) {
                    val tx = TransactionEntity(
                        id = doc.id,
                        type = doc.getString("type") ?: "INCOME",
                        voucherOrReceiptNo = doc.getString("voucherOrReceiptNo") ?: "",
                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                        dateFormatted = doc.getString("dateFormatted") ?: "",
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        paymentMode = doc.getString("paymentMode") ?: "CASH",
                        partyName = doc.getString("partyName") ?: "",
                        festivalId = doc.getString("festivalId"),
                        festivalName = doc.getString("festivalName"),
                        notes = doc.getString("notes") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedBy = doc.getString("updatedBy") ?: "",
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        syncStatus = "SYNCED"
                    )
                    database.transactionDao().insert(tx)
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not pull remote transactions", e)
            }

            // Pull remote festivals
            try {
                val festSnapshot = firestore.collection("festivals").get().await()
                for (doc in festSnapshot.documents) {
                    val fest = FestivalEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        startDate = doc.getLong("startDate") ?: 0L,
                        endDate = doc.getLong("endDate") ?: 0L,
                        openingBalance = doc.getDouble("openingBalance") ?: 0.0,
                        notes = doc.getString("notes") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        syncStatus = "SYNCED"
                    )
                    database.festivalDao().insert(fest)
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not pull festivals", e)
            }

            Result.success(SyncStatus.SYNCED)
        } catch (e: Exception) {
            Log.e(tag, "Sync failed with error", e)
            Result.failure(e)
        }
    }
}
