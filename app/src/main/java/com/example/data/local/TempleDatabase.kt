package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        TransactionEntity::class,
        TransferEntity::class,
        FestivalEntity::class,
        SponsorEntity::class,
        OpeningBalanceEntity::class,
        AuditLogEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TempleDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun festivalDao(): FestivalDao
    abstract fun sponsorDao(): SponsorDao
    abstract fun openingBalanceDao(): OpeningBalanceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: TempleDatabase? = null

        fun getInstance(context: Context): TempleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TempleDatabase::class.java,
                    "chirayil_temple_accounts.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
