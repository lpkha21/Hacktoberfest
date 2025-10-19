package com.example.lulu.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val qDate: LocalDate,
    val orderIndex: Int,
)

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId: Int,
    val text: String,
    val createdAtMs: Long,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "assistant" | "user"
    val content: String,
    val questionId: Int?,
    val mDate: LocalDate,
    val createdAtMs: Long,
)
