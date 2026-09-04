package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("type"),
        Index("date"),
        Index("category"),
        Index("paymentMode"),
        Index("voucherOrReceiptNo", unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // INCOME or EXPENSE
    val voucherOrReceiptNo: String,
    val date: Long,
    val dateFormatted: String,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMode: String, // CASH or BANK
    val partyName: String, // Received From or Paid To
    val festivalId: String? = null,
    val festivalName: String? = null,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(
    tableName = "transfers",
    indices = [
        Index("date")
    ]
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val transferType: String, // CASH_TO_BANK or BANK_TO_CASH
    val date: Long,
    val dateFormatted: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(
    tableName = "festivals",
    indices = [
        Index("name")
    ]
)
data class FestivalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val openingBalance: Double = 0.0,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(
    tableName = "sponsors",
    indices = [
        Index("sponsorName"),
        Index("date"),
        Index("receiptNumber")
    ]
)
data class SponsorEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val dateFormatted: String,
    val sponsorName: String,
    val contact: String = "",
    val amount: Double,
    val paymentMode: String, // CASH or BANK
    val purpose: String = "",
    val festivalId: String? = null,
    val festivalName: String? = null,
    val receiptNumber: String = "",
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(tableName = "opening_balances")
data class OpeningBalanceEntity(
    @PrimaryKey val id: String = "MAIN_BALANCE",
    val cashOpening: Double = 0.0,
    val bankOpening: Double = 0.0,
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index("timestamp"),
        Index("action")
    ]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val userEmail: String,
    val userName: String,
    val userRole: String,
    val recordId: String? = null,
    val details: String = "",
    val syncStatus: String = "PENDING_SYNC"
)

@Entity(
    tableName = "users",
    indices = [
        Index("email", unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // TREASURER or MEMBER
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
