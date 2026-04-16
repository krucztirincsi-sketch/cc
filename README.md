# Native IDE for Android

A mobile IDE for C++ and Python development, featuring a built-in terminal and graphical output support.

## Features
- **Code Editor**: Support for C++ and Python with syntax highlighting and auto-indentation.
- **Dual Mode**: Easily switch between C++ and Python environments.
- **Integrated Terminal**: Run shell commands, manage libraries, and execute your code.
- **Graphical Output**: Support for viewing graphical applications (simulated via X11 architecture).
- **Environment Management**: Automatic setup of the necessary runtime environments.

## Architecture
The IDE is built using Kotlin and follows a modular architecture:
- `MainActivity`: Orchestrates the UI and interaction between the editor and the terminal.
- `EnvironmentManager`: Handles the bootstrap process, downloading and extracting Clang and Python environments into the app's internal storage (`/data/data/com.example.nativeide/files/usr`).
- `ShellExecutor`: Executes shell commands with correctly configured environment variables (`PATH`, `LD_LIBRARY_PATH`, `DISPLAY`).
- `GraphicalSurfaceView`: A `SurfaceView` implementation designed to render graphical output from compiled programs.

## CI/CD
This project includes a GitHub Actions workflow that automatically builds a debug APK on every push to the main branch or feature branches. You can find the generated APKs in the "Actions" tab of the repository under the "Upload APK" step artifacts.

## Setup & Building
1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Build and run the app on an Android device or emulator (API 24+).

### Note on Environment
The IDE bundles the toolchain within the APK's assets (`app/src/main/assets/env`). On first launch, the `EnvironmentManager` extracts these files to the app's internal storage.

In this repository, the assets contain placeholder files. To make the IDE functional, you must:
1. Replace the files in `app/src/main/assets/env` with real cross-compiled binaries for your target architecture (e.g., aarch64-linux-android).
2. Ensure `clang++` and `python` are present in the `bin/` subfolder of assets.

## How to use
1. Choose your language (C++ or Python) from the top bar.
2. Write your code in the editor.
3. Tap the **Run** button to compile/execute.
4. Use the terminal at the bottom to run custom commands like `apt install` or to interact with the environment.
5. To see graphical output, run a command containing `gui` or `graphics`.
