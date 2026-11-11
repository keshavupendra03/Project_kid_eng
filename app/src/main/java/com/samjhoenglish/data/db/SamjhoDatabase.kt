package com.samjhoenglish.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import androidx.room.Room
import androidx.room.RoomDatabase

/** All of a child's progress, stored only on this phone. */
@Database(
    entities = [
        Child::class, LessonProgress::class, SkillState::class, ItemState::class,
        Attempt::class, StudySession::class, MissionState::class, SchoolLink::class, ReadingResult::class, ExpressResult::class,
    ],
    version = 6,
    // 2 adds the parent section, 3 school links, 4 reading results, 5 Tinku Express, 6 removes the Voice Diary
    // (the app no longer keeps any recording of a child's voice). Children's existing progress is kept.
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = SamjhoDatabase.DropVoiceDiary::class),
    ],
)
abstract class SamjhoDatabase : RoomDatabase() {
    @DeleteTable(tableName = "voice_clips")
    class DropVoiceDiary : AutoMigrationSpec

    abstract fun progress(): ProgressDao

    companion object {
        fun create(context: Context): SamjhoDatabase =
            Room.databaseBuilder(context, SamjhoDatabase::class.java, "samjho.db").build()
    }
}
