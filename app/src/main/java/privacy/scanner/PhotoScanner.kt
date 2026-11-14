package privacy.scanner

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.util.UUID

object PhotoScanner {

    fun scanPhotoUri(context: Context, uri: Uri, fileName: String, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)

                if (hasLatLong) {
                    val lat = latLong[0]
                    val lon = latLong[1]
                    val alt = exif.getAltitude(0.0)

                    findings.add(
                        MobileFinding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "MEDIA_GPS",
                            findingType = "Photo EXIF GPS Coordinates",
                            severity = "HIGH",
                            confidence = 1.0f,
                            sourceItem = fileName,
                            location = "EXIF:GPS",
                            rawValue = "Lat: $lat, Lon: $lon, Alt: $alt",
                            maskedValue = String.format("Lat: %.2f**, Lon: %.2f**", lat, lon),
                            explanation = "Photo stores precise geographic capture coordinates in EXIF header.",
                            risk = "Stalking, physical home/work location disclosure upon sharing or uploading.",
                            recommendedAction = "Strip EXIF GPS location before publishing or sending photo.",
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                }

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                if (make != null || model != null) {
                    findings.add(
                        MobileFinding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "MEDIA_GPS",
                            findingType = "Camera Hardware Fingerprint",
                            severity = "LOW",
                            confidence = 1.0f,
                            sourceItem = fileName,
                            location = "EXIF:Device",
                            rawValue = "$make $model",
                            maskedValue = "$make $model",
                            explanation = "EXIF metadata records camera hardware manufacturer and model.",
                            risk = "Camera fingerprinting across multiple anonymous photo uploads.",
                            recommendedAction = "Clear EXIF tags using the Clean Copy action.",
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handled safely
        }

        return findings
    }
}
