package com.jacqueb.nsztoolbox

import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import com.chaquo.python.android.AndroidPlatform
import com.jacqueb.nsztoolbox.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var outputTreeUri: Uri? = null
    private var pendingSendIntent: Intent? = null

    companion object {
        private const val REQUEST_CODE_PICK_OUTPUT = 1001
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
            pendingSendIntent = intent
            Toast.makeText(this, "Please choose an output folder", Toast.LENGTH_LONG).show()
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
        if (requestCode == REQUEST_CODE_PICK_OUTPUT && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                outputTreeUri = uri
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Toast.makeText(this, "Output folder selected", Toast.LENGTH_SHORT).show()
                pendingSendIntent?.let { handleSend(it) }
                pendingSendIntent = null
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
        val logWriter = OutputStreamWriter(logStream)
        val logPrintWriter = PrintWriter(logWriter, true)

        fun log(msg: String) {
            logPrintWriter.println(msg)
            Log.d("NSZToolbox", msg)
        }

        try {
            val inputName = DocumentFile.fromSingleUri(this, inputUri)!!.name ?: "game.nsz"
            val tempInput = File(cacheDir, inputName)
            val inputStream = contentResolver.openInputStream(inputUri)!!
            tempInput.outputStream().use { it.write(inputStream.readBytes()) }

            val outputName = inputName.replace(".nsz", ".nsp")
            val tempOutput = File(cacheDir, outputName)

            val py = Python.getInstance()
            val builtins = py.getBuiltins()
            val sys = py.getModule("sys")
            sys["stdout"] = PyObject.fromJava(logPrintWriter)
            sys["stderr"] = PyObject.fromJava(logPrintWriter)

            val argv: PyObject = builtins.callAttr("list", arrayOf(
                "nsz", "-D", "--verify",
                "-o", tempOutput.absolutePath,
                tempInput.absolutePath
            ))

            sys["argv"] = argv
            py.getModule("nsz.__main__").callAttr("main")

            val finalOutDoc = tree.createFile("application/octet-stream", outputName)!!
            val outStream = contentResolver.openOutputStream(finalOutDoc.uri)!!
            outStream.write(tempOutput.readBytes())
            outStream.close()

            log("Finished decompression to: ${finalOutDoc.uri}")
            Toast.makeText(this, "Done: ${finalOutDoc.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            log("Error: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            logPrintWriter.close()
            logWriter.close()
            logStream.close()
        }
    }
}