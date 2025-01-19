package fn.logmilo.blockify.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamNumberDao {
    @Query("SELECT * FROM spam_numbers ORDER BY reportCount DESC")
    fun getAllSpamNumbers(): Flow<List<SpamNumber>>

    @Query("SELECT EXISTS(SELECT 1 FROM spam_numbers WHERE phoneNumber = :number)")
    suspend fun isSpamNumber(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(numbers: List<SpamNumber>)

    @Query("DELETE FROM spam_numbers")
    suspend fun deleteAll()

    @Query("UPDATE spam_numbers SET reportCount = reportCount + 1, lastUpdated = :timestamp WHERE phoneNumber = :number")
    suspend fun incrementReportCount(number: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT MAX(lastUpdated) FROM spam_numbers")
    suspend fun getLastUpdateTime(): Long?
}