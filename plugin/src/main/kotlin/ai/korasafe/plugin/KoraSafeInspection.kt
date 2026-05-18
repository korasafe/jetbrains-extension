package ai.korasafe.plugin

import ai.korasafe.analyzers.Analyzer
import ai.korasafe.analyzers.Finding
import ai.korasafe.analyzers.Severity
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class KoraSafeInspection : LocalInspectionTool() {
    private val analyzer = Analyzer()

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return emptyArray()
        val result = analyzer.analyzeCode(document.text, file.language.id.lowercase())
        file.project.service<KoraSafeFindingStore>().update(file.name, result)

        return result.findings.mapNotNull { finding ->
            val element = file.elementAtFindingLine(finding) ?: return@mapNotNull null
            manager.createProblemDescriptor(
                element,
                finding.message,
                KoraSafeApplyFix(finding),
                finding.highlightType(),
                isOnTheFly,
            )
        }.toTypedArray()
    }
}

class KoraSafeApplyFix(private val finding: Finding) : LocalQuickFix {
    override fun getFamilyName(): String = "Apply KoraSafe fix"

    override fun getName(): String = "Apply fix: ${finding.remediation}"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile ?: return
        applyToFile(project, file, finding)
    }

    companion object {
        fun applyToFile(project: Project, file: PsiFile, finding: Finding) {
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            val lineIndex = (finding.line - 1).coerceIn(0, (document.lineCount - 1).coerceAtLeast(0))
            val lineStart = document.getLineStartOffset(lineIndex)
            val prefix = commentPrefix(file)
            val note = "$prefix KoraSafe remediation: ${finding.remediation}\n"

            WriteCommandAction.runWriteCommandAction(project, "Apply KoraSafe fix", null, Runnable {
                if (!document.text.contains(note)) {
                    document.insertString(lineStart, note)
                    PsiDocumentManager.getInstance(project).commitDocument(document)
                }
            }, file)
        }

        private fun commentPrefix(file: PsiFile): String {
            val id = file.language.id.lowercase()
            return when {
                id.contains("python") || id.contains("ruby") -> "#"
                id.contains("yaml") || id.contains("shell") -> "#"
                else -> "//"
            }
        }
    }
}

private fun PsiFile.elementAtFindingLine(finding: Finding): PsiElement? {
    val document = PsiDocumentManager.getInstance(project).getDocument(this) ?: return null
    if (document.lineCount <= 0) return this
    val line = (finding.line - 1).coerceIn(0, document.lineCount - 1)
    val startOffset = document.getLineStartOffset(line)
    val endOffset = document.getLineEndOffset(line)
    return findElementAt(startOffset) ?: findElementAt(endOffset.coerceAtLeast(startOffset)) ?: this
}

private fun Finding.highlightType(): ProblemHighlightType =
    when (severity) {
        Severity.Critical, Severity.High -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        Severity.Medium -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        Severity.Low -> ProblemHighlightType.WEAK_WARNING
    }
