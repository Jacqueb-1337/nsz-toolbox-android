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
import java.io.*

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
            startActivityForResult(Intent(), REQUEST_CODE_HANDLE_SEND)
            // The actual logic will be retried in onActivityResult
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
                    data?.data?.let { uri ->
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
                intent?.let { handleSend(it) }
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

        val tree = DocumentFile.fromTreeUri(this, outputTreeUri!!)
        if (tree == null || !tree.isDirectory) {
            Toast.makeText(this, "Invalid output folder", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val inputName = DocumentFile.fromSingleUri(this, inputUri)?.name ?: "game.nsz"
            val baseName = inputName.removeSuffix(".nsz")

            // Log files
            val logFile = tree.createFile("text/plain", "nsz_debug.log.txt")
            val outLogFile = tree.createFile("text/plain", "nsz_output.log.txt")

            val logStream = contentResolver.openOutputStream(logFile!!.uri)!!
            val outLogStream = contentResolver.openOutputStream(outLogFile!!.uri)!!

            val logWriter = BufferedWriter(OutputStreamWriter(logStream))
            val outWriter = BufferedWriter(OutputStreamWriter(outLogStream))

            fun log(msg: String) {
                logWriter.write("$msg\n")
                logWriter.flush()
            }

            // Redirect stdout and stderr
            System.setOut(PrintStream(object : OutputStream() {
                override fun write(b: Int) { outWriter.write(b.toChar().toString()) }
                override fun flush() { outWriter.flush() }
            }))
            System.setErr(System.out)

            log("=== NSZ Toolbox Debug Log ===")
            log("Input URI: $inputUri")
            log("Output URI (folder): $outputTreeUri")

            // Copy input NSZ to local cache
            val inputFile = File(cacheDir, inputName)
            contentResolver.openInputStream(inputUri)!!.use { input ->
                inputFile.outputStream().use { output -> input.copyTo(output) }
            }

            val outputFile = File(cacheDir, "$baseName.nsp")

            // Call NSZ CLI
            val py = Python.getInstance()
            val result = py.getModule("nsz").callAttr("main", listOf(
                "--decompress",
                "--overwrite",
                inputFile.absolutePath,
                "--output", outputFile.absolutePath
            ))

            log("NSZ command completed.")
            log("Output file path: ${outputFile.absolutePath}")
            log("NSZ returned: $result")

            if (!outputFile.exists()) {
                log("NSZ did not produce an output file.")
                Toast.makeText(this, "Conversion failed", Toast.LENGTH_LONG).show()
            } else {
                val finalOutDoc = tree.createFile("application/octet-stream", "$baseName.nsp")
                contentResolver.openOutputStream(finalOutDoc!!.uri)!!.use { finalOut ->
                    outputFile.inputStream().use { input -> input.copyTo(finalOut) }
                }
                log("Successfully saved to: ${finalOutDoc.uri}")
                Toast.makeText(this, "Done: ${finalOutDoc.name}", Toast.LENGTH_SHORT).show()
            }

            logWriter.close()
            outWriter.close()
        } catch (e: Exception) {
            try {
                val fallbackLog = tree.createFile("text/plain", "nsz_crash.log.txt")
                val crashWriter = BufferedWriter(OutputStreamWriter(contentResolver.openOutputStream(fallbackLog!!.uri)!!))
                crashWriter.write("Crash: ${e.message}\n${Log.getStackTraceString(e)}")
                crashWriter.close()
            } catch (_: Exception) {}
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}