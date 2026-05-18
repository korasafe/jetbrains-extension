package ai.korasafe.config

import ai.korasafe.analyzers.Severity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RulesManifestLoaderTest {
    @Test
    fun parsesAndHashesManifestDeterministically() {
        val manifest = parseRulesManifest("""{"rules":[{"severity":"high","id":"missing-hitl"}],"version":"1"}""")

        assertEquals("1", manifest.version)
        assertEquals(1, manifest.rules.size)
        assertTrue(manifest.rules.single().enabled)
        assertEquals(Severity.High, manifest.rules.single().severity)
        assertEquals(
            manifestSha(manifest),
            manifestSha(
                manifest.copy(
                    rules = listOf(manifest.rules.single().copy(enabled = true, severity = Severity.High)),
                ),
            ),
        )
        assertNotNull(manifest.sha)
    }

    @Test
    fun diffsAddedRemovedAndChangedRulesById() {
        val diff = diffRulesManifest(
            parseRulesManifest(
                """
                {"version":"before","sha":"before-sha","rules":[
                  {"id":"removed-rule","enabled":true,"severity":"low"},
                  {"id":"changed-rule","enabled":true,"severity":"medium"},
                  {"id":"same-rule","enabled":true,"severity":"high"}
                ]}
                """.trimIndent(),
            ),
            parseRulesManifest(
                """
                {"version":"after","sha":"after-sha","rules":[
                  {"id":"added-rule","enabled":true,"severity":"critical"},
                  {"id":"changed-rule","enabled":true,"severity":"high"},
                  {"id":"same-rule","enabled":true,"severity":"high"}
                ]}
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("added-rule"), diff.added.map { it.id })
        assertEquals(listOf("removed-rule"), diff.removed.map { it.id })
        assertEquals(listOf("changed-rule"), diff.changed.map { it.after.id })
        assertFalse(isEmptyDiff(diff))
        assertEquals("Rules updated: +1 new, ~1 changed, -1 removed.", formatRuleDiffSummary(diff))
    }

    @Test
    fun fetchesManifestWithKtorClient() = kotlinx.coroutines.test.runTest {
        var acceptHeader: String? = null
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    acceptHeader = request.headers[HttpHeaders.Accept]
                    respond(
                        """{"version":"remote","rules":[{"id":"cloud-rule","enabled":true}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

        val manifest = RulesManifestFetcher(client).fetch("https://korasafe.ai/rules.json")

        assertEquals("application/json", acceptHeader)
        assertEquals("remote", manifest?.version)
        assertEquals("cloud-rule", manifest?.rules?.single()?.id)
    }

    @Test
    fun fetchReturnsNullForNonOkResponse() = kotlinx.coroutines.test.runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { respond("nope", status = HttpStatusCode.ServiceUnavailable) }
            }
        }

        assertNull(RulesManifestFetcher(client).fetch("https://korasafe.ai/rules.json"))
    }
}
