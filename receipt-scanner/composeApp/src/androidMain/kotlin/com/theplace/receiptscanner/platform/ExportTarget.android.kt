package com.theplace.receiptscanner.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Cible d'export Android : URI de dossier obtenu par OpenDocumentTree. */
actual class PlatformExportTarget internal constructor(val treeUri: Uri)

@Composable
actual fun rememberExportFolderLauncher(
    onResult: (PlatformExportTarget?) -> Unit,
): ExportFolderLauncher {
    val current = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        current.value(uri?.let { PlatformExportTarget(it) })
    }
    return remember(launcher) {
        object : ExportFolderLauncher {
            override fun launch() = launcher.launch(null)
        }
    }
}
