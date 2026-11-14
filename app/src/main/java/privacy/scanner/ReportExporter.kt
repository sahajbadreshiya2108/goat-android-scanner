package privacy.scanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ReportExporter {

    fun exportScanToJson(scan: DeviceScan, findings: List<MobileFinding>): String {
        val root = JSONObject()
        root.put("schema_version", "1.0.0")
        root.put("scanner_platform", "ANDROID_COMPANION")
        root.put("scan_id", scan.id)
        root.put("timestamp", scan.timestamp)
        root.put("device_model", scan.deviceModel)
        root.put("manufacturer", scan.manufacturer)
        root.put("android_version", scan.androidVersion)
        root.put("sdk_version", scan.sdkVersion)
        root.put("privacy_score", scan.privacyScore)

        val summary = JSONObject()
        summary.put("total_findings", scan.totalFindings)
        summary.put("critical", scan.criticalCount)
        summary.put("high", scan.highCount)
        summary.put("medium", scan.mediumCount)
        summary.put("low", scan.lowCount)
        root.put("summary", summary)

        val findingsArray = JSONArray()
        for (f in findings) {
            val fo = JSONObject()
            fo.put("id", f.id)
            fo.put("category", f.category)
            fo.put("finding_type", f.findingType)
            fo.put("severity", f.severity)
            fo.put("confidence", f.confidence)
            fo.put("source_item", f.sourceItem)
            fo.put("location", f.location)
            fo.put("masked_value", f.maskedValue)
            fo.put("explanation", f.explanation)
            fo.put("risk", f.risk)
            fo.put("recommended_action", f.recommendedAction)
            fo.put("timestamp", f.timestamp)
            findingsArray.put(fo)
        }
        root.put("findings", findingsArray)

        return root.toString(2)
    }

    fun saveReportFile(context: Context, jsonStr: String, scanId: String): File {
        val dir = File(context.filesDir, "exported_reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "android_privacy_audit_${scanId.take(8)}.json")
        file.writeText(jsonStr)
        return file
    }
}
