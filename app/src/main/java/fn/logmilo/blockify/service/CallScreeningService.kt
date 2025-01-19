package fn.logmilo.blockify.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log
import fn.logmilo.blockify.data.SpamDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningService : CallScreeningService() {
    private val TAG = "CallScreeningService"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart?.let { number ->
            // Clean the phone number (remove spaces, dashes, etc.)
            number.replace(Regex("[^0-9+]"), "")
        }
        
        if (phoneNumber == null) {
            Log.d(TAG, "No phone number available, allowing call")
            respondToCall(callDetails, false)
            return
        }

        scope.launch {
            try {
                val isSpam = SpamDatabase.getInstance().spamNumberDao().isSpamNumber(phoneNumber)
                if (isSpam) {
                    Log.d(TAG, "Blocking spam number: $phoneNumber")
                } else {
                    Log.d(TAG, "Allowing non-spam number: $phoneNumber")
                }
                respondToCall(callDetails, isSpam)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking spam number", e)
                respondToCall(callDetails, false)
            }
        }
    }

    private fun respondToCall(callDetails: Call.Details, shouldBlock: Boolean) {
        val response = CallResponse.Builder()
        
        if (shouldBlock) {
            response.apply {
                setDisallowCall(true)          // Prevent the call from ringing
                setRejectCall(true)            // Immediately reject the call
                setSkipCallLog(false)          // Still log the blocked call
                setSkipNotification(false)     // Show a notification for blocked call
                setSilenceCall(true)           // Ensure no sound is played
            }
        } else {
            response.apply {
                setDisallowCall(false)
                setRejectCall(false)
                setSkipCallLog(false)
                setSkipNotification(false)
                setSilenceCall(false)
            }
        }

        respondToCall(callDetails, response.build())
    }
} 