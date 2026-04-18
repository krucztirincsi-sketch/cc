package com.example.nativeide

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.Executors

class EnvironmentManager(private val context: Context, private val onStatusUpdate: (String) -> Unit) {

    private val executor = Executors.newSingleThreadExecutor()
    private val rootDir = context.filesDir.absolutePath + "/usr"

    fun isEnvironmentReady(): Boolean {
        return File(rootDir).exists()
    }

    fun setupEnvironment(onComplete: (Boolean) -> Unit) {
        executor.execute {
            try {
                onStatusUpdate("Extracting toolchain from assets...")
                val assetManager = context.assets
                copyAssets(assetManager, "env", rootDir)

                // Ensure everything in bin is executable
                File(rootDir, "bin").listFiles()?.forEach {
                    if (it.isFile) it.setExecutable(true)
                }

                onStatusUpdate("Environment ready.")
                onComplete(true)
            } catch (e: Exception) {
                onStatusUpdate("Error setting up environment: ${e.message}")
                onComplete(false)
            }
        }
    }

    private fun copyAssets(assetManager: android.content.res.AssetManager, path: String, targetDir: String) {
        val assets = assetManager.list(path) ?: return
        if (assets.isEmpty()) {
            // It's a file
            copyFile(assetManager, path, targetDir)
        } else {
            // It's a directory
            val dir = File(targetDir)
            if (!dir.exists()) dir.mkdirs()
            for (asset in assets) {
                val newPath = if (path.isEmpty()) asset else "$path/$asset"
                val newTargetDir = "$targetDir/$asset"
                copyAssets(assetManager, newPath, newTargetDir)
            }
        }
    }

    private fun copyFile(assetManager: android.content.res.AssetManager, filename: String, targetFile: String) {
        assetManager.open(filename).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    fun getExecutablePath(name: String): String {
        return "$rootDir/bin/$name"
    }
}
