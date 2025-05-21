package com.jacqueb.nsztoolbox

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.github.luben.zstd.ZstdInputStream

private const val PREFS_NAME = "settings"
private const val KEY_OUTPUT_URI = "output_folder"

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var outputFolderUri: Uri? = null
    private var pendingInputUri: Uri? = null

    private lateinit var deleteToggle: Switch
    private lateinit var logText: TextView

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Persist permissions
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString(KEY_OUTPUT_URI, uri.toString()).apply()
                outputFolderUri = uri
                Toast.makeText(this, "Output folder set.", Toast.LENGTH_SHORT).show()

                // If a share was pending, now process it
                pendingInputUri?.let {
                    processNSZ(it)
                    pendingInputUri = null
                }
            } else {
                Toast.makeText(this, "Folder not selected.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        deleteToggle = findViewById(R.id.delete_switch)
        logText      = findViewById(R.id.log_text)

        // Restore previously picked folder URI (if any)
        prefs.getString(KEY_OUTPUT_URI, null)?.let { str ->
            Uri.parse(str).also { uri ->
                // Check we still hold permission
                if (contentResolver.persistedUriPermissions.any { it.uri == uri }) {
                    outputFolderUri = uri
                }
            }
        }

        deleteToggle.isChecked = prefs.getBoolean("delete_nsz", false)
        deleteToggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("delete_nsz", checked).apply()
        }

        // Handle a share intent
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { inputUri ->
                pendingInputUri = inputUri
                maybeProcessPending()
            }
        }
    }

    private fun maybeProcessPending() {
        val inputUri = pendingInputUri ?: return
        val outUri   = outputFolderUri

        if (outUri == null) {
            // Need to pick folder first
            folderPicker.launch(null)
        } else {
            // We have both URIs → process immediately
            processNSZ(inputUri)
            pendingInputUri = null
        }
    }

    private fun processNSZ(inputUri: Uri) {
        try {
            val filename   = inputUri.lastPathSegment ?: "input.nsz"
            val outputName = filename.substringBeforeLast('.') + ".nsp"

            // Wrap the tree URI
            val tree = DocumentFile.fromTreeUri(this, outputFolderUri!!)
                ?: throw IllegalArgumentException("Invalid output-folder URI")

            // Create the new file
            val newFile = tree.createFile(
                "application/octet-stream",
                outputName
            ) ?: throw Exception("Could not create $outputName")

            // Stream decompress into it
            contentResolver.openOutputStream(newFile.uri)!!.use { out ->
                contentResolver.openInputStream(inputUri)!!.use { inp ->
                    ZstdInputStream(inp).use { zstd ->
                        zstd.copyTo(out)
                    }
                }
            }

            // Optionally delete original
            if (deleteToggle.isChecked) {
                DocumentsContract.deleteDocument(contentResolver, inputUri)
            }

            logText.text = "Done! Saved as $outputName"
        } catch (e: Exception) {
            logText.text = "Error: ${e.localizedMessage}"
        }
    }
}