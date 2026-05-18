# Migrating from KoraSafe for VS Code to KoraSafe for JetBrains

If you already use KoraSafe Governance in VS Code (or Cursor / Continue), the JetBrains plugin gives you the same finding catalog and policy model with IntelliJ-native surfaces. Your `.korasafe/policy.yaml`, KoraSafe API key, and organization policy pack all carry over.

## What carries over without changes

- **Your `.korasafe/policy.yaml`** at the project root reads the same in both plugins. No conversion step.
- **Your `.korasafe/ignore` file** reads the same in both. Identical glob syntax.
- **Your KoraSafe API key** authenticates the same on both sides. The same key works on both clients.
- **Your organization's policy pack** loads from the same KoraSafe API endpoint and applies the same finding catalog.
- **Cloud finding semantics** (severity, rule IDs, regulation mappings) match. A finding flagged "secrets-stripe-live" in VS Code surfaces with the same rule ID in JetBrains.

## What changes (and where to find it)

| VS Code concept | JetBrains equivalent | Where to find it |
|---|---|---|
| Activity bar shield icon | Tool window icon on the right rail | View, Tool Windows, KoraSafe |
| Sidebar finding tree | KoraSafe tool window tree | Right rail panel |
| Editor squiggle | Inline inspection underline | Same place in editor; press Alt+Enter for fixes |
| Hover tooltip | Inline inspection tooltip | Hover over the underline |
| Quick fix (lightbulb) | Intention action (Alt+Enter / Option+Return) | Same shortcut model |
| Problems panel | KoraSafe tool window plus the IDE's Problems tool window | View, Tool Windows, Problems for the IDE-wide list |
| Output channel | IDE event log | View, Tool Windows, Event Log |
| Command palette | Find Action (Cmd+Shift+A / Ctrl+Shift+A) | Search "KoraSafe" |
| SecretStorage | IntelliJ PasswordSafe | Both wrap the OS keychain; same security guarantees |
| Workspace trust | Project trust | File, Trust Project |
| `settings.json` | IDE Settings (Preferences on macOS) | Settings, Tools, KoraSafe |
| `KoraSafe: Scan workspace` command | Tools, KoraSafe, Scan Project action | Same scope |
| `KoraSafe: Export evidence bundle` command | Tools, KoraSafe, Export evidence action | Same output format |

## Settings migration

VS Code stores KoraSafe settings under `korasafe.*` keys in `settings.json`. JetBrains stores them under the equivalent keys in IDE Settings, Tools, KoraSafe.

Copy these values across when you set up the JetBrains plugin:

| VS Code setting | JetBrains setting | Notes |
|---|---|---|
| `korasafe.apiUrl` | API URL | Same default `https://korasafe.ai` |
| `korasafe.enableCloudChecks` | Enable cloud checks | Same default `false` |
| `korasafe.manifestUrl` | Rules manifest URL | Same |
| `korasafe.manifestRefreshHours` | Manifest refresh hours | Same |
| `korasafe.mcpEnabled` | Enable MCP server | Same default `true` |
| `korasafe.mcpPort` | MCP port | Same default `7741`; ephemeral fallback |
| `korasafe.scanOnSave` | Scan on save | Same default `true` |
| `korasafe.severityThreshold` | Minimum severity | Same |
| `korasafe.telemetryEnabled` | Enable telemetry | Same default `false`; respects the IDE's data sharing level |
| `korasafe.tenantId` | Tenant ID | Optional, attached to OTLP spans when set |
| `korasafe.otlpEnabled`, `otlpEndpoint`, `otlpHeaders` | OTLP traces config | Identical schema |

The API key migrates separately. Use the JetBrains action Tools, KoraSafe, Set API key and paste the same key you used in VS Code. The key is stored in IntelliJ PasswordSafe (OS keychain).

## Running both side by side

Many teams keep KoraSafe installed in both editors during a transition. Both clients hit the same API and the same organization policy pack. No coordination is required.

Two things to know:

- The MCP server in each plugin binds to `127.0.0.1:7741` by default. If both run on the same machine, the second to start picks an open ephemeral port. Use the relevant client's "Copy MCP auth token" action to read the bound port from the event log.
- Telemetry events from both clients arrive at the same KoraSafe ingestion endpoint when enabled. They carry distinct `client_type` fields (`vscode` vs `jetbrains-ic`, `jetbrains-py`, etc) so you can split out adoption metrics if your org tracks them.

## Workflow muscle-memory

If you reach for VS Code shortcuts in JetBrains, the IDE supports a "VS Code Keymap" plugin that maps Cmd+P, Cmd+Shift+P, Cmd+B, Alt+Click, etc to their IntelliJ equivalents. The Find Action invocation for KoraSafe commands is Cmd+Shift+P with that keymap installed (otherwise Cmd+Shift+A on macOS / Ctrl+Shift+A on other platforms).

## What to do on day one

1. Install the JetBrains plugin per the [install guide](./install.md).
2. Open one of your projects that already has a `.korasafe/policy.yaml`.
3. Trust the project (File, Trust Project).
4. Run Tools, KoraSafe, Set API key, paste your existing key.
5. Run Tools, KoraSafe, Scan Project.
6. Open the KoraSafe tool window on the right rail and confirm the finding set matches what you see in VS Code on the same project.
7. If anything is missing, file a comparison-bug at https://github.com/korasafe/jetbrains-extension/issues with the rule ID, file path, and the VS Code finding for reference.

## What I might lose for now

The JetBrains plugin is at parity for the local + cloud governance scanning, evidence export, and MCP server flows. A few VS Code features ship later in the JetBrains roadmap:

- **Cursor and Continue compatibility.** These are VS Code-host extensions; their JetBrains analogs (Junie, JetBrains AI Assistant) integrate differently. KoraSafe's IntelliJ inspection runs alongside them in the same IDE.
- **Single-button "Generate PR report" command.** Manual export through Tools, KoraSafe, Export evidence works today; a one-shot PR-body markdown formatter ships in a later release.

If a VS Code feature you depend on is missing, file an issue and include the VS Code version + the command name. The JetBrains roadmap covers the major flows; smaller niceties land in subsequent releases.

## Related documentation

- [Install guide](./install.md)
- [JetBrains vs VS Code feature differences](./jetbrains-vs-vscode.md)
- [Privacy policy](https://korasafe.ai/privacy/jetbrains-extension)
