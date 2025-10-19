import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SessionEntity::class,
        DailyQuestionEntity::class,
        AnswerEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun dailyQuestionDao(): DailyQuestionDao
    abstract fun answerDao(): AnswerDao
}
