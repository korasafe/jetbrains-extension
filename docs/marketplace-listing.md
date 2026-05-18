# JetBrains Marketplace listing copy

Companion to the plugin's `plugin.xml` description and the root `README.md`. Holds the listing metadata Anwesa pastes into the Marketplace dashboard when those fields don't live in `plugin.xml`. Mirror of the chrome and vscode listing docs.

## Basic info (auto-populated from plugin.xml)

- **Plugin name**: KoraSafe Governance (`<name>`)
- **Plugin ID**: `ai.korasafe.jetbrains` (`<id>`)
- **Vendor**: KoraSafe (`<vendor>`)
- **Vendor email**: Contact-us@korasafe.ai
- **Vendor URL**: https://korasafe.ai
- **Since-build**: 242 (IntelliJ Platform 2024.2)
- **Until-build**: 243.\* (IntelliJ Platform 2024.3)
- **Version**: 0.2.0 (matches Gradle `version` for tag-based releases)

## Tags

- code-quality
- security
- linter
- ai
- compliance

## Short description (200 char max, shown in search results and the listing header)

> AI governance checks in your JetBrains IDE. Flags secrets, PII in prompts, LLM calls without error handling, and destructive actions without a human approval gate. Maps findings to EU AI Act, GDPR, NIST AI RMF.

(197 chars.)

## Detailed description (rendered as HTML in `plugin.xml` `<description>`)

The marketplace listing renders the HTML in the `<description>` CDATA block. Replace the placeholder copy with the production version below.

```html
<![CDATA[
<p>
  KoraSafe Governance runs AI-specific governance checks every time you save.
  It catches the patterns that block a regulated launch: leaked API keys, PII
  in prompts, LLM calls shipped without error handling, destructive actions
  without a human approval gate, missing rate limits on routes that hit an LLM.
</p>

<p>
  Part of <a href="https://korasafe.ai">KoraSafe</a>, the AI governance
  intelligence platform for compliance, security, and GRC teams.
</p>

<h3>What it catches</h3>
<ul>
  <li><b>Secrets.</b> Anthropic, OpenAI, AWS, GitHub, Slack, JWT tokens, and hardcoded credential strings.</li>
  <li><b>PII.</b> Social Security numbers, credit card numbers, emails, phone numbers in code literals.</li>
  <li><b>LLM calls.</b> Direct SDK calls to Anthropic, OpenAI, Bedrock, Vertex, Cohere, and generic <code>client.invoke</code> / <code>model.generate</code> patterns.</li>
  <li><b>Destructive actions.</b> <code>execute()</code>, database <code>.delete()</code>, deploys, file writes, network <code>.send()</code> patterns.</li>
  <li><b>Missing human-in-the-loop gates.</b> Flags destructive actions that ship without an approval or consent path nearby.</li>
  <li><b>Missing error handling.</b> LLM calls outside a <code>try</code>/<code>catch</code> or a <code>withErrorHandling</code> wrapper.</li>
  <li><b>Missing rate limits.</b> API endpoints that hit an LLM without any rate-limit primitive in scope.</li>
</ul>

<h3>Where findings show up</h3>
<ul>
  <li>KoraSafe tool window on the right rail, grouped by severity.</li>
  <li>Inline inspections that underline the offending range and surface in the Problems tool window.</li>
  <li>Intention actions on Alt+Enter (Option+Return on macOS) with quick fixes and regulation references.</li>
</ul>

<h3>Cloud governance (opt-in)</h3>
<p>
  Set an API key and the plugin layers regulation mapping (EU AI Act, GDPR, NIST AI RMF), organization policy
  sync, and evidence-bundle export on top of the local checks. Cloud checks are off by default; a confirmation
  dialog runs before the first cloud call. Files above the configured size limit are skipped. Untrusted
  projects never trigger cloud checks regardless of settings.
</p>

<h3>What ships with the plugin</h3>
<ul>
  <li>Local rule scanner with bundled rule manifest. Zero network calls in the default install.</li>
  <li>Project-trust-aware cloud governance, gated on API key plus explicit opt-in.</li>
  <li>Local MCP server on 127.0.0.1 so MCP-capable assistants (Claude, JetBrains AI Assistant) can call <code>scan_file</code>, <code>get_finding_detail</code>, <code>get_policy</code>, <code>dismiss_finding</code> against your project.</li>
  <li>Signed evidence-bundle export for audit packets.</li>
  <li>OTLP trace export to your chosen receiver.</li>
</ul>

<h3>What data leaves your machine</h3>
<p>
  Nothing, by default. Cloud checks send file content to KoraSafe only after you set an API key and confirm
  the consent dialog. Telemetry is off by default and respects the IDE's data sharing level. Read the full
  <a href="https://korasafe.ai/privacy/jetbrains-extension">privacy policy</a> before enabling either.
</p>

<h3>Supported IDEs</h3>
<p>
  IntelliJ IDEA, PyCharm, WebStorm, GoLand, RubyMine, AppCode. Baseline platform IntelliJ 2024.2
  (<code>since-build=242</code>).
</p>
]]>
```

## Change notes (rendered in `<change-notes>`)

Maintained in plugin.xml. Initial v0.2.0 change notes below; subsequent releases append.

```html
<![CDATA[
<h3>0.2.0</h3>
<ul>
  <li>First public release on JetBrains Marketplace.</li>
  <li>Local governance rules: secrets, PII, LLM calls, destructive actions, missing HITL gates, missing error handling, missing rate limits.</li>
  <li>KoraSafe tool window, inline inspections, intention actions.</li>
  <li>Opt-in cloud checks with EU AI Act, GDPR, NIST AI RMF regulation mapping.</li>
  <li>Local MCP server on 127.0.0.1:7741 for MCP-capable assistants.</li>
  <li>Signed evidence-bundle export.</li>
  <li>OTLP trace export.</li>
</ul>
]]>
```

## Privacy and support URLs

Set in the Marketplace dashboard under Plugin Settings:

- **Plugin home page**: https://korasafe.ai
- **Issue tracker**: https://github.com/korasafe/jetbrains-extension/issues
- **Privacy policy**: https://korasafe.ai/privacy/jetbrains-extension
- **Documentation**: https://github.com/korasafe/jetbrains-extension/tree/main/docs/jetbrains

## Categories

Pick all that apply in the Marketplace listing form:

- Code Quality
- Security
- AI

## Marketplace publish flow

1. Anwesa signs in to the JetBrains Marketplace at https://plugins.jetbrains.com with a JetBrains account.
2. Click Upload Plugin, select the `korasafe-governance-0.2.0.zip` artifact built by Gradle.
3. Confirm metadata: it auto-populates from `plugin.xml`. Cross-check against this doc.
4. Set the privacy URL to `https://korasafe.ai/privacy/jetbrains-extension`.
5. Add the screenshots from the in-repo `docs/screenshots/` directory once they exist (handoff to design after the listing is approved).
6. Submit for review.

JetBrains Marketplace review timelines vary. First-time publisher reviews are 1-3 business days for plugins that pass the automated validator. Plugins requesting elevated permissions (rare) may take longer.

## Post-publish

- Update the plugin home page on https://korasafe.ai with the install link.
- Add a marketplace badge to the repo README:
  `[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/ai.korasafe.jetbrains)](https://plugins.jetbrains.com/plugin/ai.korasafe.jetbrains)`
- Open a follow-up ticket for the design team to replace screenshot placeholders with real captures.
- Post launch to #launches Slack + LinkedIn.
