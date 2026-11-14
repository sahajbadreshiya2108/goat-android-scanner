package privacy.scanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class WebsiteFinding(
    val findingType: String,
    val severity: String,
    val rawValue: String,
    val maskedValue: String,
    val explanation: String,
    val recommendedAction: String
)

class MainActivity : ComponentActivity() {

    private lateinit var repository: ScanRepository
    private val phoneFindingsState = mutableStateListOf<MobileFinding>()
    private var phoneScoreState by mutableStateOf(100)
    private var metadataState by mutableStateOf<DeviceMetadata?>(null)
    private var currentScanId = UUID.randomUUID().toString()

    // Website Audit State
    private var webUrlInput by mutableStateOf("")
    private var webScoreState by mutableStateOf<Int?>(null)
    private var isWebScanning by mutableStateOf(false)
    private val webFindingsState = mutableStateListOf<WebsiteFinding>()
    private val revealedEyeMap = mutableStateMapOf<Int, Boolean>()

    // Current Tab: 0 = Phone Audit, 1 = Website Audit, 2 = Web Portal
    private var selectedTabIndex by mutableStateOf(0)

    // Pickers
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val photoFindings = PhotoScanner.scanPhotoUri(this, it, "Selected_Photo.jpg", currentScanId)
            phoneFindingsState.addAll(photoFindings)
            phoneScoreState = repository.calculatePrivacyScore(phoneFindingsState)
        }
    }

    private val pickDocLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val docFindings = DocumentScanner.scanTextOrDocUri(this, it, "Selected_File", currentScanId)
            val backupFindings = BackupImporter.scanBackupUri(this, it, "Selected_File", currentScanId)
            phoneFindingsState.addAll(docFindings)
            phoneFindingsState.addAll(backupFindings)
            phoneScoreState = repository.calculatePrivacyScore(phoneFindingsState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScanRepository(this)
        metadataState = DeviceInfo.getDeviceMetadata(this)

        // Automatically run initial Phone audit on open so user sees device details
        runPhoneAudit()

        setContent {
            val meta = metadataState ?: DeviceInfo.getDeviceMetadata(this)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp)
            ) {
                // Header with Goat Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Goat Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Goat Privacy Scanner",
                            color = Color(0xFF38BDF8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Scan Before You Share • Know Before You Publish",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Tabs: Phone Audit | Website Audit
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(bottom = 14.dp)
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Phone Audit", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Website Audit", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                when (selectedTabIndex) {
                    0 -> PhoneAuditView(
                        meta = meta,
                        score = phoneScoreState,
                        findings = phoneFindingsState,
                        onScanMedia = { pickPhotoLauncher.launch("image/*") },
                        onScanDocs = { pickDocLauncher.launch("*/*") },
                        onClearPhone = {
                            phoneFindingsState.clear()
                            phoneScoreState = 100
                        },
                        onRerunAudit = { runPhoneAudit() }
                    )
                    1 -> WebsiteAuditView(
                        url = webUrlInput,
                        onUrlChange = { webUrlInput = it },
                        isScanning = isWebScanning,
                        score = webScoreState,
                        findings = webFindingsState,
                        revealedMap = revealedEyeMap,
                        onAuditClick = { triggerWebsiteAudit(webUrlInput) },
                        onClearWeb = {
                            webUrlInput = ""
                            webScoreState = null
                            webFindingsState.clear()
                            revealedEyeMap.clear()
                        },
                        onToggleEye = { index ->
                            revealedEyeMap[index] = !(revealedEyeMap[index] ?: false)
                        }
                    )
                }
            }
        }
    }

    private fun runPhoneAudit() {
        lifecycleScope.launch {
            phoneFindingsState.clear()
            currentScanId = UUID.randomUUID().toString()

            val permFindings = PermissionAudit.auditInstalledApps(this@MainActivity, currentScanId)
            phoneFindingsState.addAll(permFindings)

            val locFindings = LocationAudit.auditDeviceLocationSettings(this@MainActivity, currentScanId)
            phoneFindingsState.addAll(locFindings)

            val netFindings = NetworkAudit.auditNetworkState(this@MainActivity, currentScanId)
            phoneFindingsState.addAll(netFindings)

            phoneScoreState = repository.calculatePrivacyScore(phoneFindingsState)
        }
    }

    private fun triggerWebsiteAudit(targetUrl: String) {
        if (targetUrl.isBlank()) return
        isWebScanning = true
        webFindingsState.clear()
        revealedEyeMap.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Call local or live scanner endpoint
                val cleanUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                    "https://$targetUrl"
                } else targetUrl

                // We try live tunnel or fallback to direct audit
                val endPoint = "https://fruity-brooms-march.loca.lt/api/scan/website"
                val connection = URL(endPoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 8000
                connection.readTimeout = 12000
                connection.doOutput = true

                val payload = JSONObject().apply { put("url", cleanUrl) }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

                if (connection.responseCode == 200) {
                    val respText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(respText)
                    val score = json.optInt("privacy_score", 85)
                    val findingsArr = json.optJSONArray("findings") ?: JSONArray()

                    val list = mutableListOf<WebsiteFinding>()
                    for (i in 0 until findingsArr.length()) {
                        val f = findingsArr.getJSONObject(i)
                        list.add(
                            WebsiteFinding(
                                findingType = f.optString("finding_type", "Finding"),
                                severity = f.optString("severity", "LOW"),
                                rawValue = f.optString("raw_value", f.optString("masked_value")),
                                maskedValue = f.optString("masked_value", ""),
                                explanation = f.optString("explanation", ""),
                                recommendedAction = f.optString("recommended_action", "")
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        webScoreState = score
                        webFindingsState.addAll(list)
                        isWebScanning = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isWebScanning = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isWebScanning = false
                }
            }
        }
    }
}

