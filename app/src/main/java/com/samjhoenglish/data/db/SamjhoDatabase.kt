package com.samjhoenglish.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** All of a child's progress, stored only on this phone. */
@Database(
    entities = [Child::class, LessonProgress::class, SkillState::class, ItemState::class, Attempt::class, StudySession::class],
    version = 1,
)
abstract class SamjhoDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao

    companion object {
        fun create(context: Context): SamjhoDatabase =
            Room.databaseBuilder(context, SamjhoDatabase::class.java, "samjho.db").build()
    }
}
