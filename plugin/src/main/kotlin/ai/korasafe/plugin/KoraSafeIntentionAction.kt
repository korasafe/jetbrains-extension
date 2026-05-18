package ai.korasafe.plugin

import ai.korasafe.analyzers.Analyzer
import ai.korasafe.analyzers.Finding
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class KoraSafeIntentionAction : PsiElementBaseIntentionAction() {
    private val analyzer = Analyzer()

    override fun getText(): String = "Apply KoraSafe fix"

    override fun getFamilyName(): String = "KoraSafe governance"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
        findingAtCaret(project, editor, element) != null

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val finding = findingAtCaret(project, editor, element) ?: return
        KoraSafeApplyFix.applyToFile(project, element.containingFile, finding)
    }

    override fun startInWriteAction(): Boolean = false

    private fun findingAtCaret(project: Project, editor: Editor, element: PsiElement): Finding? {
        val file = element.containingFile ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return null
        val line = document.getLineNumber(editor.caretModel.offset) + 1
        return analyzer.analyzeCode(document.text, file.language.id.lowercase())
            .findings
            .firstOrNull { it.line == line }
    }
}
