package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByTypeFlow(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE festivalId = :festivalId ORDER BY date DESC")
    fun getTransactionsByFestivalFlow(festivalId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT voucherOrReceiptNo FROM transactions WHERE type = :type AND voucherOrReceiptNo LIKE :pattern ORDER BY voucherOrReceiptNo DESC LIMIT 1")
    suspend fun getLatestNumberForPattern(type: String, pattern: String): String?

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC, createdAt DESC")
    fun getAllTransfersFlow(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<TransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<TransferEntity>)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transfers")
    suspend fun clearAll()
}

@Dao
interface FestivalDao {
    @Query("SELECT * FROM festivals ORDER BY startDate DESC")
    fun getAllFestivalsFlow(): Flow<List<FestivalEntity>>

    @Query("SELECT * FROM festivals WHERE id = :id")
    suspend fun getFestivalById(id: String): FestivalEntity?

    @Query("SELECT * FROM festivals WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<FestivalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(festival: FestivalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(festivals: List<FestivalEntity>)

    @Query("DELETE FROM festivals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM festivals")
    suspend fun clearAll()
}

@Dao
interface SponsorDao {
    @Query("SELECT * FROM sponsors ORDER BY date DESC, createdAt DESC")
    fun getAllSponsorsFlow(): Flow<List<SponsorEntity>>

    @Query("SELECT * FROM sponsors WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<SponsorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sponsor: SponsorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sponsors: List<SponsorEntity>)

    @Query("DELETE FROM sponsors WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sponsors")
    suspend fun clearAll()
}

@Dao
interface OpeningBalanceDao {
    @Query("SELECT * FROM opening_balances WHERE id = 'MAIN_BALANCE' LIMIT 1")
    fun getOpeningBalancesFlow(): Flow<OpeningBalanceEntity?>

    @Query("SELECT * FROM opening_balances WHERE id = 'MAIN_BALANCE' LIMIT 1")
    suspend fun getOpeningBalances(): OpeningBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(balance: OpeningBalanceEntity)

    @Query("DELETE FROM opening_balances")
    suspend fun clearAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE role = 'TREASURER' AND isActive = 1")
    suspend fun getActiveTreasurersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}
