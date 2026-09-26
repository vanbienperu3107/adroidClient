package dev.chungjungsoo.gptmobile.opencode

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCodeBackupRulesTest {

    @Test
    fun backupRulesExcludeOpenCodeVaultAndRuntimeProfileStore() {
        assertRules("app/src/main/res/xml/backup_rules.xml", "full-backup-content")
        assertRules("app/src/main/res/xml/data_extraction_rules.xml", "data-extraction-rules")
    }

    private fun assertRules(relativePath: String, root: String) {
        val file = listOf(File(relativePath), File("../$relativePath")).firstOrNull { it.exists() }
            ?: error("Backup rules not found: $relativePath")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        assertTrue(document.documentElement.nodeName == root)
        val excludes = document.getElementsByTagName("exclude")
        val paths = (0 until excludes.length).map { excludes.item(it).attributes.getNamedItem("path").nodeValue }
        assertTrue(paths.contains("opencode-vault/"))
        assertTrue(paths.contains("opencode-profiles.preferences_pb"))
    }
}
