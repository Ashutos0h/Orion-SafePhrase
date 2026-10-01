package com.safephrase.data.local.db

import android.content.Context
import androidx.room.*
import com.safephrase.data.local.db.dao.*
import com.safephrase.data.local.db.entity.*

@Database(
    entities = [
        EmergencyEventEntity::class,
        TrustedContactEntity::class,
        SafePhraseEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SafePhraseDatabase : RoomDatabase() {
    abstract fun emergencyEventDao(): EmergencyEventDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun safePhraseDao(): SafePhraseDao

    companion object {
        @Volatile private var INSTANCE: SafePhraseDatabase? = null

        fun getInstance(context: Context): SafePhraseDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SafePhraseDatabase::class.java,
                    "safephrase.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
