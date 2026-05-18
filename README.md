# KoraSafe JetBrains Extension

KoraSafe governance checks for JetBrains IDEs.

This repository starts as a Gradle multi-module Kotlin project:

- `core` contains the analyzer port skeleton mirroring the VS Code extension analyzer structure.
- `plugin` contains IntelliJ Platform Plugin SDK integration and UI/action registration.

Supported IDE targets declared for this plugin family:

- IntelliJ IDEA
- PyCharm
- WebStorm
- GoLand
- RubyMine
- AppCode

The baseline platform is IntelliJ Platform 2024.2 (`since-build=242`).
