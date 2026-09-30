package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PermissionLogEntity
import com.example.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionLogDao {
    @Query("SELECT * FROM permission_logs ORDER BY timestamp DESC LIMIT 100")
    fun getLogs(): Flow<List<PermissionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: PermissionLogEntity)

    @Query("DELETE FROM permission_logs")
    suspend fun clearLogs()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Query("UPDATE routines SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM study_notes")
    suspend fun clearNotes()
}
