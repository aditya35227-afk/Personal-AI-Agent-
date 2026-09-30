package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_logs")
data class PermissionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val permissionKey: String,
    val actionType: String, // "CHECKED", "REQUESTED", "GRANTED", "DENIED", "BLOCKED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerPhrase: String,
    val actionType: String,
    val actionParams: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String = "GENERAL", // "HOMEWORK", "REVISION", "DOUBT", "QUIZ", "GENERAL"
    val updatedAt: Long = System.currentTimeMillis()
)
