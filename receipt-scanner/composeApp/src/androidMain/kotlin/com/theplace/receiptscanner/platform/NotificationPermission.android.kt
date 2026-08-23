package com.theplace.receiptscanner.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* résultat ignoré : si refusée, on n'insiste pas */ }

    return remember(launcher, context) {
        object : NotificationPermissionRequester {
            override fun requestIfNeeded() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) return
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
