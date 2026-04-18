package com.example.nativeide

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class ShellExecutor(private val onOutput: (String) -> Unit) {
    private val executor = Executors.newSingleThreadExecutor()
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null

    fun startShell(workingDir: String, environment: Map<String, String>) {
        if (process != null) return

        executor.execute {
            try {
                val pb = ProcessBuilder("/system/bin/sh", "-i")
                    .directory(java.io.File(workingDir))
                    .redirectErrorStream(true)

                pb.environment().putAll(environment)

                val p = pb.start()
                process = p
                writer = OutputStreamWriter(p.outputStream)

                val reader = BufferedReader(InputStreamReader(p.inputStream))
                var char: Int
                val buffer = StringBuilder()
                while (reader.read().also { char = it } != -1) {
                    val c = char.toChar()
                    buffer.append(c)
                    if (c == '\n' || !reader.ready()) {
                        onOutput(buffer.toString())
                        buffer.setLength(0)
                    }
                }
            } catch (e: Exception) {
                onOutput("Shell Error: ${e.message}")
            }
        }
    }

    fun execute(command: String) {
        executor.execute {
            try {
                writer?.let {
                    it.write(command + "\n")
                    it.flush()
                }
            } catch (e: Exception) {
                onOutput("Write Error: ${e.message}")
            }
        }
    }

    fun stopShell() {
        process?.destroy()
        process = null
        writer = null
    }
}
