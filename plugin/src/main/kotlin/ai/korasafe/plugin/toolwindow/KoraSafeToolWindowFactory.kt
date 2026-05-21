package ai.korasafe.plugin.toolwindow

import ai.korasafe.plugin.services.KoraSafePluginService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import javax.swing.JPanel

class KoraSafeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        panel.add(JBLabel(project.service<KoraSafePluginService>().sidebarStatus()), BorderLayout.NORTH)
        val content = toolWindow.contentManager.factory.createContent(panel, "Governance", false)
        toolWindow.contentManager.addContent(content)
    }
}
