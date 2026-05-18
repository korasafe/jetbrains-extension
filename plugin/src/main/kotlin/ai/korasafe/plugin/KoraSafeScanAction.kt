package ai.korasafe.plugin

import ai.korasafe.analyzers.Analyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class KoraSafeScanAction : AnAction() {
    private val analyzer = Analyzer()

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val project = event.project ?: return
        val document = editor?.document ?: return
        val languageId = event.getData(CommonDataKeys.PSI_FILE)?.language?.id?.lowercase() ?: "text"
        val result = analyzer.analyzeCode(document.text, languageId)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("KoraSafe")
            .createNotification(
                "KoraSafe scan complete",
                "${result.summary.total} governance finding(s) detected.",
                NotificationType.INFORMATION,
            )
            .notify(project)
    }
}
