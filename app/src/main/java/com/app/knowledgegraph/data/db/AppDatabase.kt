package com.app.knowledgegraph.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.knowledgegraph.data.db.converter.Converters
import com.app.knowledgegraph.data.db.dao.*
import com.app.knowledgegraph.data.db.entity.*

@Database(
    entities = [
        Card::class,
        Edge::class,
        ReviewSchedule::class,
        ErrorTag::class,
        PracticeRecord::class,
        ImportedQuestion::class,
        QuestionFolder::class,
        QuestionFolderItem::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun edgeDao(): EdgeDao
    abstract fun reviewDao(): ReviewDao
    abstract fun errorTagDao(): ErrorTagDao
    abstract fun practiceRecordDao(): PracticeRecordDao
    abstract fun importedQuestionDao(): ImportedQuestionDao
    abstract fun questionFolderDao(): QuestionFolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS imported_questions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        stem TEXT NOT NULL,
                        optionsJson TEXT NOT NULL DEFAULT '',
                        answer TEXT NOT NULL,
                        explanation TEXT NOT NULL DEFAULT '',
                        source TEXT NOT NULL DEFAULT '',
                        batchId INTEGER NOT NULL DEFAULT 0,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        correctCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE imported_questions ADD COLUMN figureSvg TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Card indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_chapter ON cards (chapter)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_type ON cards (type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_updatedAt ON cards (updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_chapter_type ON cards (chapter, type)")
                // ImportedQuestion indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_imported_questions_source ON imported_questions (source)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_imported_questions_batchId ON imported_questions (batchId)")
                // ReviewSchedule indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_schedules_nextReviewDate ON review_schedules (nextReviewDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_schedules_status ON review_schedules (status)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS question_folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS question_folder_items (
                        folderId INTEGER NOT NULL,
                        questionId INTEGER NOT NULL,
                        PRIMARY KEY(folderId, questionId),
                        FOREIGN KEY(folderId) REFERENCES question_folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(questionId) REFERENCES imported_questions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_question_folder_items_questionId ON question_folder_items (questionId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knowledge_graph.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
