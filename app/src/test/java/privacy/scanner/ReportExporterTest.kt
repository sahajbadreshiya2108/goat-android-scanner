package privacy.scanner

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReportExporterTest {

    @Test
    fun testExportJsonStructure() {
        val scan = DeviceScan(
            id = "test-scan-123",
            timestamp = "1700000000",
            deviceModel = "Pixel 8",
            manufacturer = "Google",
            androidVersion = "14",
            sdkVersion = 34,
            privacyScore = 75,
            totalFindings = 2,
            criticalCount = 0,
            highCount = 1,
            mediumCount = 1,
            lowCount = 0
        )

        val findings = listOf(
            MobileFinding(
                id = "finding-1",
                scanId = "test-scan-123",
                category = "MEDIA_GPS",
                findingType = "Photo EXIF GPS Coordinates",
                severity = "HIGH",
                confidence = 1.0f,
                sourceItem = "vacation.jpg",
                location = "EXIF:GPS",
                rawValue = "Lat: 37.77, Lon: -122.41",
                maskedValue = "Lat: 37.77**, Lon: -122.41**",
                explanation = "EXIF GPS embedded in photo.",
                risk = "Location stalking.",
                recommendedAction = "Strip EXIF.",
                timestamp = "1700000000"
            )
        )

        val jsonStr = ReportExporter.exportScanToJson(scan, findings)
        val obj = JSONObject(jsonStr)

        assertEquals("1.0.0", obj.getString("schema_version"))
        assertEquals("ANDROID_COMPANION", obj.getString("scanner_platform"))
        assertEquals("test-scan-123", obj.getString("scan_id"))
        assertEquals("Pixel 8", obj.getString("device_model"))
        assertEquals(75, obj.getInt("privacy_score"))

        val array = obj.getJSONArray("findings")
        assertEquals(1, array.length())
        assertEquals("Photo EXIF GPS Coordinates", array.getJSONObject(0).getString("finding_type"))
    }
}
