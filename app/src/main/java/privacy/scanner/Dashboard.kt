package privacy.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    deviceMetadata: DeviceMetadata,
    privacyScore: Int,
    findings: List<MobileFinding>,
    isUsbConnected: Boolean,
    onRunFullAudit: () -> Unit,
    onPickMediaToScan: () -> Unit,
    onPickFileToScan: () -> Unit,
    onExportReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // App Header
        Text(
            text = "PRIVACY SCANNER",
            color = Color(0xFF38BDF8),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Scan Before You Share. Know Before You Publish.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Device Info Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${deviceMetadata.manufacturer} ${deviceMetadata.model} (Android ${deviceMetadata.androidVersion})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Storage: ${(deviceMetadata.availableStorageBytes / (1024*1024*1024))} GB Free of ${(deviceMetadata.totalStorageBytes / (1024*1024*1024))} GB",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val usbColor = if (isUsbConnected) Color(0xFF10B981) else Color(0xFF64748B)
                Text(
                    text = if (isUsbConnected) "USB: Connected to Host" else "USB: Disconnected",
                    color = usbColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Privacy Score Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device Privacy Score", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    val scoreColor = if (privacyScore >= 80) Color(0xFF10B981) else if (privacyScore >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    Text("$privacyScore / 100", color = scoreColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onRunFullAudit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Run Audit")
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickMediaToScan,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("Scan Media", fontSize = 12.sp)
            }
            Button(
                onClick = onPickFileToScan,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("Scan Docs", fontSize = 12.sp)
            }
            Button(
                onClick = onExportReport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Export", fontSize = 12.sp)
            }
        }

        // Findings Title
        Text(
            text = "Audit Findings (${findings.size})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Findings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(findings) { finding ->
                FindingCard(finding)
            }
        }
    }
}

@Composable
fun FindingCard(finding: MobileFinding) {
    val sevColor = when (finding.severity) {
        "CRITICAL" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF97316)
        "MEDIUM" -> Color(0xFFF59E0B)
        "LOW" -> Color(0xFF3B82F6)
        else -> Color(0xFF64748B)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = sevColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = finding.severity,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = finding.findingType,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Item: ${finding.sourceItem} (${finding.location})", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(text = "Match: ${finding.maskedValue}", color = Color(0xFF38BDF8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = finding.explanation, color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Action: ${finding.recommendedAction}", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
