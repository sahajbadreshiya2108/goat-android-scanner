package privacy.scanner

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object ArchiveScanner {

    private val SENSITIVE_NAMES = setOf(
        ".env", "id_rsa", "id_ed25519", "credentials.json", "master.key",
        "desktop.ini", "thumbs.db", ".ds_store", "location_history.json",
        "records.json", "timeline.json"
    )

    fun scanArchiveUri(context: Context, uri: Uri, fileName: String, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    var count = 0

                    while (entry != null && count < 500) {
                        val name = entry.name
                        val simpleName = name.substringAfterLast("/").lowercase()

                        if (SENSITIVE_NAMES.contains(simpleName)) {
                            findings.add(
                                MobileFinding(
                                    id = UUID.randomUUID().toString(),
                                    scanId = scanId,
                                    category = "BACKUP",
                                    findingType = "Sensitive File in Mobile Archive: $simpleName",
                                    severity = if (simpleName.contains("id_") || simpleName.contains(".env")) "CRITICAL" else "HIGH",
                                    confidence = 1.0f,
                                    sourceItem = fileName,
                                    location = "ZipEntry:$name",
                                    rawValue = name,
                                    maskedValue = name,
                                    explanation = "Archive bundles sensitive private key, credentials, or location export file.",
                                    risk = "Disclosure of identity, keys, or personal tracking history.",
                                    recommendedAction = "Extract and redact sensitive elements before sharing archive.",
                                    timestamp = System.currentTimeMillis().toString()
                                )
                            )
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                        count++
                    }
                }
            }
        } catch (e: Exception) {}

        return findings
    }
}
