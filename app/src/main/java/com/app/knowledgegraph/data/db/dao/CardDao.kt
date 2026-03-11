package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: Card): Long

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): Card?

    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Card>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<Card>): List<Long>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observeById(id: Long): Flow<Card?>

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE chapter = :chapter ORDER BY createdAt ASC")
    fun observeByChapter(chapter: String): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: CardType): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE chapter = :chapter AND type = :type ORDER BY createdAt ASC")
    fun observeByChapterAndType(chapter: String, type: CardType): Flow<List<Card>>

    @Query("SELECT DISTINCT chapter FROM cards ORDER BY chapter ASC")
    fun observeAllChapters(): Flow<List<String>>

    @Query("""
        SELECT * FROM cards 
        WHERE prompt LIKE '%' || :keyword || '%' 
           OR answer LIKE '%' || :keyword || '%'
           OR tags LIKE '%' || :keyword || '%'
        ORDER BY updatedAt DESC
    """)
    fun search(keyword: String): Flow<List<Card>>

    @Query("SELECT COUNT(*) FROM cards")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE chapter = :chapter")
    suspend fun getCountByChapter(chapter: String): Int

    @Query("DELETE FROM cards WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT DISTINCT tags FROM cards WHERE tags != '' ORDER BY tags")
    fun observeAllTags(): Flow<List<String>>
}
