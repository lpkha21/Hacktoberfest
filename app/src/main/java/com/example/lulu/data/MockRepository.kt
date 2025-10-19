import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MockRepository(
    private val sessionDao: SessionDao,
    private val dqDao: DailyQuestionDao,
    private val answerDao: AnswerDao
) {
    private val sessionId = "mock-session-1"

    fun messagesForToday(): Flow<List<Pair<String, String>>> {
        val today = LocalDate.now()
        return dqDao.getQuestionsBySession(sessionId).map { questions ->
            val answersMap = emptyMap<String, String>()
            val out = mutableListOf<Pair<String, String>>()
            questions.forEach { q ->
                out.add("assistant" to q.question)
            }
            out
        }
    }

    suspend fun nextUnanswered(): DailyQuestionEntity? {
        val today = LocalDate.now()
        val all = dqDao.getQuestionsBySession(sessionId).first()
        val answered = answerDao.getAnswersBySession(sessionId).first().filter { it.date == today }
        val answeredSet = answered.map { it.question }.toSet()
        return all.firstOrNull { it.question !in answeredSet }
    }

    suspend fun submitAnswer(question: DailyQuestionEntity, answerText: String) {
        val today = LocalDate.now()
        val a = AnswerEntity(
            id = 0,
            question = question.question,
            answer = answerText,
            sessionId = sessionId,
            date = today
        )
        answerDao.insertAnswer(a)
    }
}
