package com.example.nativeide

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class ShellExecutor(private val onOutput: (String) -> Unit) {
    private val executor = Executors.newSingleThreadExecutor()

    fun execute(command: String, workingDir: String = "/", environment: Map<String, String> = emptyMap()) {
        executor.execute {
            try {
                val pb = ProcessBuilder("/system/bin/sh", "-c", command)
                    .directory(java.io.File(workingDir))
                    .redirectErrorStream(true)

                val env = pb.environment()
                env.putAll(environment)

                val process = pb.start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    onOutput(line ?: "")
                }
                process.waitFor()
            } catch (e: Exception) {
                onOutput("Error: ${e.message}")
            }
        }
    }
}
