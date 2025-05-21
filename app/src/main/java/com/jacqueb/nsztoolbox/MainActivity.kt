package com.jacqueb.nsztoolbox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.jacqueb.nsztoolbox.databinding.ActivityMainBinding
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var outputTreeUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_PICK_OUTPUT = 1001
        private const val REQUEST_CODE_HANDLE_SEND = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wire up the folder‐picker button
        binding.btnChooseFolder.setOnClickListener {
            launchFolderPicker()
        }

        // If the user shared an NSZ when launching, handle it
        if (intent?.action == Intent.ACTION_SEND) {
            // delay actual handling until after SAF permission is granted
            startActivityForResult(Intent(), REQUEST_CODE_HANDLE_SEND)
            handleSend(intent) 
        }
    }

    private fun launchFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_OUTPUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_OUTPUT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.also { uri ->
                        outputTreeUri = uri
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        Toast.makeText(this, "Output folder selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_CODE_HANDLE_SEND -> {
                if (intent?.action == Intent.ACTION_SEND) {
                    handleSend(intent)
                }
            }
        }
    }

    private fun handleSend(intent: Intent) {
        val inputUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (inputUri == null) {
            Toast.makeText(this, "No file to process", Toast.LENGTH_SHORT).show()
            return
        }
        if (outputTreeUri == null) {
            Toast.makeText(this, "Please choose an output folder first", Toast.LENGTH_LONG).show()
            return
        }

        // Prepare debug log
        val tree = DocumentFile.fromTreeUri(this, outputTreeUri!!)!!
        val logFile = tree.createFile("text/plain", "nsz_debug.log.txt")
        val logStream = contentResolver.openOutputStream(logFile!!.uri)!!

        fun log(msg: String) {
            logStream.write((msg + "\n").toByteArray())
        }

        log("=== NSZ Toolbox Debug Log ===")
        log("Input URI: $inputUri")
        log("Output tree URI: $outputTreeUri")

        try {
            // Create output NSP file
            val inputName = DocumentFile.fromSingleUri(this, inputUri)!!.name ?: "game.nsz"
            val outDoc = tree.createFile(
                "application/octet-stream",
                inputName.replace(".nsz", ".nsp")
            )!!
            val outStream = contentResolver.openOutputStream(outDoc.uri)!!

            log("Starting decompression...")
            // TODO: your actual NSZ→NSP logic here (Py, JNI, etc.)
            // e.g. Python.run(...) or Zstd.decompress(...)
            log("Finished decompression to: ${outDoc.uri}")

            outStream.close()
            Toast.makeText(this, "Done: ${outDoc.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            log("Error: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            logStream.close()
        }
    }
}