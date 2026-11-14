package privacy.scanner

import android.content.Context
import androidx.room.Room
import java.util.UUID

class ScanRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        ScanDatabase::class.java,
        "mobile_privacy_scanner.db"
    ).build()

    private val dao = db.scanDao()

    suspend fun saveScan(scan: DeviceScan, findings: List<MobileFinding>) {
        dao.insertScan(scan)
        dao.insertFindings(findings)
    }

    suspend fun getAllScans(): List<DeviceScan> = dao.getAllScans()

    suspend fun getFindingsForScan(scanId: String): List<MobileFinding> = dao.getFindingsForScan(scanId)

    suspend fun clearHistory() = dao.clearAll()

    fun calculatePrivacyScore(findings: List<MobileFinding>): Int {
        var penalty = 0
        for (f in findings) {
            val weight = when (f.severity) {
                "CRITICAL" -> 35
                "HIGH" -> 18
                "MEDIUM" -> 8
                "LOW" -> 2
                else -> 0
            }
            penalty += (weight * f.confidence).toInt()
        }
        return (100 - penalty).coerceIn(0, 100)
    }
}
