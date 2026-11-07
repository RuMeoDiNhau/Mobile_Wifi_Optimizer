package com.mobilelegend.wlo.regression

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

/** Reads the actual project XML, not a copied fixture or Android runtime stub. */
class BackupPolicyTest {
    private val androidNamespace = "http://schemas.android.com/apk/res/android"
    private val storageDomains = setOf(
        "root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref"
    )

    private fun xml(relativePath: String): Element {
        val root = requireNotNull(System.getProperty("wlo.projectRoot")) {
            "Run via :app:testDebugUnitTest so wlo.projectRoot points to the repository"
        }
        val file = File(root, "app/src/main/$relativePath")
        assertTrue("Missing source resource: $file", file.isFile)
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        return file.inputStream().use { factory.newDocumentBuilder().parse(it).documentElement }
    }

    private fun Element.elements(): List<Element> = (0 until childNodes.length)
        .mapNotNull { childNodes.item(it) as? Element }

    private fun application(): Element = xml("AndroidManifest.xml").elements()
        .single { it.tagName == "application" }

    private fun assertFullExclusion(section: Element) {
        val rules = section.elements()
        assertEquals("Every storage domain must be explicitly excluded", storageDomains.size, rules.size)
        assertTrue("No include/default fallback", rules.all { it.tagName == "exclude" })
        assertTrue("Both domain and path are required", rules.all {
            it.hasAttribute("domain") && it.hasAttribute("path")
        })
        assertEquals(storageDomains, rules.map { it.getAttribute("domain") }.toSet())
        assertTrue("Exclude the whole directory, not one file", rules.all { it.getAttribute("path") == "." })
    }

    @Test fun manifest_opts_out_of_cloud_backup_for_the_session_only_app() {
        assertEquals("false", application().getAttributeNS(androidNamespace, "allowBackup"))
    }

    @Test fun manifest_references_both_existing_rule_resources() {
        val app = application()
        val references = mapOf("fullBackupContent" to "backup_rules", "dataExtractionRules" to "data_extraction_rules")
        references.forEach { (attribute, resource) ->
            assertEquals("@xml/$resource", app.getAttributeNS(androidNamespace, attribute))
            xml("res/xml/$resource.xml")
        }
    }

    @Test fun legacy_rules_exclude_all_storage_domains_with_valid_attributes() {
        val legacy = xml("res/xml/backup_rules.xml")
        assertEquals("full-backup-content", legacy.tagName)
        assertFullExclusion(legacy)
    }

    @Test fun modern_rules_explicitly_configure_both_cloud_and_device_transfer() {
        val modern = xml("res/xml/data_extraction_rules.xml")
        assertEquals("data-extraction-rules", modern.tagName)
        assertEquals(2, modern.elements().size)
        assertEquals(setOf("cloud-backup", "device-transfer"), modern.elements().map { it.tagName }.toSet())
    }

    @Test fun modern_cloud_backup_excludes_every_domain_with_complete_paths() {
        val cloud = xml("res/xml/data_extraction_rules.xml").elements().single { it.tagName == "cloud-backup" }
        assertFullExclusion(cloud)
    }

    @Test fun device_transfer_excludes_every_domain_even_if_oem_ignores_allow_backup() {
        val transfer = xml("res/xml/data_extraction_rules.xml").elements().single { it.tagName == "device-transfer" }
        assertFullExclusion(transfer)
    }
}
