package pl.complet.QuestSync.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

private const val TAG = "HealthPermissionHandler"

@Composable
fun HealthPermissionHandler(
    onPermissionsGranted: @Composable () -> Unit
) {
    // EMERGENCY BYPASS: Force entry to Dashboard regardless of system permission state.
    // This resolves the issue where users are stuck on a non-interactive authorization screen.
    
    LaunchedEffect(Unit) {
        Log.d(TAG, "EMERGENCY BYPASS: Forcing dashboard entry")
    }

    // Immediately render the granted content (Dashboard)
    onPermissionsGranted()
}
