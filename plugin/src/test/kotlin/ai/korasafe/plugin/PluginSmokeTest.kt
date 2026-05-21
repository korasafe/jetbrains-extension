package ai.korasafe.plugin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class PluginSmokeTest {
    @Test
    fun actionCanBeConstructed() {
        assertNotNull(KoraSafeScanAction())
    }

    @Test
    fun inspectionCanBeConstructed() {
        assertNotNull(KoraSafeInspection())
    }

    @Test
    fun intentionCanBeConstructed() {
        assertNotNull(KoraSafeIntentionAction())
    }

    @Test
    fun pluginXmlRegistersParityCommandsAndToolWindow() {
        val xml = Files.readString(Path.of("src/main/resources/META-INF/plugin.xml"))
        listOf(
            "KoraSafe.InitWorkspace",
            "KoraSafe.ScanWorkspace",
            "KoraSafe.GeneratePrReport",
            "KoraSafe.TraceAgentRun",
            "KoraSafe.ExportEvidenceBundle",
            "KoraSafe.ConnectMcp",
            "KoraSafeToolWindowFactory",
            "KoraSafeFindingAnnotator",
        ).forEach { marker ->
            assertTrue(xml.contains(marker), "plugin.xml should register $marker")
        }
    }
}
