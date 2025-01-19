package fn.logmilo.blockify.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fn.logmilo.blockify.data.SpamDatabase
import fn.logmilo.blockify.data.SpamNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.*
import java.io.IOException
import android.content.ComponentName
import android.content.Context
import android.telecom.TelecomManager
import android.content.Intent
import android.os.Build
import fn.logmilo.blockify.service.CallScreeningService
import android.app.Activity
import android.app.role.RoleManager

class MainViewModel : ViewModel() {
    private val TAG = "MainViewModel"
    
    var isBlockingEnabled by mutableStateOf(false)
        private set
    
    var isUpdating by mutableStateOf(false)
        private set
        
    var isChecking by mutableStateOf(false)
        private set
        
    var databaseStatus by mutableStateOf<DatabaseStatus>(DatabaseStatus.Unknown)
        private set
        
    var hasRequiredPermissions by mutableStateOf(false)
        private set

    var showUpdateSuccess by mutableStateOf(false)
        private set

    private var context: Context? = null

    init {
        databaseStatus = DatabaseStatus.Unknown
    }
    
    sealed class DatabaseStatus {
        object Unknown : DatabaseStatus()
        data class UpToDate(val lastUpdateDate: Long) : DatabaseStatus()
        data class NeedsUpdate(val localDate: Long, val remoteDate: Long) : DatabaseStatus()
        data class Error(val message: String) : DatabaseStatus()
    }

    fun checkDatabaseStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isChecking = true
                Log.d(TAG, "Checking database status...")
                val localLastUpdate = SpamDatabase.getInstance().spamNumberDao().getLastUpdateTime() ?: 0L
                Log.d(TAG, "Local last update: ${formatDate(localLastUpdate)}")
                
                Log.d(TAG, "Fetching GitHub content...")
                val url = URL("https://raw.githubusercontent.com/logm1lo/Blockify/master/SpamNumbers/Vietnam/SpamNumbers.txt")
                val connection = url.openConnection()
                connection.connectTimeout = 3000 // 3 seconds timeout
                connection.readTimeout = 3000
                connection.connect()
                
                // Get the last modified date from the connection
                val githubLastUpdate = connection.lastModified.takeIf { it > 0 }
                    ?: Instant.now().toEpochMilli()
                Log.d(TAG, "GitHub last update: ${formatDate(githubLastUpdate)}")
                
                databaseStatus = if (localLastUpdate >= githubLastUpdate) {
                    Log.d(TAG, "Database is up to date")
                    DatabaseStatus.UpToDate(localLastUpdate)
                } else {
                    Log.d(TAG, "Database needs update")
                    DatabaseStatus.NeedsUpdate(localLastUpdate, githubLastUpdate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database status", e)
                databaseStatus = DatabaseStatus.Error("Failed to check database status: ${e.message}")
            } finally {
                isChecking = false
            }
        }
    }

    fun updateSpamDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isUpdating = true
                Log.d(TAG, "Starting database update...")
                
                Log.d(TAG, "Fetching spam numbers from GitHub...")
                val content = URL("https://raw.githubusercontent.com/logm1lo/Blockify/master/SpamNumbers/Vietnam/SpamNumbers.txt")
                    .readText()
                Log.d(TAG, "Fetched content length: ${content.length}")
                
                val numbers = content.split("\n")
                    .filter { it.isNotBlank() }
                    .map { SpamNumber(phoneNumber = it.trim()) }
                Log.d(TAG, "Parsed ${numbers.size} spam numbers")
                
                Log.d(TAG, "Updating spam list in database...")
                SpamDatabase.updateSpamList(numbers)
                Log.d(TAG, "Database update completed")
                
                // Update the database status immediately after successful update
                val currentTime = System.currentTimeMillis()
                databaseStatus = DatabaseStatus.UpToDate(currentTime)
                showUpdateSuccess = true
                
                // Auto-hide success message after 3 seconds
                viewModelScope.launch {
                    delay(3000)
                    showUpdateSuccess = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating database", e)
                databaseStatus = DatabaseStatus.Error("Failed to update database: ${e.message}")
            } finally {
                isUpdating = false
            }
        }
    }

    fun onPermissionsGranted() {
        hasRequiredPermissions = true
    }

    fun onPermissionsDenied() {
        hasRequiredPermissions = false
        isBlockingEnabled = false
    }

    fun setContext(context: Context) {
        this.context = context
    }

    fun toggleBlocking() {
        if (!hasRequiredPermissions) {
            return
        }
        
        context?.let { ctx ->
            if (!isBlockingEnabled) {
                // Enable call screening
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val roleManager = ctx.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        if (ctx is Activity) {
                            ctx.startActivityForResult(intent, ROLE_REQUEST_CODE)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to request call screening role", e)
                    }
                }
            } else {
                // Disable call screening
                isBlockingEnabled = false
            }
        }
    }

    fun onCallScreeningRoleGranted() {
        isBlockingEnabled = true
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    companion object {
        const val ROLE_REQUEST_CODE = 1234
    }
} 