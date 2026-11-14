package privacy.scanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var repository: ScanRepository
    private val findingsState = mutableStateListOf<MobileFinding>()
    private var scoreState by mutableStateOf(100)
    private var metadataState by mutableStateOf<DeviceMetadata?>(null)
    private var currentScanId = UUID.randomUUID().toString()

    // SAF photo picker
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val photoFindings = PhotoScanner.scanPhotoUri(this, it, "Selected_Photo.jpg", currentScanId)
            findingsState.addAll(photoFindings)
            scoreState = repository.calculatePrivacyScore(findingsState)
            Toast.makeText(this, "Photo scanned: ${photoFindings.size} issues found", Toast.LENGTH_SHORT).show()
        }
    }

    // SAF document/backup picker
    private val pickDocLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val docFindings = DocumentScanner.scanTextOrDocUri(this, it, "Selected_File", currentScanId)
            val backupFindings = BackupImporter.scanBackupUri(this, it, "Selected_File", currentScanId)
            findingsState.addAll(docFindings)
            findingsState.addAll(backupFindings)
            scoreState = repository.calculatePrivacyScore(findingsState)
            Toast.makeText(this, "File scanned: ${docFindings.size + backupFindings.size} findings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScanRepository(this)
        metadataState = DeviceInfo.getDeviceMetadata(this)

        setContent {
            val metadata = metadataState ?: DeviceInfo.getDeviceMetadata(this)
            val isUsb = UsbConnection.isUsbConnected(this)

            DashboardScreen(
                deviceMetadata = metadata,
                privacyScore = scoreState,
                findings = findingsState,
                isUsbConnected = isUsb,
                onRunFullAudit = { runDeviceAudit() },
                onPickMediaToScan = { pickPhotoLauncher.launch("image/*") },
                onPickFileToScan = { pickDocLauncher.launch("*/*") },
                onExportReport = { exportReport() },
                onClearResults = {
                    findingsState.clear()
                    scoreState = 100
                    Toast.makeText(this@MainActivity, "Results cleared!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun runDeviceAudit() {
        lifecycleScope.launch {
            findingsState.clear()
            currentScanId = UUID.randomUUID().toString()

            // 1. Audit Installed App Permissions
            val permFindings = PermissionAudit.auditInstalledApps(this@MainActivity, currentScanId)
            findingsState.addAll(permFindings)

            // 2. Audit Device Location Settings
            val locFindings = LocationAudit.auditDeviceLocationSettings(this@MainActivity, currentScanId)
            findingsState.addAll(locFindings)

            // 3. Audit Local Network
            val netFindings = NetworkAudit.auditNetworkState(this@MainActivity, currentScanId)
            findingsState.addAll(netFindings)

            scoreState = repository.calculatePrivacyScore(findingsState)

            val meta = metadataState ?: DeviceInfo.getDeviceMetadata(this@MainActivity)
            val scanRecord = DeviceScan(
                id = currentScanId,
                timestamp = System.currentTimeMillis().toString(),
                deviceModel = meta.model,
                manufacturer = meta.manufacturer,
                androidVersion = meta.androidVersion,
                sdkVersion = meta.sdkVersion,
                privacyScore = scoreState,
                totalFindings = findingsState.size,
                criticalCount = findingsState.count { it.severity == "CRITICAL" },
                highCount = findingsState.count { it.severity == "HIGH" },
                mediumCount = findingsState.count { it.severity == "MEDIUM" },
                lowCount = findingsState.count { it.severity == "LOW" }
            )

            repository.saveScan(scanRecord, findingsState.toList())
            Toast.makeText(this@MainActivity, "Device audit complete! Findings: ${findingsState.size}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportReport() {
        val meta = metadataState ?: DeviceInfo.getDeviceMetadata(this)
        val scanRecord = DeviceScan(
            id = currentScanId,
            timestamp = System.currentTimeMillis().toString(),
            deviceModel = meta.model,
            manufacturer = meta.manufacturer,
            androidVersion = meta.androidVersion,
            sdkVersion = meta.sdkVersion,
            privacyScore = scoreState,
            totalFindings = findingsState.size,
            criticalCount = findingsState.count { it.severity == "CRITICAL" },
            highCount = findingsState.count { it.severity == "HIGH" },
            mediumCount = findingsState.count { it.severity == "MEDIUM" },
            lowCount = findingsState.count { it.severity == "LOW" }
        )

        val json = ReportExporter.exportScanToJson(scanRecord, findingsState.toList())
        val file = ReportExporter.saveReportFile(this, json, currentScanId)
        Toast.makeText(this, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
    }
}
