package com.example.nsztoolbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.github.luben.zstd.ZstdInputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "NSZToolbox"
        private const val PREFS = "nsz_prefs"
        private const val KEY_TREE_URI = "outputTreeUri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                handleSendIntent(intent)
                finish()
            }
            else -> {
                // Your normal UI—pick your output folder once here:
                setContentView(R.layout.activity_main)
                findViewById<Button>(R.id.btnChooseFolder).setOnClickListener {
                    launchFolderPicker()
                }
            }
        }
    }

    private fun handleSendIntent(intent: Intent) {
        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (streamUri == null) {
            logDebug("No EXTRA_STREAM URI")
            return
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val treeUriString = prefs.getString(KEY_TREE_URI, null)
        if (treeUriString.isNullOrBlank()) {
            logDebug("No output folder stored")
            return
        }
        val treeUri = Uri.parse(treeUriString)

        // **Use fromSingleUri** for the incoming file
        val inputDoc = DocumentFile.fromSingleUri(this, streamUri)
        // **Use fromTreeUri** for your output folder
        val outTree = DocumentFile.fromTreeUri(this, treeUri)

        if (inputDoc == null || outTree == null) {
            logDebug("Could not resolve DocumentFile")
            return
        }

        val originalName = inputDoc.name ?: "shared-file.nsz"
        val outputName = originalName.substringBeforeLast('.') + ".nsp"

        val debugLog = outTree.createFile("text/plain", "nsz_debug.log.txt")!!
        debugWrite(debugLog.uri, """
            === NSZ Toolbox Debug Log ===
            Input URI: $streamUri
            Output tree URI: $treeUri
            Original filename: $originalName
            Output filename: $outputName

        """.trimIndent())

        // Now do the Zstd decompression via JNI
        try {
            contentResolver.openInputStream(streamUri)?.use { inStream ->
                ZstdInputStream(inStream).use { zstdIn ->
                    val outFile = outTree.createFile("application/octet-stream", outputName)!!
                    contentResolver.openOutputStream(outFile.uri)?.use { outStream ->
                        copyStream(zstdIn, outStream)
                    }
                }
            }
            debugWrite(debugLog.uri, "\n✅ Success: wrote $outputName")
        } catch (e: Exception) {
            logDebug("Exception: ${e.message}")
            debugWrite(debugLog.uri, "\n❌ Error: ${Log.getStackTraceString(e)}")
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buf = ByteArray(8 * 1024)
        var len: Int
        while (input.read(buf).also { len = it } > 0) {
            output.write(buf, 0, len)
        }
    }

    private fun debugWrite(uri: Uri, text: String) {
        try {
            contentResolver.openOutputStream(uri, "wa")?.bufferedWriter().use { it?.write(text) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing debug log", e)
        }
    }

    private fun logDebug(msg: String) {
        Log.d(TAG, msg)
    }

    private fun launchFolderPicker() {
        // your SAF folder-picker code here, storing the tree URI into prefs:
        // getSharedPreferences(PREFS, MODE_PRIVATE).edit()
        //   .putString(KEY_TREE_URI, treeUri.toString())
        //   .apply()
    }
}