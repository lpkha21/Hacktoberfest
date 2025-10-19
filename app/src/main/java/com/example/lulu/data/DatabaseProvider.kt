import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.Executors

object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }
    }

    private fun build(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "lulu.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    prepopulate(context)
                }
            })
            .build()
    }

    private fun prepopulate(context: Context) {
        // Run on a background thread
        Executors.newSingleThreadExecutor().execute {
            val db = get(context)
            val sessionDao = db.sessionDao()
            val dqDao = db.dailyQuestionDao()

            // Create a mock session
            val sessionId = "mock-session-1"
            val session = SessionEntity(
                id = 1,
                sessionId = sessionId,
                patientId = "user-1",
                startDate = LocalDate.now(),
                endDate = null,
                isActive = true
            )

            // Insert session
            CoroutineScope(Dispatchers.IO).launch {
                sessionDao.insertSession(session)

                // Seed daily questions for the mock session
                val questions = listOf(
                    DailyQuestionEntity(id = 1, question = "How did you sleep last night?", sessionId = sessionId),
                    DailyQuestionEntity(id = 2, question = "What is your current pain level (1-10)?", sessionId = sessionId),
                    DailyQuestionEntity(id = 3, question = "Any shortness of breath today?", sessionId = sessionId),
                    DailyQuestionEntity(id = 4, question = "Did you experience nausea?", sessionId = sessionId),
                    DailyQuestionEntity(id = 5, question = "How is your energy compared to yesterday?", sessionId = sessionId),
                    DailyQuestionEntity(id = 6, question = "Any new or worsening symptoms?", sessionId = sessionId)
                )

                questions.forEach { dqDao.insertQuestion(it) }
            }
        }
    }
}
