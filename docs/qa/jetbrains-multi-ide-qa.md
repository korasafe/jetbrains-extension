# JetBrains Multi-IDE QA And Publish Runbook

This runbook is the execution record template for vscode-extension #87. Keep the final filled matrix in the #87 completion comment and attach screenshots from a built plugin ZIP, not only a development sandbox.

## Headless CI

Pull requests and main pushes run:

- `gradle check buildPlugin` on JDK 17 and 21
- `gradle buildPlugin verifyPlugin -PkorasafeVerifyIde=<ide>` in a per-IDE verifier matrix on JDK 17
- artifact upload for `plugin/build/distributions/*.zip` and verifier reports

The verifier matrix is configured in `.github/workflows/ci.yml` and `plugin/build.gradle.kts` for the IDEs exposed by the IntelliJ Platform Gradle Plugin 2.5 verifier product list:

| IDE | Build used for verifier | Manual smoke |
|---|---:|---|
| IntelliJ IDEA Ultimate | 2024.2.5 | Required |
| IntelliJ IDEA Community | 2024.2.5 | Required |
| PyCharm Professional | 2024.2.5 | Required |
| PyCharm Community | 2024.2.5 | Required |
| WebStorm | 2024.2.5 | Required |
| GoLand | 2024.2.5 | Required |
| RubyMine | 2024.2.5 | Required |
| AppCode | Manual only | Required if a supported local binary is available |

Rider is an optional ninth compatibility pass after the required eight are green.

AppCode is intentionally manual-only in CI because the current IntelliJ Platform Gradle Plugin verifier type list no longer exposes an AppCode binary target. Keep AppCode in the smoke matrix for the day-of QA pass and record it as skipped only when no supported local binary is available.

## Manual Smoke Matrix

Use the release ZIP from the CI artifact or `plugin/build/distributions/`. For each IDE, install the ZIP through the plugin settings dialog, restart if prompted, open the matching fixture project, and record the outcome.

| IDE | Fixture | Startup clean | Settings visible | Local scan | policy.yaml | Rules manifest | Tool window | Inspection | Intention | Trust gate | MCP cloud | Telemetry | Result | Notes |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| IntelliJ IDEA Ultimate | Java/Kotlin | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| IntelliJ IDEA Community | Java/Kotlin | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| PyCharm Professional | Python | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| PyCharm Community | Python | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| WebStorm | JavaScript/TypeScript | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| GoLand | Go | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| RubyMine | Ruby | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | |
| AppCode | Swift/Objective-C | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Skip only if no supported local binary is available. |

## Smoke Cases

- Startup clean: IDE log has no fatal plugin exception after installation.
- Settings visible: plugin settings expose API key, workspace trust, telemetry, rules manifest, and policy location state.
- Local scan: fixture finding count and severity match the expected baseline.
- Clean file: clean fixture produces no false-positive gutter markers.
- Tool window navigation: selecting a finding focuses the expected file and line.
- Regulation reference: reference links open the expected detail target or external URL.
- Intention action: Alt+Enter shows an apply-fix action and the resulting edit matches the expected patch.
- Trust gate: untrusted project blocks cloud calls before network activity.
- Missing API key: cloud check is blocked with a clear settings path and no token placeholder leaks.
- MCP connect: authenticated websocket connects and heartbeat remains healthy for the smoke interval.
- MCP failure: timeout or auth failure is visible and local findings remain available.
- Telemetry off: telemetry is absent unless JetBrains data sharing and the KoraSafe telemetry setting are both enabled.
- Offline rules cache: cached rules are usable and stale-cache state is visible.
- Malformed policy: invalid policy reports an actionable parse error without breaking local analysis.

## Marketplace Capture Checklist

Capture five launch screenshots from a built plugin artifact:

| Shot | Preferred IDE | Required content | Status |
|---|---|---|---|
| Tool window pane | IntelliJ IDEA Ultimate | Compliance score, finding list, regulation refs, selected finding | Pending |
| Inspection popup | IntelliJ IDEA Community | Gutter marker and inline inspection message | Pending |
| Intention action menu | PyCharm Community | Alt+Enter menu showing apply-fix action | Pending |
| Settings page | WebStorm | API key, trust, telemetry, rules manifest, and policy controls | Pending |
| Status bar | GoLand | KoraSafe status segment after local scan and policy load | Pending |

Do not show real API keys, tokens, customer names, production org data, dates, pricing, or unapproved marketing claims.

## Marketplace Publish Path

Publishing runs only through `.github/workflows/publish.yml` on `jetbrains-v*` tags or manual workflow dispatch.

Required repository secrets, owned by Anwesa:

- `INTELLIJ_PUBLISH_TOKEN`
- `JETBRAINS_CERTIFICATE_CHAIN`
- `JETBRAINS_PRIVATE_KEY`
- `JETBRAINS_PRIVATE_KEY_PASSWORD`

Anwesa-owned setup before the release workflow can publish:

1. Create or activate the KoraSafe publisher account at `plugins.jetbrains.com`.
2. Register the publisher/vendor profile.
3. Generate a Hub permanent token scoped for Marketplace publishing.
4. Add the token and signing material as GitHub repository secrets.
5. Confirm the desired channel, defaulting to `default` unless Lead chooses a private channel.

## Completion Comment Template

Include this block when handing back #87:

```text
SQL handoff required: No - no schema/data changes.

Headless CI:
- check:
- buildPlugin:
- verifyPlugin:
- artifact:
- publish workflow:

Manual smoke:
<paste matrix with Pass/Fail/Skipped and notes>

Marketplace captures:
<paste five-shot checklist with artifact paths or blockers>

Publish blockers:
- Anwesa publisher account:
- INTELLIJ_PUBLISH_TOKEN:
- signing secrets:
```
