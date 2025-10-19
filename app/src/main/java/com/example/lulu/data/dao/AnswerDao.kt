import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AnswerDao {
    @Query("SELECT * FROM answers WHERE sessionId = :sessionId AND date = :date")
    fun getAnswersForDate(sessionId: String, date: LocalDate): Flow<List<AnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: AnswerEntity)

    @Query("SELECT * FROM answers WHERE sessionId = :sessionId")
    fun getAnswersBySession(sessionId: String): Flow<List<AnswerEntity>>
}
