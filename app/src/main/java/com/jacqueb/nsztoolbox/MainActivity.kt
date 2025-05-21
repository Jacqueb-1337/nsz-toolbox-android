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

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var outputFolderUri: Uri? = null
    private var pendingInputUri: Uri? = null

    private lateinit var deleteToggle: Switch
    private lateinit var logText: TextView

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString("output_folder", uri.toString()).apply()
                outputFolderUri = uri

                Toast.makeText(this, "Output folder set.", Toast.LENGTH_SHORT).show()

                // If we had a share pending, handle it now:
                pendingInputUri?.also {
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

        prefs        = getSharedPreferences("settings", MODE_PRIVATE)
        deleteToggle = findViewById(R.id.delete_switch)
        logText      = findViewById(R.id.log_text)

        // Try to restore the folder URI:
        prefs.getString("output_folder", null)?.let {
            outputFolderUri = Uri.parse(it)
        }

        deleteToggle.isChecked =
            prefs.getBoolean("delete_nsz", false)
        deleteToggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("delete_nsz", checked).apply()
        }

        // Handle share intent *after* we've attempted to restore the folder:
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { inputUri ->
                if (outputFolderUri != null) {
                    // we already have a folder → process immediately
                    processNSZ(inputUri)
                } else {
                    // no folder yet → save and launch picker
                    pendingInputUri = inputUri
                    folderPicker.launch(null)
                }
            }
        }
    }

    private fun processNSZ(inputUri: Uri) {
        try {
            val name       = inputUri.lastPathSegment ?: "input.nsz"
            val outputName = name.substringBeforeLast('.') + ".nsp"

            // Safely unwrap our saved folder URI:
            val treeUri = outputFolderUri
                ?: throw IllegalStateException("No output folder selected")

            // Use DocumentFile API to create a new child document:
            val tree = DocumentFile.fromTreeUri(this, treeUri)
                ?: throw IllegalArgumentException("Invalid output-folder URI")

            val newFile = tree.createFile(
                "application/octet-stream",
                outputName
            ) ?: throw Exception("Could not create $outputName")

            // Stream the decompression:
            contentResolver.openOutputStream(newFile.uri)!!.use { out ->
                contentResolver.openInputStream(inputUri)!!.use { inp ->
                    ZstdInputStream(inp).use { zstd ->
                        zstd.copyTo(out)
                    }
                }
            }

            // Optionally delete the original:
            if (deleteToggle.isChecked) {
                DocumentsContract.deleteDocument(contentResolver, inputUri)
            }

            logText.text = "Done! Saved as $outputName"
        } catch (e: Exception) {
            logText.text = "Error: ${e.localizedMessage}"
        }
    }
}