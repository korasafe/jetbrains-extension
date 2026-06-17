package ai.korasafe.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryReportTest {
    @Test
    fun matchesManifestsTheServerParses() {
        assertTrue(isCodeDiscoveryManifest("package.json"))
        assertTrue(isCodeDiscoveryManifest("services/api/package.json"))
        assertTrue(isCodeDiscoveryManifest("requirements.txt"))
        assertTrue(isCodeDiscoveryManifest("svc/requirements-dev.txt"))
        assertTrue(isCodeDiscoveryManifest("app\\requirements.txt"))
    }

    @Test
    fun rejectsNonManifestPaths() {
        assertFalse(isCodeDiscoveryManifest("src/Main.kt"))
        assertFalse(isCodeDiscoveryManifest("package.json.bak"))
        assertFalse(isCodeDiscoveryManifest("my-requirements.md"))
    }

    @Test
    fun sanitizesWorkspaceIdToServerPattern() {
        val pattern = Regex("^[a-zA-Z0-9._:/-]{1,200}$")
        assertEquals("risk-service", sanitizeWorkspaceId("risk-service"))
        assertEquals("My-Repo-prod", sanitizeWorkspaceId("My Repo (prod)"))
        assertEquals("workspace", sanitizeWorkspaceId("   "))
        assertEquals("workspace", sanitizeWorkspaceId("***"))
        assertEquals(200, sanitizeWorkspaceId("a".repeat(500)).length)
        for (name in listOf("My Repo (prod)", "café+app", "a".repeat(500), "plain")) {
            assertTrue(pattern.matches(sanitizeWorkspaceId(name)), "sanitized id must match server pattern: $name")
        }
    }

    @Test
    fun detectsLanguageFromManifests() {
        assertEquals("typescript", detectDiscoveryLanguage(listOf("web/package.json")))
        assertEquals("typescript", detectDiscoveryLanguage(listOf("requirements.txt", "web/package.json")))
        assertEquals("python", detectDiscoveryLanguage(listOf("svc/requirements-dev.txt")))
        assertEquals("other", detectDiscoveryLanguage(listOf("go.mod", "Cargo.toml")))
        assertEquals("other", detectDiscoveryLanguage(emptyList()))
    }
}
