package com.nsztoolbox

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

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

        val folder = prefs.getString("output_folder", null)
        if (folder == null) {
            folderPicker.launch(null)
        } else {
            outputFolderUri = Uri.parse(folder)
        }

        deleteToggle.isChecked = prefs.getBoolean("delete_nsz", false)
        deleteToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("delete_nsz", isChecked).apply()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                processNSZ(uri)
            }
        }
    }

    private fun processNSZ(uri: Uri) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val py = Python.getInstance()
        val script = py.getModule("nsz_script")
        try {
            val result = script.callAttr("decompress", contentResolver.openInputStream(uri), outputFolderUri.toString())
            logText.text = "Done!\n$result"

            if (prefs.getBoolean("delete_nsz", false)) {
                DocumentsContract.deleteDocument(contentResolver, uri)
            }

        } catch (e: Exception) {
            logText.text = "Error: ${e.localizedMessage}"
        }
    }
}