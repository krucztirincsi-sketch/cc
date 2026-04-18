package com.example.nativeide

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor
    private lateinit var terminalOutput: TextView
    private lateinit var terminalInput: EditText
    private lateinit var btnRun: View
    private lateinit var tvCpp: TextView
    private lateinit var layoutPython: View
    private lateinit var tvPython: TextView

    private var isCppMode = false
    private lateinit var envManager: EnvironmentManager
    private lateinit var shellExecutor: ShellExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        terminalOutput = findViewById(R.id.terminalOutput)
        terminalInput = findViewById(R.id.terminalInput)
        btnRun = findViewById(R.id.btnRun)
        tvCpp = findViewById(R.id.tvCpp)
        layoutPython = findViewById(R.id.layoutPython)
        tvPython = findViewById(R.id.tvPython)

        setupEditor()
        setupListeners()

        envManager = EnvironmentManager(this) { status ->
            runOnUiThread { appendToTerminal(status) }
        }
        shellExecutor = ShellExecutor { output ->
            runOnUiThread {
                terminalOutput.append(output)
                scrollToBottom()
            }
        }

        if (!envManager.isEnvironmentReady()) {
            envManager.setupEnvironment { success ->
                if (success) {
                    runOnUiThread {
                        appendToTerminal("System ready for use.")
                        startShell()
                    }
                }
            }
        } else {
            startShell()
        }

        // Initial mode - Load from preferences
        val prefs = getSharedPreferences("NativeIDE", MODE_PRIVATE)
        isCppMode = prefs.getBoolean("isCppMode", false)
        val savedCode = prefs.getString("savedCode", "")

        setMode(isCppMode)
        if (!savedCode.isNullOrEmpty()) {
            editor.setText(savedCode)
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("NativeIDE", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("isCppMode", isCppMode)
            putString("savedCode", editor.text.toString())
            apply()
        }
    }

    private fun setupEditor() {
        editor.colorScheme = EditorColorScheme()
        // Basic configuration
        editor.isWordwrap = false
        editor.isLineNumberEnabled = true
        editor.tabWidth = 4
    }

    private fun setupListeners() {
        tvCpp.setOnClickListener { setMode(true) }
        layoutPython.setOnClickListener { setMode(false) }

        btnRun.setOnClickListener {
            runCode()
        }

        terminalInput.setOnEditorActionListener { v, actionId, event ->
            val command = terminalInput.text.toString()
            if (command.isNotEmpty()) {
                executeCommand(command)
                terminalInput.setText("")
                true
            } else {
                false
            }
        }

        terminalOutput.movementMethod = ScrollingMovementMethod()
    }

    private fun setMode(isCpp: Boolean) {
        this.isCppMode = isCpp
        if (isCpp) {
            tvCpp.setTextColor(android.graphics.Color.parseColor("#311B92"))
            tvCpp.setBackgroundResource(R.drawable.bg_toggle_selected)
            layoutPython.setBackgroundResource(0)
            tvPython.setTextColor(android.graphics.Color.parseColor("#B39DDB"))

            if (editor.text.toString().isEmpty() || editor.text.toString() == "print(\"Hello Python!\")") {
                editor.setText("#include <iostream>\n\nint main() {\n    std::cout << \"Hello C++!\" << std::endl;\n    return 0;\n}")
            }
        } else {
            layoutPython.setBackgroundResource(R.drawable.bg_toggle_selected)
            tvPython.setTextColor(android.graphics.Color.parseColor("#311B92"))
            tvCpp.setBackgroundResource(0)
            tvCpp.setTextColor(android.graphics.Color.parseColor("#B39DDB"))

            if (editor.text.toString().isEmpty() || editor.text.toString().startsWith("#include")) {
                editor.setText("print(\"Hello Python!\")")
            }
        }
    }

    private fun startShell() {
        val env = mutableMapOf<String, String>()
        val binPath = java.io.File(filesDir, "usr/bin").absolutePath
        env["PATH"] = "$binPath:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = java.io.File(filesDir, "usr/lib").absolutePath
        env["HOME"] = filesDir.absolutePath
        env["TERM"] = "xterm"
        shellExecutor.startShell(filesDir.absolutePath, env)
    }

    private fun runCode() {
        val code = editor.text.toString()
        val tempFile: java.io.File

        if (isCppMode) {
            tempFile = java.io.File(filesDir, "temp.cpp")
            tempFile.writeText(code)
            val outputExe = java.io.File(filesDir, "temp")
            shellExecutor.execute("clang++ ${tempFile.absolutePath} -o ${outputExe.absolutePath} && chmod 755 ${outputExe.absolutePath} && ${outputExe.absolutePath}")
        } else {
            tempFile = java.io.File(filesDir, "temp.py")
            tempFile.writeText(code)
            shellExecutor.execute("python ${tempFile.absolutePath}")
        }
    }

    private fun executeCommand(command: String) {
        if (command.contains("gui") || command.contains("graphics")) {
            val intent = android.content.Intent(this, GraphicsActivity::class.java)
            startActivity(intent)
        } else {
            shellExecutor.execute(command)
        }
    }

    private fun appendToTerminal(text: String) {
        terminalOutput.append("\n$text")
        scrollToBottom()
    }

    private fun scrollToBottom() {
        terminalOutput.post {
            val scrollAmount = terminalOutput.layout?.getLineTop(terminalOutput.lineCount) ?: 0
            if (scrollAmount > terminalOutput.height) {
                terminalOutput.scrollTo(0, scrollAmount - terminalOutput.height)
            }
        }
    }
}
