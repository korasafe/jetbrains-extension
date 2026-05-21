package ai.korasafe.analyzers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzerTest {
    @Test
    fun findsHardcodedSecretsInSkeletonPort() {
        val result = Analyzer().analyzeCode(
            code = """const key = "sk-ant-abcdefghijklmnop";""",
            languageId = "typescript",
        )

        assertEquals(1, result.summary.critical)
        assertEquals("hardcoded-secret", result.findings.single().rule)
    }

    @Test
    fun surfacesVsCodeParityGovernanceRules() {
        val result = Analyzer().analyzeCode(
            code = """
                const prompt = "patient ssn 123-45-6789";
                openai.responses.create({ model: "gpt-4.1", input: prompt });
                transfer(25000);
                eval(userSuppliedCode);
                const deps = { openai: "latest" };
            """.trimIndent(),
            languageId = "typescript",
        )

        val rules = result.findings.map { it.rule }.toSet()
        assertTrue("pii-in-prompt" in rules)
        assertTrue("missing-error-handling" in rules)
        assertTrue("missing-hitl-gate" in rules)
        assertTrue("unsafe-eval" in rules)
        assertTrue("c14-unpinned-ai-dependency" in rules)
    }
}
