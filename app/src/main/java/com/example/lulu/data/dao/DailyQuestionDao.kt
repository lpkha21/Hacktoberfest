import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestionDao {
    @Query("SELECT * FROM daily_questions WHERE sessionId = :sessionId")
    fun getQuestionsBySession(sessionId: String): Flow<List<DailyQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: DailyQuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: DailyQuestionEntity)
}
