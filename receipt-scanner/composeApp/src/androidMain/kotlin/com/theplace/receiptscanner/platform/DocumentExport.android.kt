package com.theplace.receiptscanner.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PlatformDocumentTarget internal constructor(val uri: Uri)

@Composable
actual fun rememberCreateDocumentLauncher(
    mimeType: String,
    defaultName: String,
    onResult: (PlatformDocumentTarget?) -> Unit,
): CreateDocumentLauncher {
    val current = rememberUpdatedState(onResult)
    // CreateDocument prend en argument la suggestion de nom, le mime est
    // configuré dans le contrat par la sous-classe `CreateDocument(mime)`.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType),
    ) { uri: Uri? ->
        current.value(uri?.let { PlatformDocumentTarget(it) })
    }
    return remember(launcher, defaultName) {
        object : CreateDocumentLauncher {
            override fun launch() = launcher.launch(defaultName)
        }
    }
}

internal class AndroidDocumentWriter(private val context: Context) : DocumentWriter {
    override suspend fun writeText(
        target: PlatformDocumentTarget,
        content: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("Flux de sortie nul")
            true
        }.getOrElse { false }
    }
}
