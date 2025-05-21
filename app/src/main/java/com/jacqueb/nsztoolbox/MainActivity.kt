package com.jacqueb.nsztoolbox

import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.github.luben.zstd.ZstdInputStream
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME      = "settings"
        private const val KEY_OUTPUT_URI = "output_folder"
        private const val KEY_DELETE_NSZ = "delete_nsz"
    }

    private lateinit var prefs: SharedPreferences
    private var outputFolderUri: Uri? = null
    private var pendingInputUri: Uri? = null

    private lateinit var folderPicker: ActivityResultLauncher<Uri?>
    private lateinit var deleteToggle: Switch
    private lateinit var logText:     TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs        = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        deleteToggle = findViewById(R.id.delete_switch)
        logText      = findViewById(R.id.log_text)

        // Restore output‐folder URI if we still have permission
        prefs.getString(KEY_OUTPUT_URI, null)?.let { uriStr ->
            val uri = Uri.parse(uriStr)
            if (contentResolver.persistedUriPermissions.any { it.uri == uri }) {
                outputFolderUri = uri
            }
        }

        // Restore delete toggle
        deleteToggle.isChecked = prefs.getBoolean(KEY_DELETE_NSZ, false)
        deleteToggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_DELETE_NSZ, checked).apply()
        }

        // Set up the SAF folder picker
        folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                // Persist permissions long-term
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString(KEY_OUTPUT_URI, treeUri.toString()).apply()
                outputFolderUri = treeUri
                Toast.makeText(this, "Output folder set.", Toast.LENGTH_SHORT).show()

                // If a share arrived before we had a folder, process it now
                pendingInputUri?.let {
                    handleFileUri(it)
                    pendingInputUri = null
                }
            } else {
                Toast.makeText(this, "Folder selection canceled.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle a SHARE‐NSZ intent
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                pendingInputUri = uri
                handleShare(uri)
            }
        }
    }

    private fun handleShare(inputUri: Uri) {
        // If we don’t yet have an output folder, pick it now
        if (outputFolderUri == null) {
            folderPicker.launch(null)
        } else {
            handleFileUri(inputUri)
        }
    }

    private fun handleFileUri(inputUri: Uri) {
        try {
            // 1) Figure out the real display name:
            val originalName = queryFileName(inputUri)
                ?: inputUri.lastPathSegment
                ?: "input.nsz"

            val baseName = originalName.substringBeforeLast('.')
            val outputName = "$baseName.nsp"

            // 2) Resolve the tree and create the new file
            val tree = DocumentFile.fromTreeUri(this, outputFolderUri!!)
                ?: throw IllegalArgumentException("Invalid output‐folder URI")

            val outFile = tree.createFile(
                "application/octet-stream",
                outputName
            ) ?: throw Exception("Could not create $outputName")

            // 3) Stream‐decompress NSZ → NSP
            contentResolver.openInputStream(inputUri)!!.use { inp ->
                contentResolver.openOutputStream(outFile.uri)!!.use { out ->
                    ZstdInputStream(inp).use { zstd ->
                        zstd.copyTo(out)
                    }
                }
            }

            // 4) Optional delete original
            if (deleteToggle.isChecked) {
                DocumentsContract.deleteDocument(contentResolver, inputUri)
            }

            logText.text = "Done! Saved as $outputName"
        }
        catch (e: Exception) {
            logText.text = "Error: ${e.localizedMessage}"
            Toast.makeText(this, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**  
     * Safely query the user‐visible filename for a content:// Uri  
     */
    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
        }
        // Fallback for file:// Uris
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            File(uri.path!!).name.let { return it }
        }
        return null
    }
}