package com.theplace.receiptscanner.scanner

import android.app.Activity
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

/** Résultat du scan une fois l'utilisateur revenu dans l'app. */
sealed interface ScanOutcome {
    data class Success(val pdfResult: GmsDocumentScanningResult.Pdf) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Failure(val message: String) : ScanOutcome
}

/**
 * Mémoise un launcher pour ouvrir le scanner ML Kit. Les pages sont retournées
 * sous forme PDF prêt à archiver (RESULT_FORMAT_PDF).
 */
@Composable
fun rememberDocumentScannerLauncher(
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
                        if (pdf != null) ScanOutcome.Success(pdf)
                        else ScanOutcome.Failure("Le scanner n'a pas retourné de PDF.")
                    )
                }
                Activity.RESULT_CANCELED -> onResult(ScanOutcome.Cancelled)
                else -> onResult(
                    ScanOutcome.Failure("Code de résultat inattendu : ${result.resultCode}")
                )
            }
        }

    return remember(activityLauncher) {
        DocumentScannerLauncher(
            options = defaultScannerOptions(),
            launcher = activityLauncher,
            onError = { msg -> onResult(ScanOutcome.Failure(msg)) },
            getActivity = { context as? Activity },
        )
    }
}

private fun defaultScannerOptions(): GmsDocumentScannerOptions =
    GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(10) // un ticket = quelques pages au plus
        .setResultFormats(RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

class DocumentScannerLauncher internal constructor(
    private val options: GmsDocumentScannerOptions,
    private val launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    private val onError: (String) -> Unit,
    private val getActivity: () -> Activity?,
) {
    fun launch() {
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
