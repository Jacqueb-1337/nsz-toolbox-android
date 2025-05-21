package com.jacqueb.nsztoolbox

import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.github.luben.zstd.ZstdInputStream
import java.io.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME      = "settings"
        private const val KEY_OUTPUT_URI = "output_folder"
        private const val KEY_DELETE_NSZ = "delete_nsz"
    }

    private lateinit var prefs: SharedPreferences
    private var outputFolderUri: Uri? = null
    private var pendingInputUri: Uri? = null

    private lateinit var folderPicker: ActivityResultLauncher<Uri?>
    private lateinit var deleteToggle: Switch
    private lateinit var logText:     TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs        = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        deleteToggle = findViewById(R.id.delete_switch)
        logText      = findViewById(R.id.log_text)

        // Restore output‐folder URI
        prefs.getString(KEY_OUTPUT_URI, null)?.let { uriStr ->
            Uri.parse(uriStr).takeIf { uri ->
                contentResolver.persistedUriPermissions.any { it.uri == uri }
            }?.also { outputFolderUri = it }
        }

        // Delete‐after‐convert toggle
        deleteToggle.isChecked = prefs.getBoolean(KEY_DELETE_NSZ, false)
        deleteToggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_DELETE_NSZ, checked).apply()
        }

        // SAF folder picker
        folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            treeUri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString(KEY_OUTPUT_URI, it.toString()).apply()
                outputFolderUri = it
                Toast.makeText(this, "Output folder set.", Toast.LENGTH_SHORT).show()

                pendingInputUri?.let { uri ->
                    pendingInputUri = null
                    handleFileUri(uri)
                }
            } ?: run {
                Toast.makeText(this, "Folder selection canceled.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle shared NSZ
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                pendingInputUri = uri
                handleShare(uri)
            }
        }
    }

    private fun handleShare(inputUri: Uri) {
        if (outputFolderUri == null) {
            folderPicker.launch(null)
        } else {
            handleFileUri(inputUri)
        }
    }

    private fun handleFileUri(inputUri: Uri) {
        val logBuilder = StringBuilder()
        fun log(line: String) {
            logBuilder.append(line).append('\n')
        }

        // we'll write this at the end
        var logFileDoc: DocumentFile? = null

        try {
            log("=== NSZ Toolbox Debug Log ===")
            log("Input URI: $inputUri")
            log("Output tree URI: $outputFolderUri")

            // find display name
            val originalName = queryFileName(inputUri)
                ?: inputUri.lastPathSegment
                ?: "input.nsz"
            log("Original filename: $originalName")

            val baseName = originalName.substringBeforeLast('.')
            val outputName = "$baseName.nsp"
            log("Output filename: $outputName")

            // get tree
            val tree = DocumentFile.fromTreeUri(this, outputFolderUri!!)
                ?: throw IllegalArgumentException("Invalid output‐folder URI")
            log("Resolved DocumentFile tree.")

            // create debug log file in the tree
            logFileDoc = tree.createFile("text/plain", "nsz_debug.log")
            log("Created debug log file: ${logFileDoc?.uri}")

            // create output file
            val outFile = tree.createFile("application/octet-stream", outputName)
                ?: throw Exception("Could not create $outputName")
            log("Created output file: ${outFile.uri}")

            // decompress
            log("Starting decompression...")
            contentResolver.openInputStream(inputUri)!!.use { inp ->
                contentResolver.openOutputStream(outFile.uri)!!.use { out ->
                    ZstdInputStream(inp).use { zis ->
                        val copied = zis.copyTo(out)
                        log("Copied $copied bytes.")
                    }
                }
            }
            log("Decompression finished.")

            // optional delete
            if (deleteToggle.isChecked) {
                log("Deleting original NSZ...")
                DocumentsContract.deleteDocument(contentResolver, inputUri)
                log("Original deleted.")
            }

            log("SUCCESS: wrote $outputName")
            logText.text = "Done! Check debug log in output folder."
        }
        catch (e: Exception) {
            log("ERROR: ${e.javaClass.simpleName}: ${e.localizedMessage}")
            StringWriter().use { sw ->
                e.printStackTrace(PrintWriter(sw))
                log(sw.toString())
            }
            logText.text = "Failed—see nsz_debug.log"
            Toast.makeText(this, "Conversion failed, see log.", Toast.LENGTH_LONG).show()
        }
        finally {
            // write out the debug log
            try {
                logFileDoc?.let { doc ->
                    contentResolver.openOutputStream(doc.uri)?.use { fos ->
                        fos.writer(Charsets.UTF_8).use { w ->
                            w.write(logBuilder.toString())
                        }
                    }
                }
            }
            catch (io: IOException) {
                // last‐ditch: show toast if log write fails
                Toast.makeText(this, "Could not write debug log: ${io.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**  
     * Query DISPLAY_NAME or fallback to file‐path name  
     */
    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
        }
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return File(uri.path!!).name
        }
        return null
    }
}