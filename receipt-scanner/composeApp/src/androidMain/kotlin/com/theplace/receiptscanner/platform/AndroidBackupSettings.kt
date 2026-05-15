package com.theplace.receiptscanner.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.theplace.receiptscanner.work.BackupScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS = "backup_settings"
private const val KEY_FOLDER_URI = "folder_uri"
private const val KEY_ENABLED = "enabled"
private const val KEY_EXPORTED = "exported_filenames"

internal class AndroidBackupSettings(context: Context) : BackupSettings {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scheduler = BackupScheduler(appContext) { snapshot() }

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _folderLabel = MutableStateFlow(loadFolderLabel())
    override val folderLabel: StateFlow<String> = _folderLabel.asStateFlow()

    override val isReady: Boolean
        get() = folderUri() != null

    init {
        scheduler.refresh()
    }

    override fun setFolder(target: PlatformExportTarget?) {
        if (target == null) {
            prefs.edit().remove(KEY_FOLDER_URI).apply()
            _folderLabel.value = ""
        } else {
            val uri = target.treeUri
            // Persistance des permissions d'accès au dossier au-delà de la
            // session courante — sinon le worker ne pourrait plus y écrire.
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
            _folderLabel.value = displayLabel(uri)
        }
        scheduler.refresh()
    }

    override fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
        scheduler.refresh()
    }

    internal fun folderUri(): Uri? =
        prefs.getString(KEY_FOLDER_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    internal fun exportedFileNames(): MutableSet<String> =
        prefs.getStringSet(KEY_EXPORTED, emptySet())?.toMutableSet() ?: mutableSetOf()

    internal fun markExported(fileName: String) {
        val current = prefs.getStringSet(KEY_EXPORTED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(fileName)) {
            prefs.edit().putStringSet(KEY_EXPORTED, current).apply()
        }
    }

    private fun snapshot(): BackupScheduler.Snapshot =
        BackupScheduler.Snapshot(enabled = _enabled.value, isReady = isReady)

    private fun loadFolderLabel(): String =
        folderUri()?.let { displayLabel(it) }.orEmpty()

    private fun displayLabel(uri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return uri.lastPathSegment.orEmpty()
        return docId.substringAfterLast(':', missingDelimiterValue = docId)
    }
}
