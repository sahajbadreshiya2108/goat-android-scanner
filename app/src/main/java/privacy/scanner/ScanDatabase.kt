package privacy.scanner

import androidx.room.*

@Entity(tableName = "device_scans")
data class DeviceScan(
    @PrimaryKey val id: String,
    val timestamp: String,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val privacyScore: Int,
    val totalFindings: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)

@Entity(tableName = "mobile_findings")
data class MobileFinding(
    @PrimaryKey val id: String,
    val scanId: String,
    val category: String, // PERMISSION, LOCATION, MEDIA_GPS, BACKUP, NETWORK
    val findingType: String,
    val severity: String,
    val confidence: Float,
    val sourceItem: String,
    val location: String,
    val rawValue: String,
    val maskedValue: String,
    val explanation: String,
    val risk: String,
    val recommendedAction: String,
    val timestamp: String
)

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: DeviceScan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<MobileFinding>)

    @Query("SELECT * FROM device_scans ORDER BY timestamp DESC")
    suspend fun getAllScans(): List<DeviceScan>

    @Query("SELECT * FROM mobile_findings WHERE scanId = :scanId")
    suspend fun getFindingsForScan(scanId: String): List<MobileFinding>

    @Query("DELETE FROM device_scans")
    suspend fun clearAll()
}

@Database(entities = [DeviceScan::class, MobileFinding::class], version = 1, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}
