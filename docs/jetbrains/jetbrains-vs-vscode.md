# JetBrains vs VS Code feature differences

Companion to the [migration guide](./migrate-from-vscode.md). A focused side-by-side reference for governance teams running KoraSafe on both editors.

## Surfacing findings

| Concern | VS Code (KoraSafe Governance) | JetBrains (KoraSafe Governance) |
|---|---|---|
| Where findings appear | Activity-bar sidebar (KoraSafe shield); diagnostic squiggles in the editor; Problems panel | KoraSafe tool window on the right rail; inline inspections in the editor; Problems tool window |
| How to jump to a finding's source | Click a sidebar item | Click a tool-window tree item; double-click navigates to the line |
| How to fix or suppress | Right-click squiggle, Quick Fix | Alt+Enter (Option+Return on macOS), Intention action |
| How to refresh after a config change | KoraSafe: Refresh rules manifest (command palette) | Tools, KoraSafe, Refresh rules manifest |

## Scope and triggering

| Concern | VS Code | JetBrains |
|---|---|---|
| What triggers a scan | Save event (when `scanOnSave=true`); manual scan via command palette | Save event (when scan-on-save enabled); manual scan via action |
| Project vs workspace | Single workspace root or multi-root with per-root policy | Single project (IntelliJ's concept). Multi-project workflows use Project Tool Window, or multiple IDE windows |
| Per-root policy | Each multi-root resolves its own `.korasafe/policy.yaml` | Single `.korasafe/policy.yaml` at project root |
| Workspace trust model | VS Code workspace trust (limited support; cloud checks off in untrusted) | IntelliJ project trust (cloud checks off in untrusted) |
| Activation languages | Active for JS, TS, Python, Go, JSX, TSX | Same set; the IDE may not load all of them (e.g., PyCharm only loads Python) |

## Settings, secrets, telemetry

| Concern | VS Code | JetBrains |
|---|---|---|
| Settings location | `settings.json` under `korasafe.*` keys | Settings, Tools, KoraSafe |
| API key storage | VS Code SecretStorage (OS keychain) | IntelliJ PasswordSafe (OS keychain) |
| Telemetry opt-in respects | VS Code global telemetry setting | IDE data sharing level setting |
| Telemetry payload identifier | `client_type=vscode` | `client_type=jetbrains-<product_code>` (e.g., `jetbrains-ic`, `jetbrains-py`) |

## Local scanning

Both plugins run the same rule set bundled at build time. Cloud checks layer regulation mapping on top.

| Concern | VS Code | JetBrains |
|---|---|---|
| Local rules ship with the plugin | Yes (bundled at build) | Yes (bundled at build) |
| Rule manifest auto-refresh | Configurable TTL (default 24 hours) | Same configurable TTL |
| Offline rule update | Manifest URL supports `file://` for explicit refreshes | Same |
| Language detection | VS Code language IDs | IntelliJ Language IDs (PSI-based); mapped to the same internal language identifiers |

## Cloud checks

| Concern | VS Code | JetBrains |
|---|---|---|
| Cloud check trigger | Per-file save after API key set + `enableCloudChecks=true` | Same flow |
| First-time consent dialog | Yes, shown before the first cloud call | Yes, shown before the first cloud call |
| Authentication | Bearer token via API key | Same |
| Size limit | `korasafe.cloudMaxFileSizeKb` (default 512 KB) | Same setting, same default |

## MCP server

Both plugins start an opt-in local MCP server on `127.0.0.1:7741` by default. The server exposes the same set of tools (scan_file, scan_workspace, get_finding_detail, get_policy, get_rules_manifest, dismiss_finding) and uses bearer-token auth.

When running both plugins on the same machine, the second to start falls back to an open ephemeral port. The bound port is logged to the IDE's event log (JetBrains) or output channel (VS Code).

## Evidence export

| Concern | VS Code | JetBrains |
|---|---|---|
| Export evidence bundle action | KoraSafe: Export evidence bundle (command palette) | Tools, KoraSafe, Export evidence bundle |
| Output format | Signed zip with manifest, policy, findings, signatures, PDF report | Same format |
| Signing mode | KoraSafe cloud signer when API key set, otherwise local Ed25519 development key | Same |
| Verification | `korasafe verify <bundle.zip>` (SDK) | Same SDK, identical bundles |

## Integrations and adjacent tools

| Concern | VS Code | JetBrains |
|---|---|---|
| Editor-host extensions that pair well | Cursor, Continue (both VS Code-compatible hosts) | JetBrains AI Assistant, Junie (IntelliJ-native AI assistants) |
| Inline AI completion compatibility | Works alongside Cursor/Continue completions | Works alongside JetBrains AI Assistant completions |
| OTLP trace export | Same OTLP HTTP traces target | Same |
| Output channel logs | KoraSafe output channel (View, Output) | KoraSafe events under IDE Event Log (View, Tool Windows, Event Log) |

## What ships only on one side today

Both plugins are at parity for governance scanning, cloud checks, evidence export, and MCP server flows. A handful of features differ:

**Only in VS Code today** (JetBrains parity is on the roadmap):

- One-shot "Generate PR report" command that emits a Markdown PR body. JetBrains users invoke Tools, KoraSafe, Export evidence and copy from the JSON for now.
- VS Code keymap shortcuts (Cmd+Shift+P palette); JetBrains uses Cmd+Shift+A Find Action by default unless the VS Code Keymap plugin is installed.

**Only in JetBrains today**:

- Intention action grouping under the standard Alt+Enter menu, with KoraSafe quick fixes alongside the IDE's own refactors.
- Direct integration with the Problems tool window so KoraSafe findings show alongside the IDE's other static analysis output.

## Related documentation

- [Install guide](./install.md)
- [Migration guide from VS Code](./migrate-from-vscode.md)
- [Privacy policy](https://korasafe.ai/privacy/jetbrains-extension)
