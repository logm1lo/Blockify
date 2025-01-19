package fn.logmilo.blockify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SpamNumber::class], version = 1)
abstract class SpamDatabase : RoomDatabase() {
    abstract fun spamNumberDao(): SpamNumberDao

    companion object {
        @Volatile
        private var INSTANCE: SpamDatabase? = null
        private lateinit var fileManager: SpamFileManager

        fun initialize(context: Context) {
            fileManager = SpamFileManager(context)
            
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpamDatabase::class.java,
                    "spam_numbers.db"
                ).build().also { database ->
                    INSTANCE = database
                    // Initialize database with numbers from file
                    CoroutineScope(Dispatchers.IO).launch {
                        syncWithFile(database)
                    }
                }
            }
        }

        fun getInstance(): SpamDatabase {
            return INSTANCE ?: throw IllegalStateException("Database not initialized")
        }

        private suspend fun syncWithFile(database: SpamDatabase) {
            val fileNumbers = fileManager.readSpamNumbers()
            if (fileNumbers.isNotEmpty()) {
                database.spamNumberDao().insertAll(fileNumbers)
            }
        }

        suspend fun updateSpamList(numbers: List<SpamNumber>) {
            fileManager.writeSpamNumbers(numbers)
            getInstance().spamNumberDao().apply {
                deleteAll()
                insertAll(numbers)
            }
        }

        suspend fun addSpamNumber(number: SpamNumber) {
            fileManager.appendSpamNumber(number)
            getInstance().spamNumberDao().insertAll(listOf(number))
        }
    }
} 