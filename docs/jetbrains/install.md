# Install KoraSafe Governance in your JetBrains IDE

KoraSafe Governance runs on the IntelliJ Platform 2024.2 and newer (`since-build=242`). The plugin works in every JetBrains IDE built on that platform, including those listed below.

## Supported IDEs

| IDE | Minimum version | Languages activated by default |
|---|---|---|
| IntelliJ IDEA (Ultimate, Community) | 2024.2 | JavaScript, TypeScript, Python (Ultimate), Go, JSX, TSX |
| PyCharm (Professional, Community) | 2024.2 | Python |
| WebStorm | 2024.2 | JavaScript, TypeScript, JSX, TSX |
| GoLand | 2024.2 | Go |
| RubyMine | 2024.2 | (language scan disabled by default; cloud governance checks still apply) |
| AppCode | 2024.2 | (language scan disabled by default; cloud governance checks still apply) |

KoraSafe activates language-specific local scanners only when the language is loaded by the IDE. Cloud checks (governance mapping, regulation coverage) work in every IDE once an API key is set.

## Install from JetBrains Marketplace

The fastest path. KoraSafe Marketplace listing accepts the same JetBrains account you use for other plugins.

1. Open Settings (Preferences on macOS).
2. Go to Plugins, then Marketplace.
3. Search "KoraSafe Governance".
4. Click Install.
5. Restart the IDE when prompted.
6. Open Tools, KoraSafe, Set API key, paste your KoraSafe API key. (Optional, only needed for cloud checks.)

After restart, the KoraSafe tool window appears on the right rail and inline inspections start running on save.

## Install from .zip file

For air-gapped environments or pre-release builds.

1. Download `korasafe-governance-<version>.zip` from the release assets at https://github.com/korasafe/jetbrains-extension/releases.
2. Open Settings, Plugins.
3. Click the gear icon, then Install Plugin from Disk.
4. Select the downloaded zip.
5. Restart the IDE.

The plugin signature is verified on install. The IDE rejects unsigned or tampered builds.

## First-run setup

The plugin works out of the box with local-only governance rules. To enable cloud checks (regulation mapping, organization policy sync):

1. Sign in at https://korasafe.ai and create an API key under Account, API keys.
2. In the IDE, run Tools, KoraSafe, Set API key. Paste the key.
3. The plugin stores the key in IntelliJ PasswordSafe (your OS credential store). Never write the key to a file that ends up in source control.
4. Enable Settings, Tools, KoraSafe, "Enable cloud checks". On the first cloud-check call after enabling, the plugin shows a confirmation dialog that explains what data leaves your machine and links to the [privacy policy](https://korasafe.ai/privacy/jetbrains-extension).

## Project trust

KoraSafe respects IntelliJ's project trust model. Local analysis runs in untrusted projects so you can review code before granting trust. Cloud checks are disabled in untrusted projects regardless of the `enableCloudChecks` setting. Trust a project under File, Trust Project to enable the full cloud workflow.

## Configure local rules

Place `.korasafe/policy.yaml` at your project root to tune local rules. The plugin reloads the file on change. Malformed YAML is non-fatal and appears as a warning in the IDE event log.

```yaml
# .korasafe/policy.yaml
version: 1
languages: [python, typescript, javascript, go]
severity:
  pii: warning
  secrets: error
  llm_no_try: warning
  rate_limit_missing: warning
ignore:
  - tests/**
  - vendor/**
```

## Where the findings show up

- **KoraSafe tool window.** Anchored to the right rail. Click the icon or run View, Tool Windows, KoraSafe. The tree groups findings by severity (Critical, High, Medium, Low) and lets you click into the source line.
- **Inline inspections.** Each finding underlines the offending range in the editor. Hover for the finding title, severity, and regulation mapping.
- **Intention actions.** Press Alt+Enter (Option+Return on macOS) on an underlined finding to surface quick fixes (Suppress, Open in tool window, View regulation reference).
- **Event log.** Tool, KoraSafe events including scan completion, rule manifest refresh, and cloud check status.

## Running a scan manually

- Right-click in the editor, KoraSafe: Scan Current File.
- Tools, KoraSafe, Scan Project (scans all files matching the language activators).
- Tools, KoraSafe, Refresh rules manifest (pull the latest rule definitions from KoraSafe).

## Uninstall

Settings, Plugins, KoraSafe Governance, Uninstall. Restart the IDE. Uninstalling removes the local rule cache and clears the API key from IntelliJ PasswordSafe.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Tool window empty after install | Plugin not yet enabled for the open project | Trust the project (File, Trust Project), then re-run a scan |
| API key prompt missing | IDE running in safe mode | Restart with plugins enabled |
| Findings show but cloud mapping does not | Cloud checks disabled or API key missing | Check Settings, Tools, KoraSafe; re-run Set API key |
| MCP server unreachable | Port 7741 in use | The plugin falls back to an open ephemeral port; check the event log for the bound port |
| Inspection severity wrong | Project policy overrides defaults | Inspect `.korasafe/policy.yaml`; remove or adjust the severity override |

For other issues, file a bug at https://github.com/korasafe/jetbrains-extension/issues or email Contact-us@korasafe.ai.

## Related documentation

- [Migrating from VS Code](./migrate-from-vscode.md)
- [JetBrains vs VS Code feature differences](./jetbrains-vs-vscode.md)
- [Privacy policy](https://korasafe.ai/privacy/jetbrains-extension)
- [KoraSafe platform documentation](https://korasafe.ai)
