package com.example.lulu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE qDate = :epochDay ORDER BY orderIndex ASC")
    suspend fun forDay(epochDay: java.time.LocalDate): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE qDate = :day ORDER BY orderIndex ASC LIMIT 1 OFFSET :offset")
    suspend fun getByOffset(day: java.time.LocalDate, offset: Int): QuestionEntity?
}

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: AnswerEntity)

    @Query("SELECT COUNT(*) FROM answers WHERE questionId = :questionId")
    suspend fun countForQuestion(questionId: Int): Int
}

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_messages WHERE mDate = :day ORDER BY createdAtMs ASC")
    suspend fun messagesForDay(day: java.time.LocalDate): List<ChatMessageEntity>
}
