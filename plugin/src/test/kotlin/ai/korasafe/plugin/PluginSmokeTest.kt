package ai.korasafe.plugin

import kotlin.test.Test
import kotlin.test.assertNotNull

class PluginSmokeTest {
    @Test
    fun actionCanBeConstructed() {
        assertNotNull(KoraSafeScanAction())
    }
}
