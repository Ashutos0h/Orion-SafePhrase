package com.safephrase.data.local.db.dao

import androidx.room.*
import com.safephrase.data.local.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EmergencyEventEntity)

    @Update
    suspend fun update(event: EmergencyEventEntity)

    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<EmergencyEventEntity>>

    @Query("SELECT * FROM emergency_events WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY timestamp ASC")
    suspend fun getPending(): List<EmergencyEventEntity>

    @Query("SELECT * FROM emergency_events WHERE id = :id")
    suspend fun getById(id: String): EmergencyEventEntity?
}

@Dao
interface TrustedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity)

    @Delete
    suspend fun delete(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM trusted_contacts WHERE enabled = 1 ORDER BY name ASC")
    fun getAllFlow(): Flow<List<TrustedContactEntity>>
}

@Dao
interface SafePhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: SafePhraseEntity)

    @Query("SELECT * FROM safe_phrase ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveFlow(): Flow<SafePhraseEntity?>

    @Query("SELECT * FROM safe_phrase ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActive(): SafePhraseEntity?
}
