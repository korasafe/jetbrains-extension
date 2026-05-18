package ai.korasafe.plugin

import ai.korasafe.analyzers.Finding
import ai.korasafe.analyzers.Severity
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class KoraSafeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = KoraSafeToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class KoraSafeToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val store = project.service<KoraSafeFindingStore>()
    private val body = JPanel()

    init {
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        body.layout = BoxLayout(body, BoxLayout.Y_AXIS)
        add(JBScrollPane(body), BorderLayout.CENTER)
        store.onChange = { render() }
        render()
    }

    private fun render() {
        body.removeAll()
        val result = store.latestResult
        if (result == null) {
            body.add(header("KoraSafe governance", "Run KoraSafe: Scan Current File or open a file with inspections enabled."))
            refresh()
            return
        }

        body.add(header("KoraSafe governance", store.latestFileName))
        body.add(scorePanel(result.summary.total, result.summary.critical, result.summary.high, result.summary.medium, result.summary.low))
        body.add(Box.createVerticalStrut(10))

        if (result.findings.isEmpty()) {
            body.add(messagePanel("No findings", "This file has no local KoraSafe governance findings."))
        } else {
            result.findings.forEach { finding ->
                body.add(findingPanel(finding))
                body.add(Box.createVerticalStrut(8))
            }
        }
        refresh()
    }

    private fun refresh() {
        body.revalidate()
        body.repaint()
    }

    private fun header(title: String, subtitle: String): JComponent =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 0, 12, 0)
            add(JBLabel(title).apply {
                font = font.deriveFont(Font.BOLD, 16f)
            }, BorderLayout.NORTH)
            add(JBLabel(subtitle).apply {
                foreground = JBColor.GRAY
            }, BorderLayout.SOUTH)
        }

    private fun scorePanel(total: Int, critical: Int, high: Int, medium: Int, low: Int): JComponent =
        JPanel(GridBagLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
            )
            val values = listOf(
                "Findings" to total,
                "Critical" to critical,
                "High" to high,
                "Medium" to medium,
                "Low" to low,
            )
            values.forEachIndexed { index, (label, value) ->
                add(metric(label, value), GridBagConstraints().apply {
                    gridx = index
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(0, 4, 0, 4)
                })
            }
        }

    private fun metric(label: String, value: Int): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JBLabel(value.toString()).apply {
                font = font.deriveFont(Font.BOLD, 18f)
            })
            add(JBLabel(label).apply {
                foreground = JBColor.GRAY
            })
        }

    private fun findingPanel(finding: Finding): JComponent =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
            )
            add(JPanel(BorderLayout()).apply {
                add(JBLabel("${finding.severity.label()} | ${finding.rule} | line ${finding.line}").apply {
                    foreground = finding.severity.color()
                    font = font.deriveFont(Font.BOLD)
                }, BorderLayout.NORTH)
                add(JBLabel("<html>${finding.message.escapeHtml()}</html>"), BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
                add(JBLabel("<html><b>Evidence:</b> ${finding.evidence.escapeHtml()}</html>"))
                add(JBLabel("<html><b>Remediation:</b> ${finding.remediation.escapeHtml()}</html>"))
                if (finding.regulationRefs.isNotEmpty()) {
                    add(referenceLinks(finding.regulationRefs))
                }
            }, BorderLayout.CENTER)
        }

    private fun referenceLinks(refs: List<String>): JComponent =
        JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            refs.forEach { ref ->
                add(JButton(ref).apply {
                    isBorderPainted = false
                    isContentAreaFilled = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    toolTipText = "Open regulation reference"
                    addActionListener { BrowserUtil.browse(regulationUrl(ref)) }
                })
            }
        }

    private fun messagePanel(title: String, detail: String): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
            )
            add(JBLabel(title).apply { font = font.deriveFont(Font.BOLD) })
            add(JBLabel(detail).apply { foreground = JBColor.GRAY })
        }

    private fun Severity.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

    private fun Severity.color(): JBColor =
        when (this) {
            Severity.Critical -> JBColor(0xB91C1C, 0xFCA5A5)
            Severity.High -> JBColor(0xC2410C, 0xFDBA74)
            Severity.Medium -> JBColor(0xA16207, 0xFDE68A)
            Severity.Low -> JBColor(0x047857, 0x6EE7B7)
        }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun regulationUrl(ref: String): String =
        "https://korasafe.ai/glossary?ref=" + ref.replace(" ", "%20")
}
