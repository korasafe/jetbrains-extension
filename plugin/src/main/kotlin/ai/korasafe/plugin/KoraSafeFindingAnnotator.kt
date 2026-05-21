package ai.korasafe.plugin

import ai.korasafe.analyzers.Severity
import ai.korasafe.plugin.services.KoraSafePluginService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class KoraSafeFindingAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        if (element != file.firstChild) return
        val languageId = file.language.id.lowercase()
        val result = element.project.service<KoraSafePluginService>().analyzeText(file.text, languageId)
        result.findings.forEach { finding ->
            val severity = when (finding.severity) {
                Severity.Critical, Severity.High -> HighlightSeverity.ERROR
                Severity.Medium -> HighlightSeverity.WARNING
                Severity.Low -> HighlightSeverity.WEAK_WARNING
            }
            holder.newAnnotation(severity, "${finding.rule}: ${finding.message}")
                .tooltip(buildString {
                    append("<b>KoraSafe</b><br/>")
                    append(finding.remediation)
                    if (finding.regulationRefs.isNotEmpty()) append("<br/>${finding.regulationRefs.joinToString(", ")}")
                })
                .create()
        }
    }
}
