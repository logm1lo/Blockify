package fn.logmilo.blockify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fn.logmilo.blockify.ui.MainViewModel
import fn.logmilo.blockify.ui.theme.BlockifyTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import android.content.Intent
import android.app.Activity

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.onPermissionsGranted()
        } else {
            viewModel.onPermissionsDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setContext(this)
        enableEdgeToEdge()
        
        checkAndRequestPermissions()
        
        setContent {
            BlockifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.onPermissionsGranted()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MainViewModel.ROLE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.onCallScreeningRoleGranted()
            } else {
                // Role was not granted
                viewModel.onPermissionsDenied()
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Add rotation animation state
    val rotation by rememberInfiniteTransition(label = "refresh_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Blockify",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))

        if (!viewModel.hasRequiredPermissions) {
            Text(
                text = "Required permissions not granted",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        
        Switch(
            checked = viewModel.isBlockingEnabled,
            onCheckedChange = { viewModel.toggleBlocking() },
            enabled = viewModel.hasRequiredPermissions
        )
        Text(
            text = if (viewModel.isBlockingEnabled) "Blocking Enabled" else "Blocking Disabled",
            color = if (viewModel.hasRequiredPermissions) 
                MaterialTheme.colorScheme.onSurface 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { viewModel.updateSpamDatabase() },
                    enabled = when (viewModel.databaseStatus) {
                        is MainViewModel.DatabaseStatus.NeedsUpdate -> !viewModel.isUpdating && !viewModel.isChecking
                        is MainViewModel.DatabaseStatus.Error -> !viewModel.isUpdating && !viewModel.isChecking
                        else -> false // Disable when up to date or unknown
                    }
                ) {
                    Text(
                        text = when {
                            viewModel.isUpdating -> "Updating..."
                            viewModel.databaseStatus is MainViewModel.DatabaseStatus.UpToDate -> "Up to date"
                            viewModel.databaseStatus is MainViewModel.DatabaseStatus.Unknown -> "Check for updates first"
                            else -> "Update Spam Database"
                        }
                    )
                }
                
                if (viewModel.isChecking) {
                    Text(
                        text = "Checking for updates...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (viewModel.isUpdating) {
                    Text(
                        text = "Updating database...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = { viewModel.checkDatabaseStatus() },
                enabled = !viewModel.isUpdating && !viewModel.isChecking && viewModel.hasRequiredPermissions
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Check for updates",
                    modifier = if (viewModel.isChecking) {
                        Modifier.rotate(rotation)
                    } else {
                        Modifier
                    },
                    tint = when {
                        !viewModel.hasRequiredPermissions -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        viewModel.isChecking -> MaterialTheme.colorScheme.primary
                        !viewModel.isUpdating -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
        
        // Database status text
        when (val status = viewModel.databaseStatus) {
            MainViewModel.DatabaseStatus.Unknown -> {
                Text(
                    text = "Click the refresh button to check for updates",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            is MainViewModel.DatabaseStatus.UpToDate -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (viewModel.showUpdateSuccess) 
                            "✓ Database updated successfully • " 
                        else 
                            "Database is up to date • ",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatDate(status.lastUpdateDate),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is MainViewModel.DatabaseStatus.NeedsUpdate -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "New update available • ",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last updated ${formatDate(status.localDate)}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is MainViewModel.DatabaseStatus.Error -> Text(
                text = status.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}