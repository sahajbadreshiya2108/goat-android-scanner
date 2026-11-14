package privacy.scanner

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object BackupImporter {

    fun scanBackupUri(context: Context, uri: Uri, fileName: String, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream)).readText()

                // Check for GPX / KML location tracks
                if (fileName.endsWith(".gpx", ignoreCase = true) || content.contains("<gpx", ignoreCase = true)) {
                    val latCount = Regex("<trkpt lat=\"([^\"]+)\" lon=\"([^\"]+)\"").findAll(content).count()
                    findings.add(
                        MobileFinding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "LOCATION",
                            findingType = "GPX GPS Track File Imported",
                            severity = "HIGH",
                            confidence = 1.0f,
                            sourceItem = fileName,
                            location = "GPX:Trackpoints",
                            rawValue = "$latCount GPS trackpoints identified",
                            maskedValue = "$latCount Trackpoints [Coordinates Redacted]",
                            explanation = "Imported file contains historical GPS navigation trackpoints.",
                            risk = "Reveals routes, daily movement, and visited locations.",
                            recommendedAction = "Do not share unredacted GPX tracks publicly.",
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                }

                // Check for GeoJSON / Location History JSON
                if (content.contains("coordinates") || content.contains("locations") || content.contains("latitudeE7")) {
                    findings.add(
                        MobileFinding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "LOCATION",
                            findingType = "Location History / GeoJSON Export",
                            severity = "HIGH",
                            confidence = 0.95f,
                            sourceItem = fileName,
                            location = "JSON:Locations",
                            rawValue = "Location records present in JSON",
                            maskedValue = "Location export detected",
                            explanation = "Data contains structured geographic records or timeline history.",
                            risk = "Historical location profile exposure.",
                            recommendedAction = "Scrub location coordinates prior to distribution.",
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {}

        return findings
    }
}
