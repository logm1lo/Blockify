package fn.logmilo.blockify.data

import android.content.Context
import java.io.File
import java.io.IOException

class SpamFileManager(private val context: Context) {
    private val spamFile: File
        get() = File(context.filesDir, "spamnumbers.txt")

    init {
        if (!spamFile.exists()) {
            spamFile.createNewFile()
        }
    }

    fun readSpamNumbers(): List<SpamNumber> {
        return try {
            spamFile.readLines()
                .filter { it.isNotBlank() }
                .map { line ->
                    val parts = line.split(",")
                    SpamNumber(
                        phoneNumber = parts[0],
                        reportCount = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                        lastUpdated = parts.getOrNull(2)?.toLongOrNull() 
                            ?: System.currentTimeMillis()
                    )
                }
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun writeSpamNumbers(numbers: List<SpamNumber>) {
        try {
            spamFile.bufferedWriter().use { writer ->
                numbers.forEach { number ->
                    writer.write("${number.phoneNumber},${number.reportCount},${number.lastUpdated}")
                    writer.newLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun appendSpamNumber(number: SpamNumber) {
        try {
            spamFile.appendText("${number.phoneNumber},${number.reportCount},${number.lastUpdated}\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}