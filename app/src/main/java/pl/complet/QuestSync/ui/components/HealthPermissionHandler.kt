package pl.complet.QuestSync.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HealthPermissionHandler(
    onPermissionsGranted: @Composable () -> Unit
) {
    val permissions = mutableListOf(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.BODY_SENSORS
    )
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionState = rememberMultiplePermissionsState(
        permissions = permissions
    )

    if (permissionState.allPermissionsGranted) {
        onPermissionsGranted()
    } else {
        Column {
            val textToShow = if (permissionState.shouldShowRationale) {
                "Health and Activity permissions are required to sync your data."
            } else {
                "Please grant permissions to sync your health data."
            }
            Text(textToShow)
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
        }
    }
}
