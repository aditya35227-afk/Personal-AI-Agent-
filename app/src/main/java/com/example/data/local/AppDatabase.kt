package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.PermissionLogDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PermissionLogEntity
import com.example.data.local.entity.RoutineEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        PermissionLogEntity::class,
        RoutineEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun permissionLogDao(): PermissionLogDao
    abstract fun routineDao(): RoutineDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_ai_agent.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
