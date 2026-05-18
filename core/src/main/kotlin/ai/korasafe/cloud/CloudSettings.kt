package ai.korasafe.cloud

data class CloudSettings(
    val apiUrl: String = "https://korasafe.ai",
    val apiKey: String = "",
    val enableCloudChecks: Boolean = false,
    val workspaceTrusted: Boolean = true,
    val forceCloudOnUntrusted: Boolean = false,
    val telemetryEnabled: Boolean = false,
    val jetBrainsDataSharingEnabled: Boolean = false,
)

enum class CloudCheckBlockReason {
    CloudDisabled,
    MissingApiKey,
    WorkspaceUntrusted,
}

data class CloudCheckDecision(
    val allowed: Boolean,
    val reason: CloudCheckBlockReason? = null,
    val warning: String? = null,
)

private const val FORCE_CLOUD_WARNING =
    "korasafe.forceCloudOnUntrusted is not supported; KoraSafe cloud checks stay disabled in untrusted projects."

fun evaluateCloudCheckTrust(settings: CloudSettings): CloudCheckDecision {
    val warning = if (settings.forceCloudOnUntrusted) FORCE_CLOUD_WARNING else null

    if (!settings.enableCloudChecks) {
        return CloudCheckDecision(allowed = false, reason = CloudCheckBlockReason.CloudDisabled, warning = warning)
    }
    if (settings.apiKey.isBlank()) {
        return CloudCheckDecision(allowed = false, reason = CloudCheckBlockReason.MissingApiKey, warning = warning)
    }
    if (!settings.workspaceTrusted) {
        return CloudCheckDecision(allowed = false, reason = CloudCheckBlockReason.WorkspaceUntrusted, warning = warning)
    }
    return CloudCheckDecision(allowed = true, warning = warning)
}

fun shouldSendTelemetry(settings: CloudSettings): Boolean =
    settings.telemetryEnabled && settings.jetBrainsDataSharingEnabled
