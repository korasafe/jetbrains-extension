package ai.korasafe.plugin

import ai.korasafe.analyzers.AnalysisResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class KoraSafeFindingStore(@Suppress("UNUSED_PARAMETER") private val project: Project) {
    var latestFileName: String = "No file scanned"
        private set

    var latestResult: AnalysisResult? = null
        private set

    var onChange: (() -> Unit)? = null

    fun update(fileName: String, result: AnalysisResult) {
        latestFileName = fileName
        latestResult = result
        onChange?.invoke()
    }
}
