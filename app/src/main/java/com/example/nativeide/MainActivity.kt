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
    private lateinit var btnRun: FloatingActionButton
    private lateinit var toggleGroup: MaterialButtonToggleGroup

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
        toggleGroup = findViewById(R.id.toggleGroup)

        setupEditor()
        setupListeners()

        envManager = EnvironmentManager(this) { status ->
            runOnUiThread { appendToTerminal(status) }
        }
        shellExecutor = ShellExecutor { output ->
            runOnUiThread { appendToTerminal(output) }
        }

        if (!envManager.isEnvironmentReady()) {
            envManager.setupEnvironment { success ->
                if (success) {
                    runOnUiThread { appendToTerminal("System ready for use.") }
                }
            }
        }

        // Initial mode
        setMode(false) // Default to Python
    }

    private fun setupEditor() {
        editor.colorScheme = EditorColorScheme()
        // Basic configuration
        editor.isWordwrap = false
        editor.isLineNumberEnabled = true
        editor.tabWidth = 4
    }

    private fun setupListeners() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnCpp -> setMode(true)
                    R.id.btnPython -> setMode(false)
                }
            }
        }

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
        // Use TextMateLanguage for both as a generic fallback since specific modules are not found
        // In a real project, you would load the .tmLanguage files
        if (isCpp) {
            // editor.setEditorLanguage(TextMateLanguage.create("source.cpp", true))
            if (editor.text.toString().isEmpty()) {
                editor.setText("#include <iostream>\n\nint main() {\n    std::cout << \"Hello C++!\" << std::endl;\n    return 0;\n}")
            }
        } else {
            // editor.setEditorLanguage(TextMateLanguage.create("source.python", true))
            if (editor.text.toString().isEmpty()) {
                editor.setText("print(\"Hello Python!\")")
            }
        }
    }

    private fun runCode() {
        val code = editor.text.toString()
        val tempFile: java.io.File
        val command: String
        val env = mutableMapOf<String, String>()

        // Add usr/bin to PATH
        val binPath = java.io.File(filesDir, "usr/bin").absolutePath
        env["PATH"] = "$binPath:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = java.io.File(filesDir, "usr/lib").absolutePath

        if (isCppMode) {
            tempFile = java.io.File(filesDir, "temp.cpp")
            tempFile.writeText(code)
            appendToTerminal("\n> Compiling and Running C++...")
            command = "clang++ ${tempFile.absolutePath} -o ${filesDir.absolutePath}/temp && ${filesDir.absolutePath}/temp"
        } else {
            tempFile = java.io.File(filesDir, "temp.py")
            tempFile.writeText(code)
            appendToTerminal("\n> Running Python...")
            command = "python ${tempFile.absolutePath}"
        }

        shellExecutor.execute(command, filesDir.absolutePath, env)
    }

    private fun executeCommand(command: String) {
        appendToTerminal("\n$ $command")

        val env = mutableMapOf<String, String>()
        val binPath = java.io.File(filesDir, "usr/bin").absolutePath
        env["PATH"] = "$binPath:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = java.io.File(filesDir, "usr/lib").absolutePath
        env["DISPLAY"] = ":0" // Standard X11 display

        if (command.contains("gui") || command.contains("graphics")) {
            val intent = android.content.Intent(this, GraphicsActivity::class.java)
            startActivity(intent)
        } else {
            shellExecutor.execute(command, filesDir.absolutePath, env)
        }
    }

    private fun appendToTerminal(text: String) {
        terminalOutput.append("\n$text")
        // Auto scroll to bottom
        val scrollAmount = terminalOutput.layout?.getLineTop(terminalOutput.lineCount) ?: 0
        if (scrollAmount > terminalOutput.height) {
            terminalOutput.scrollTo(0, scrollAmount - terminalOutput.height)
        }
    }
}
