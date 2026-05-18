# KoraSafe Governance for JetBrains IDEs

AI governance checks that run in your editor, on every save. Flags the patterns that block a regulated launch: leaked secrets, PII in prompts, LLM calls without error handling, destructive actions without a human-in-the-loop gate.

Part of [KoraSafe](https://korasafe.ai), the AI governance intelligence platform.

## Supported IDEs

IntelliJ IDEA, PyCharm, WebStorm, GoLand, RubyMine, AppCode. Baseline platform IntelliJ 2024.2 (`since-build=242`).

## What it catches

Seven rule classes run locally with zero network round-trip:

- **Secrets.** Anthropic, OpenAI, AWS, GitHub, Slack, JWT tokens, and hardcoded `password` / `api_key` style credentials.
- **PII.** Social Security numbers, credit card numbers, emails, phone numbers in code literals.
- **LLM calls.** Direct SDK calls to Anthropic, OpenAI, Bedrock, Vertex, Cohere, and generic `client.invoke` / `model.generate` patterns.
- **Destructive actions.** `execute()`, database `.delete()`, deploys, file writes, network `.send()` patterns.
- **Missing human-in-the-loop gates.** Flags destructive actions that ship without an approval or consent path nearby.
- **Missing error handling.** LLM calls outside a `try`/`catch` wrapper.
- **Missing rate limits.** API endpoints that hit an LLM without any rate-limit primitive in scope.

Optional cloud checks layer on regulatory mapping (EU AI Act, GDPR, NIST AI RMF) when you set an API key.

## Install

See the [install guide](docs/jetbrains/install.md) for the per-IDE walkthrough and `.zip` install path. For Marketplace installs:

1. Settings (Preferences on macOS), Plugins, Marketplace
2. Search "KoraSafe Governance"
3. Install, restart the IDE
4. Tools, KoraSafe, Set API key (only needed for cloud checks)

## Use it

- **KoraSafe tool window.** Right rail. Click the icon. Findings group by severity. Click to jump to source.
- **Inline inspections.** Squiggles in the editor on save. Hover for the finding title, severity, and regulation mapping.
- **Intention actions.** Press Alt+Enter (Option+Return on macOS) on an underlined finding for quick fixes.
- **Actions** (Cmd+Shift+A on macOS, Ctrl+Shift+A on other platforms, search "KoraSafe"):
  - `KoraSafe: Scan Current File`
  - `KoraSafe: Scan Project`
  - `KoraSafe: Refresh rules manifest`
  - `KoraSafe: Export evidence bundle`
  - `KoraSafe: Set API key`
  - `KoraSafe: Copy MCP auth token`

## Policy as code

Place `.korasafe/policy.yaml` at your project root to tune local rules. The plugin reloads on change. Malformed YAML is non-fatal and appears in the IDE event log.

```yaml
version: 1
languages: [python, typescript, javascript, go]
severity:
  pii: warning
  secrets: error
ignore:
  - tests/**
  - vendor/**
```

## Cloud governance (opt-in)

Set an API key with `KoraSafe: Set API key` to layer on regulation mapping, organization policy sync, and evidence-bundle export. The first cloud call after enabling shows a consent dialog. Cloud checks are off by default and never run in untrusted projects.

## MCP server

When `korasafe.mcpEnabled` is `true` (default), the plugin starts a local MCP server bound to `127.0.0.1:7741`. Use `KoraSafe: Copy MCP auth token` to retrieve a bearer token; the bound port is logged to the IDE event log. MCP clients (Claude Code, JetBrains AI Assistant, others) can call `scan_file`, `scan_workspace`, `get_finding_detail`, `get_policy`, `get_rules_manifest`, `dismiss_finding` against your project.

Example client config:

```json
{
  "mcpServers": {
    "korasafe": {
      "url": "http://127.0.0.1:7741/mcp",
      "headers": {
        "Authorization": "Bearer <copied-token>"
      }
    }
  }
}
```

## Evidence bundles

Run `KoraSafe: Export evidence bundle` to save a signed zip containing `manifest.json`, `policy.yaml`, `findings.json`, `signatures.json`, and `report.pdf`. When an API key is set, the bundle is signed by the KoraSafe cloud signer; otherwise a local Ed25519 development key signs the bundle and marks it self-issued.

Verify a bundle with the SDK:

```sh
korasafe verify ./korasafe-evidence-bundle.zip
```

## Migration from VS Code

If you already use KoraSafe Governance in VS Code, see the [migration guide](docs/jetbrains/migrate-from-vscode.md). Your `.korasafe/policy.yaml`, API key, and organization policy pack all carry over.

## Privacy

- Local rules run entirely in the IDE process. No code leaves your machine.
- Cloud checks (opt-in) send file content over TLS, authenticated with your API key. The key is stored in IntelliJ PasswordSafe (OS keychain).
- Telemetry is opt-in and respects the IDE's data sharing level.

Full details: https://korasafe.ai/privacy/jetbrains-extension

## Documentation

- [Install guide](docs/jetbrains/install.md)
- [Migration from VS Code](docs/jetbrains/migrate-from-vscode.md)
- [JetBrains vs VS Code feature differences](docs/jetbrains/jetbrains-vs-vscode.md)
- [Marketplace listing copy](docs/marketplace-listing.md)
- [Privacy policy](https://korasafe.ai/privacy/jetbrains-extension)
- [KoraSafe platform documentation](https://korasafe.ai)

## Module structure

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

## QA and publish

The multi-IDE QA and Marketplace publish runbook lives in `docs/qa/jetbrains-multi-ide-qa.md`.

Pull requests run Gradle checks, package the plugin, run IntelliJ Plugin Verifier, and upload plugin artifacts. Marketplace publishing is tag-gated on `jetbrains-v*` through `.github/workflows/publish.yml` and requires Anwesa to configure the JetBrains Marketplace publisher account plus repository secrets.

## Support

- Platform docs: https://korasafe.ai
- Report a bug: https://github.com/korasafe/jetbrains-extension/issues
- Email: Contact-us@korasafe.ai
