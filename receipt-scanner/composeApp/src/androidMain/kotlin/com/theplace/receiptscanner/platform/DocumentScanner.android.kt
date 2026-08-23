package com.theplace.receiptscanner.platform

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/** Référence vers le PDF produit par ML Kit côté Android. */
actual class PlatformScanResult internal constructor(
    internal val uri: Uri,
    internal val pageCount: Int,
)

@Composable
actual fun rememberDocumentScannerLauncher(
    onResult: (ScanOutcome) -> Unit,
): DocumentScannerLauncher {
    val context = LocalContext.current

    val activityLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val parsed = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                    val pdf = parsed?.pdf
                    onResult(
                        if (pdf != null) {
                            ScanOutcome.Success(PlatformScanResult(pdf.uri, pdf.pageCount))
                        } else ScanOutcome.Failure("Le scanner n'a pas retourné de PDF.")
                    )
                }
                Activity.RESULT_CANCELED -> onResult(ScanOutcome.Cancelled)
                else -> onResult(
                    ScanOutcome.Failure("Code de résultat inattendu : ${result.resultCode}")
                )
            }
        }

    return remember(activityLauncher) {
        AndroidDocumentScannerLauncher(
            launcher = activityLauncher,
            onError = { msg -> onResult(ScanOutcome.Failure(msg)) },
            getActivity = { context as? Activity },
        )
    }
}

private class AndroidDocumentScannerLauncher(
    private val launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    private val onError: (String) -> Unit,
    private val getActivity: () -> Activity?,
) : DocumentScannerLauncher {
    private val options: GmsDocumentScannerOptions =
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

    override fun launch() {
        val activity = getActivity()
        if (activity == null) {
            onError("Activity indisponible")
            return
        }
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: e.javaClass.simpleName)
            }
    }
}
