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
import com.github.luben.zstd.ZstdInputStream
import com.jacqueb.nsztoolbox.R

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var outputFolderUri: Uri
    private lateinit var deleteToggle: Switch
    private lateinit var logText: TextView

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString("output_folder", uri.toString()).apply()
                outputFolderUri = uri
                Toast.makeText(this, "Output folder set.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Folder not selected.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        deleteToggle = findViewById(R.id.delete_switch)
        logText = findViewById(R.id.log_text)

        prefs.getString("output_folder", null)?.let {
            outputFolderUri = Uri.parse(it)
        } ?: folderPicker.launch(null)

        deleteToggle.isChecked = prefs.getBoolean("delete_nsz", false)
        deleteToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("delete_nsz", isChecked).apply()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                processNSZ(it)
            }
        }
    }

    private fun processNSZ(inputUri: Uri) {
        try {
            val fileName = inputUri.lastPathSegment ?: "input.nsz"
            val outputName = fileName.substringBeforeLast('.') + ".nsp"

            // Create output file via SAF
            val outUri = DocumentsContract.createDocument(
                contentResolver,
                outputFolderUri,
                "application/octet-stream",
                outputName
            ) ?: throw Exception("Cannot create output file")

            contentResolver.openOutputStream(outUri)?.use { out ->
                contentResolver.openInputStream(inputUri)?.use { inp ->
                    ZstdInputStream(inp).use { zstdIn ->
                        zstdIn.copyTo(out)
                    }
                }
            }

            if (deleteToggle.isChecked) {
                DocumentsContract.deleteDocument(contentResolver, inputUri)
            }

            logText.text = "Done! Saved as $outputName"
        } catch (e: Exception) {
            logText.text = "Error: ${e.localizedMessage}"
        }
    }
}