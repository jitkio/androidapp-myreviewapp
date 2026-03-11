package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.PracticeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PracticeRecord): Long

    @Query("SELECT * FROM practice_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PracticeRecord>>

    @Query("SELECT * FROM practice_records ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<PracticeRecord>>

    /** 正确率统计 */
    @Query("SELECT COUNT(*) FROM practice_records WHERE isCorrect = 1")
    fun observeCorrectCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM practice_records")
    fun observeTotalCount(): Flow<Int>
}
