# KoraSafe JetBrains Extension

KoraSafe governance checks for JetBrains IDEs.

This repository starts as a Gradle multi-module Kotlin project:

- `core` contains the analyzer port skeleton mirroring the VS Code extension analyzer structure.
- `plugin` contains IntelliJ Platform Plugin SDK integration and UI/action registration.

Supported IDE targets for the v1 QA matrix:

- IntelliJ IDEA Ultimate
- IntelliJ IDEA Community
- PyCharm Professional
- PyCharm Community
- WebStorm
- GoLand
- RubyMine
- AppCode

The baseline platform is IntelliJ Platform 2024.2 (`since-build=242`).

## QA And Publish

The multi-IDE QA and Marketplace publish runbook lives in `docs/qa/jetbrains-multi-ide-qa.md`.

Pull requests run Gradle checks, package the plugin, run IntelliJ Plugin Verifier, and upload plugin artifacts. Marketplace publishing is tag-gated on `jetbrains-v*` through `.github/workflows/publish.yml` and requires Anwesa to configure the JetBrains Marketplace publisher account plus repository secrets.
