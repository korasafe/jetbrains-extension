package ai.korasafe.config

import ai.korasafe.analyzers.PolicySource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PolicyLoaderTest {
    private val loader = PolicyLoader()

    @Test
    fun readsPolicyYamlWithVsCodeSchemaFields() {
        val policy = loader.parsePolicy(
            """
            autonomyTier: 2
            allowedProviders:
              - openai
              - anthropic
            approvalKeywords:
              - counsel-reviewed
            rateLimitSlaMs: 750
            disabledRules:
              - missing-error-handling
            jurisdiction: us
            """.trimIndent(),
        )

        assertEquals(PolicySource.Loaded, policy.source)
        assertEquals(2, policy.autonomyTier)
        assertEquals(listOf("openai", "anthropic"), policy.allowedProviders)
        assertEquals(listOf("counsel-reviewed"), policy.approvalKeywords)
        assertEquals(750, policy.rateLimitSlaMs)
        assertEquals(listOf("missing-error-handling"), policy.disabledRules)
        assertEquals("us", policy.jurisdiction)
    }

    @Test
    fun missingWorkspacePolicyReturnsDefaultPolicy() {
        val root = Files.createTempDirectory("korasafe-policy-test")

        val policy = loader.readWorkspacePolicy(root)

        assertEquals(PolicySource.Missing, policy.source)
        assertEquals(emptyList(), policy.allowedProviders)
        assertEquals(emptyList(), policy.approvalKeywords)
        assertEquals(emptyList(), policy.disabledRules)
    }

    @Test
    fun invalidPolicyReturnsInvalidSourceAndError() {
        val policy = loader.parsePolicy(
            """
            autonomyTier: 9
            allowedProviders: openai
            jurisdiction: moon
            """.trimIndent(),
        )

        assertEquals(PolicySource.Invalid, policy.source)
        assertNotNull(policy.error)
    }
}
