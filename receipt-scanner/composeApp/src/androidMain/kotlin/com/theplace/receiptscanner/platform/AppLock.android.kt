package com.theplace.receiptscanner.platform

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.lock_button_unlock
import com.theplace.receiptscanner.resources.lock_subtitle
import com.theplace.receiptscanner.resources.lock_title
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.stringResource

private const val PREFS = "app_lock"
private const val KEY_ENABLED = "enabled"

internal class AndroidAppLockSettings(context: Context) : AppLockSettings {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override val isBiometricAvailable: Boolean
        get() {
            val manager = BiometricManager.from(appContext)
            val authenticators = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
            return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        }

    override fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }
}

/**
 * Singleton process-scoped : indique si l'utilisateur s'est authentifié
 * durant la vie du process courant. Reset implicite à chaque process death.
 */
internal object AppLockSession {
    var unlocked: Boolean = false
}

@Composable
actual fun BiometricGate(
    settings: AppLockSettings,
    content: @Composable () -> Unit,
) {
    val enabled by settings.enabled.collectAsState()
    val activity = LocalContext.current as? FragmentActivity
    var unlocked by remember { mutableStateOf(AppLockSession.unlocked || !enabled) }

    LaunchedEffect(enabled) {
        if (!enabled) unlocked = true
    }

    if (unlocked || activity == null) {
        content()
    } else {
        LockedScreen(
            onRetry = {
                launchPrompt(activity) {
                    AppLockSession.unlocked = true
                    unlocked = true
                }
            },
        )
        // Première tentative automatique à l'ouverture de l'app.
        LaunchedEffect(Unit) {
            launchPrompt(activity) {
                AppLockSession.unlocked = true
                unlocked = true
            }
        }
    }
}

@Composable
private fun LockedScreen(onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.lock_button_unlock))
            }
        }
    }
}

private fun launchPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            // Sur Error/Failed on laisse l'utilisateur retenter via le bouton.
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        // Les libellés affichés par la system UI biométrique restent en
        // français en dur — non gérés par Compose Resources côté system.
        .setTitle("Déverrouillage")
        .setSubtitle("Authentifiez-vous pour accéder à vos tickets")
        // BIOMETRIC_WEAK + DEVICE_CREDENTIAL : fallback PIN/motif si pas de biométrie.
        .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(info)
}
