import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["sessionId"], unique = true)]  // ← Add this
)
data class SessionEntity(
    @PrimaryKey val id: Long = 0,
    val sessionId: String,
    val patientId: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean = true
)