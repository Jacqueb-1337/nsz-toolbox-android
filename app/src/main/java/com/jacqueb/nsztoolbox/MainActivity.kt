package com.jacqueb.nsztoolbox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.jacqueb.nsztoolbox.databinding.ActivityMainBinding
import java.io.File
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

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        binding.btnChooseFolder.setOnClickListener {
            launchFolderPicker()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            // Let output folder get selected before handling send
            startActivityForResult(Intent(), REQUEST_CODE_HANDLE_SEND)
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
        var logStream: OutputStream? = null
        try {
            val inputUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                ?: throw Exception("No file shared to app.")

            if (outputTreeUri == null) {
                throw Exception("No output folder selected.")
            }

            val tree = DocumentFile.fromTreeUri(this, outputTreeUri!!)!!
            val logFile = tree.createFile("text/plain", "nsz_debug_${System.currentTimeMillis()}.log.txt")
                ?: throw Exception("Failed to create log file.")
            logStream = contentResolver.openOutputStream(logFile.uri)
                ?: throw Exception("Failed to open log file stream.")

            fun log(msg: String) {
                logStream.write((msg + "\n").toByteArray())
                Log.d("NSZToolbox", msg)
            }

            log("=== NSZ Toolbox Debug Log ===")
            log("Input URI: $inputUri")
            log("Output URI: $outputTreeUri")

            // Copy to cache
            val inputName = DocumentFile.fromSingleUri(this, inputUri)?.name ?: "game.nsz"
            val tempInput = File(cacheDir, inputName)
            val inputStream = contentResolver.openInputStream(inputUri)
                ?: throw Exception("Failed to open input stream.")
            tempInput.outputStream().use { it.write(inputStream.readBytes()) }
            log("Copied input to: ${tempInput.absolutePath}")

            // Create output file path
            val tempOutput = File(cacheDir, inputName.replace(".nsz", ".nsp"))
            log("Temporary output: ${tempOutput.absolutePath}")

            // Run the NSZ tool
            val py = Python.getInstance()
            val sys = py.getModule("sys")
            sys["argv"] = listOf(
                "nsz",
                "-D",
                "--verify",
                "-o", tempOutput.absolutePath,
                tempInput.absolutePath
            )
            py.getModule("nsz.__main__").callAttr("main")
            log("NSZ tool finished successfully.")

            // Move result to SAF directory
            val finalOutDoc = tree.createFile("application/octet-stream", tempOutput.name!!)
                ?: throw Exception("Failed to create final output file.")
            val outStream = contentResolver.openOutputStream(finalOutDoc.uri)
                ?: throw Exception("Failed to open output stream.")
            outStream.write(tempOutput.readBytes())
            outStream.close()

            log("Final output saved: ${finalOutDoc.uri}")
            Toast.makeText(this, "Done: ${finalOutDoc.name}", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            val msg = "Fatal error: ${e.message}"
            logStream?.write((msg + "\n").toByteArray())
            Log.e("NSZToolbox", msg, e)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        } finally {
            logStream?.close()
        }
    }
}