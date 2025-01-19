package fn.logmilo.blockify.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_numbers")
data class SpamNumber(
    @PrimaryKey
    val phoneNumber: String,
    val reportCount: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
) 