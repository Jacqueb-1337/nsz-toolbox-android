package com.jacqueb.nsztoolbox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.jacqueb.nsztoolbox.databinding.ActivityMainBinding

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

        // Start Chaquopy if it hasn't been initialized
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        binding.btnChooseFolder.setOnClickListener {
            launchFolderPicker()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            // Wait for folder permission first
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
            val inputName = DocumentFile.fromSingleUri(this, inputUri)!!.name ?: "game.nsz"
            val tempInput = File(cacheDir, inputName)
            val inputStream = contentResolver.openInputStream(inputUri)!!
            tempInput.outputStream().use { it.write(inputStream.readBytes()) }

            val outputFile = File(cacheDir, inputName.replace(".nsz", ".nsp"))

            val py = Python.getInstance()
            val pyObj = py.getModule("main")
            pyObj.callAttr("convert_nsz_to_nsp", tempInput.absolutePath, outputFile.absolutePath)

            val finalOutDoc = tree.createFile("application/octet-stream", outputFile.name!!)!!
            val outStream = contentResolver.openOutputStream(finalOutDoc.uri)!!
            outStream.write(outputFile.readBytes())
            outStream.close()

            log("Finished decompression to: ${finalOutDoc.uri}")
            Toast.makeText(this, "Done: ${finalOutDoc.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            log("Error: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            logStream.close()
        }
    }
}