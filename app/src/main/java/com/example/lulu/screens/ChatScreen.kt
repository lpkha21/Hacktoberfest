package com.example.lulu.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material.icons.filled.Send
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.text.isNotBlank
import kotlin.text.trim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var message by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    val userId = 1
    val baseUrl = remember { "http://10.0.2.2:8000" }  // Emulator -> localhost
    var error by remember { mutableStateOf<String?>(null) }
    var lastQuestionId by remember { mutableStateOf<Int?>(null) }

    // Initialize session (generate + store questions), then get first question
    LaunchedEffect(Unit) {
        try {
            // Initialize daily session (generates and stores questions in DB)
            initDailySession(baseUrl, userId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to initialize session"
        }
        
        try {
            // Get first question from DB
            val qid = requestNextQuestion(baseUrl, userId)
            lastQuestionId = qid
        } catch (e: Exception) {
            error = e.message ?: "Failed to fetch first question"
        }
        
        try {
            // Load messages
            val msgs = fetchMessages(baseUrl, userId)
            messages.clear()
            messages.addAll(msgs)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load messages"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Daily Symptom Chat",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages) { idx, chatMessage ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> if (chatMessage.isUser) fullWidth / 2 else -fullWidth / 2 },
                        animationSpec = tween(280)
                    )
                ) {
                    MessageBubble(message = chatMessage)
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        ChatInput(
            value = message,
            onValueChange = { message = it },
            onSend = {
                if (message.isNotBlank()) {
                    val toSend = message.trim()
                    message = ""
                    
                    scope.launch {
                        try {
                            // Submit answer to DB
                            val qid = lastQuestionId
                            if (qid != null) {
                                submitAnswer(baseUrl, userId, qid, toSend)
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to submit answer"
                        }
                        
                        try {
                            // Refresh messages to show user answer
                            val after = fetchMessages(baseUrl, userId)
                            messages.clear()
                            messages.addAll(after)
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to refresh messages"
                        }
                        
                        try {
                            // Get next question from DB
                            delay(300)
                            val nextId = requestNextQuestion(baseUrl, userId)
                            lastQuestionId = nextId
                        } catch (e: Exception) {
                            // No more questions (204)
                            lastQuestionId = null
                        }
                        
                        try {
                            // Refresh messages to show next question
                            val afterNext = fetchMessages(baseUrl, userId)
                            messages.clear()
                            messages.addAll(afterNext)
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to refresh messages"
                        }
                    }
                }
            },
            errorText = error
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }
    
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(16.dp),
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val dots = remember {
        List(3) { Animatable(0f) }
    }
    
    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(key1 = animatable) {
            delay(index * 100L)
            while (true) {
                animatable.animateTo(1f, animationSpec = tween(500))
                animatable.animateTo(0f, animationSpec = tween(500))
            }
        }
    }

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dots.forEach { animatable ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = animatable.value)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    errorText: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional error text
            errorText?.let { errMsg ->
                Text(
                    text = errMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Describe symptoms (e.g., headache, nausea)...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

// --- Network helpers ---
private suspend fun initDailySession(baseUrl: String, userId: Int) = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/init_daily_session")
    val payload = JSONObject().apply {
        put("user_id", userId)
        put("patient_description", JSONObject.NULL)
    }.toString()
    val conn = (url.openConnection() as HttpURLConnection).apply {
        doOutput = true
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        connectTimeout = 15000
        readTimeout = 15000
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    if (conn.responseCode !in 200..299) throw IllegalStateException("Init session failed: ${conn.responseCode}")
}

private suspend fun fetchMessages(baseUrl: String, userId: Int): List<ChatMessage> = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/chat/messages?user_id=$userId")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json")
        connectTimeout = 8000
        readTimeout = 8000
    }
    if (conn.responseCode !in 200..299) return@withContext emptyList()
    conn.inputStream.bufferedReader().use { reader ->
        val body = reader.readText()
        val arr = JSONArray(body)
        val out = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val role = obj.getString("role")
            val content = obj.getString("content")
            out.add(ChatMessage(content = content, isUser = role == "user"))
        }
        out
    }
}

private suspend fun requestNextQuestion(baseUrl: String, userId: Int): Int? = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/chat/next-question")
    val payload = JSONObject().apply {
        put("user_id", userId)
        put("patient_description", JSONObject.NULL)
    }.toString()
    val conn = (url.openConnection() as HttpURLConnection).apply {
        doOutput = true
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        connectTimeout = 10000
        readTimeout = 10000
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    return@withContext when (conn.responseCode) {
        200 -> conn.inputStream.bufferedReader().use { br -> JSONObject(br.readText()).getInt("question_id") }
        204 -> null
        else -> null
    }
}

private suspend fun submitAnswer(baseUrl: String, userId: Int, questionId: Int, answer: String) = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/chat/answer")
    val payload = JSONObject().apply {
        put("user_id", userId)
        put("question_id", questionId)
        put("answer_text", answer)
    }.toString()
    val conn = (url.openConnection() as HttpURLConnection).apply {
        doOutput = true
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        connectTimeout = 10000
        readTimeout = 10000
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    if (conn.responseCode !in 200..299) throw IllegalStateException("Submit answer failed: ${conn.responseCode}")
}

private suspend fun generateQuestions(baseUrl: String, description: String): Map<String, String> = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/generate_daily_questions")
    val payload = JSONObject().apply { put("description", description) }.toString()
    val conn = (url.openConnection() as HttpURLConnection).apply {
        doOutput = true
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        connectTimeout = 10000
        readTimeout = 10000
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    if (conn.responseCode !in 200..299) return@withContext emptyMap()
    conn.inputStream.bufferedReader().use { br ->
        val body = br.readText()
        val obj = JSONObject(body)
        val q = obj.getJSONObject("questions")
        val map = mutableMapOf<String, String>()
        val keys = q.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = q.getString(k)
        }
        map
    }
}

private suspend fun saveAnswer(baseUrl: String, sessionId: String, question: String, answer: String) = withContext(Dispatchers.IO) {
    val url = URL("$baseUrl/save_answer")
    val payload = JSONObject().apply {
        put("session_id", sessionId)
        put("question", question)
        put("answer", answer)
    }.toString()
    val conn = (url.openConnection() as HttpURLConnection).apply {
        doOutput = true
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        connectTimeout = 10000
        readTimeout = 10000
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    if (conn.responseCode !in 200..299) throw IllegalStateException("Save answer failed: ${conn.responseCode}")
}
