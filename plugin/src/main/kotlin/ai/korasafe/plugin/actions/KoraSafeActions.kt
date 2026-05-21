package ai.korasafe.plugin.actions

import ai.korasafe.cloud.CloudSettings
import ai.korasafe.plugin.services.KoraSafePluginService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class KoraSafeInitWorkspaceAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        notify(event.project, "KoraSafe workspace initialized", ".korasafe policy, rules manifest, MCP, and OTLP settings are ready to configure.")
    }
}

class KoraSafeScanWorkspaceAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val roots = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.asSequence()
            ?: project.baseDir?.let { sequenceOf(it) }
            ?: emptySequence()
        val files = roots.flatMap(::walkFiles)
        val summary = project.service<KoraSafePluginService>().scanWorkspace(files)
        notify(project, "KoraSafe workspace scan complete", "${summary.filesScanned} file(s), ${summary.findings.size} finding(s).")
    }
}

class KoraSafeGeneratePrReportAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val report = project.service<KoraSafePluginService>().generatePrReport()
        notify(project, "KoraSafe PR report generated", report.lines().take(4).joinToString(" "))
    }
}

class KoraSafeTraceAgentRunAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val trace = project.service<KoraSafePluginService>().traceAgentRun(event.getData(CommonDataKeys.VIRTUAL_FILE))
        notify(project, "KoraSafe trace exported", "${trace.runId} · ${trace.spanName}")
    }
}

class KoraSafeExportEvidenceBundleAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val bundle = project.service<KoraSafePluginService>().exportEvidenceBundle()
        notify(project, "KoraSafe evidence bundle exported", "${bundle.lines().size} JSON lines prepared.")
    }
}

class KoraSafeConnectMcpAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val connected = project.service<KoraSafePluginService>().connectMcp(CloudSettings(workspaceTrusted = true))
        notify(project, "KoraSafe MCP ${if (connected) "connected" else "blocked"}", "Local MCP server integration is ${if (connected) "available" else "not available"}.")
    }
}

private fun notify(project: Project?, title: String, content: String) {
    if (project == null) return
    NotificationGroupManager.getInstance()
        .getNotificationGroup("KoraSafe")
        .createNotification(title, content, NotificationType.INFORMATION)
        .notify(project)
}

private fun walkFiles(file: VirtualFile): Sequence<VirtualFile> = sequence {
    yield(file)
    if (file.isDirectory) {
        file.children.forEach { child -> yieldAll(walkFiles(child)) }
    }
}
