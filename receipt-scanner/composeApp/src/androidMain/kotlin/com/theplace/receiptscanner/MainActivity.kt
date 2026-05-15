package com.theplace.receiptscanner

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.viewmodel.ReceiptViewModel

// FragmentActivity nécessaire pour BiometricPrompt (cf. AppLock.android.kt).
class MainActivity : FragmentActivity() {

    private val viewModel: ReceiptViewModel by viewModels {
        val services = (application as ReceiptScannerApp).services
        ReceiptViewModelFactory(services.repository, services.pdfActions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val services = (application as ReceiptScannerApp).services
        setContent {
            App(
                viewModel = viewModel,
                appLock = services.appLock,
                dynamicColorScheme = rememberDynamicColorScheme(),
            )
        }
    }
}

@Composable
private fun rememberDynamicColorScheme(): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
}

private class ReceiptViewModelFactory(
    private val repository: ReceiptRepository,
    private val pdfActions: PdfActions,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReceiptViewModel(repository, pdfActions) as T
}
