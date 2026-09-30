package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val noteDao: NoteDao,
    private val routineDao: RoutineDao
) {
    val notes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val routines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()

    suspend fun saveNote(title: String, content: String, tag: String = "GENERAL"): Long {
        return noteDao.insertNote(
            NoteEntity(
                title = title,
                content = content,
                tag = tag,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
    }

    suspend fun clearNotes() {
        noteDao.clearNotes()
    }

    suspend fun saveRoutine(title: String, triggerPhrase: String, actionType: String, params: String): Long {
        return routineDao.insertRoutine(
            RoutineEntity(
                title = title,
                triggerPhrase = triggerPhrase,
                actionType = actionType,
                actionParams = params,
                isEnabled = true
            )
        )
    }

    suspend fun setRoutineEnabled(id: Long, enabled: Boolean) {
        routineDao.setEnabled(id, enabled)
    }

    suspend fun deleteRoutine(id: Long) {
        routineDao.deleteRoutine(id)
    }
}
