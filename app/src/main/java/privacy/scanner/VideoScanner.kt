package privacy.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.util.UUID

object VideoScanner {

    fun scanVideoUri(context: Context, uri: Uri, fileName: String, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            val locationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            if (!locationStr.isNullOrBlank()) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "MEDIA_GPS",
                        findingType = "Video Embedded GPS Metadata",
                        severity = "HIGH",
                        confidence = 1.0f,
                        sourceItem = fileName,
                        location = "QuickTime/ISO:Location",
                        rawValue = locationStr,
                        maskedValue = locationStr.take(12) + "** [Redacted]",
                        explanation = "Video file header embeds recorded geographic coordinates.",
                        risk = "Reveals location of home, family events, or sensitive recording sites.",
                        recommendedAction = "Transcode or sanitize video metadata before sharing.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            }

            val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!date.isNullOrBlank()) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "MEDIA_GPS",
                        findingType = "Video Recording Timestamp",
                        severity = "LOW",
                        confidence = 0.95f,
                        sourceItem = fileName,
                        location = "QuickTime:Date",
                        rawValue = date,
                        maskedValue = date,
                        explanation = "Video metadata specifies original recording timestamp.",
                        risk = "Reveals capture timeline and activity habits.",
                        recommendedAction = "Scrub metadata if distributing anonymously.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            }
        } catch (e: Exception) {
            // Handled
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }

        return findings
    }
}
