package privacy.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.util.UUID

object NetworkAudit {

    fun auditNetworkState(context: Context, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)

        if (capabilities != null) {
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            if (!isVpn && isWifi) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "NETWORK",
                        findingType = "WiFi Active Without Active VPN",
                        severity = "LOW",
                        confidence = 0.90f,
                        sourceItem = "Android:NetworkCapabilities",
                        location = "Network:WiFi",
                        rawValue = "WiFi Connected, VPN Inactive",
                        maskedValue = "WiFi Connected, VPN Inactive",
                        explanation = "Device is connected to WiFi without an encrypted VPN tunnel.",
                        risk = "Local network eavesdropping on unencrypted protocol traffic.",
                        recommendedAction = "Use a trusted VPN when connecting to unfamiliar or public wireless networks.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            }
        }

        return findings
    }
}