@Composable
fun PhoneAuditView(
    meta: DeviceMetadata,
    score: Int,
    findings: List<MobileFinding>,
    onScanMedia: () -> Unit,
    onScanDocs: () -> Unit,
    onClearPhone: () -> Unit,
    onRerunAudit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${meta.manufacturer} ${meta.model} (Android ${meta.androidVersion})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Storage: ${(meta.availableStorageBytes / (1024*1024*1024))} GB Free of ${(meta.totalStorageBytes / (1024*1024*1024))} GB",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }

    // Score & Actions Row
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Device Privacy Score", color = Color(0xFF94A3B8), fontSize = 12.sp)
                val scoreColor = if (score >= 80) Color(0xFF10B981) else if (score >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                Text("$score / 100", color = scoreColor, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = onRerunAudit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Re-Audit", fontSize = 12.sp)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onScanMedia,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
        ) {
            Text("Scan Media", fontSize = 12.sp)
        }
        Button(
            onClick = onScanDocs,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
        ) {
            Text("Scan Docs", fontSize = 12.sp)
        }
    }

    // Title with Clear All
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Phone Findings (${findings.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (findings.isNotEmpty()) {
            Button(
                onClick = onClearPhone,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(findings) { finding ->
            FindingCard(finding)
        }
    }
}

@Composable
fun WebsiteAuditView(
    url: String,
    onUrlChange: (String) -> Unit,
    isScanning: Boolean,
    score: Int?,
    findings: List<WebsiteFinding>,
    revealedMap: Map<Int, Boolean>,
    onAuditClick: () -> Unit,
    onClearWeb: () -> Unit,
    onToggleEye: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Public Domain / Website URL", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("https://example.com", color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0B1120),
                    unfocusedContainerColor = Color(0xFF0B1120),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAuditClick,
                    enabled = !isScanning && url.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text(if (isScanning) "Auditing..." else "Audit Website Privacy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onClearWeb,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
    }

    if (score != null) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Privacy Exposure Score", color = Color(0xFF94A3B8), fontSize = 13.sp)
                val scoreColor = if (score >= 80) Color(0xFF10B981) else if (score >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                Text("$score / 100", color = scoreColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    // Findings List Header
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Website Findings (${findings.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (findings.isNotEmpty()) {
            Button(
                onClick = onClearWeb,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Clear Results", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(findings.size) { i ->
            val item = findings[i]
            val isRev = revealedMap[i] ?: false
            val displayVal = if (isRev) item.rawValue else item.maskedValue

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val badgeColor = when (item.severity) {
                        "CRITICAL" -> Color(0xFFEF4444)
                        "HIGH" -> Color(0xFFEA580C)
                        "MEDIUM" -> Color(0xFFF59E0B)
                        else -> Color(0xFF3B82F6)
                    }
                    Text(
                        text = item.severity,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.findingType, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("PROTECTED VALUE:", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = displayVal,
                            color = if (isRev) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isRev) "🔒" else "👁️",
                            modifier = Modifier
                                .clickable { onToggleEye(i) }
                                .padding(horizontal = 4.dp),
                            fontSize = 14.sp
                        )
                    }

                    if (item.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.explanation, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
