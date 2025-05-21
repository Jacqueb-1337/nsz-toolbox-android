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
  private lateinit var outputFolderUri: Uri
  private lateinit var deleteToggle: Switch
  private lateinit var logText: TextView

  private val folderPicker =
    registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) {
        // Persist flags so we can re-open later
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
    logText      = findViewById(R.id.log_text)

    // Restore or prompt for folder
    prefs.getString("output_folder", null)?.let {
      outputFolderUri = Uri.parse(it)
    } ?: folderPicker.launch(null)

    deleteToggle.isChecked = prefs.getBoolean("delete_nsz", false)
    deleteToggle.setOnCheckedChangeListener { _, checked ->
      prefs.edit().putBoolean("delete_nsz", checked).apply()
    }

    // If launched via “Share”
    if (intent?.action == Intent.ACTION_SEND) {
      intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        ?.let { processNSZ(it) }
    }
  }

  private fun processNSZ(inputUri: Uri) {
    try {
      // Derive output filename
      val name       = inputUri.lastPathSegment ?: "input.nsz"
      val outputName = name.substringBeforeLast('.') + ".nsp"

      // Convert the persisted tree URI into a DocumentFile directory
      val dir = DocumentFile.fromTreeUri(this, outputFolderUri)
        ?: throw IllegalArgumentException("Invalid output folder URI")

      // Now create a new file in that folder
      val newFile = dir.createFile(
        "application/octet-stream",
        outputName
      ) ?: throw Exception("Could not create $outputName")

      // Stream decompress into it
      contentResolver.openOutputStream(newFile.uri)!!.use { out ->
        contentResolver.openInputStream(inputUri)!!.use { inp ->
          ZstdInputStream(inp).use { zstdIn ->
            zstdIn.copyTo(out)
          }
        }
      }

      // Optionally delete original
      if (deleteToggle.isChecked) {
        DocumentsContract.deleteDocument(contentResolver, inputUri)
      }

      logText.text = "Done! Saved as $outputName"
    }
    catch (e: Exception) {
      logText.text = "Error: ${e.localizedMessage}"
    }
  }
}