package privacy.scanner

import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.util.UUID

object LocationAudit {

    fun auditDeviceLocationSettings(context: Context, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return findings

        val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val selfFineLocGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val selfCoarseLocGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isGpsEnabled) {
            findings.add(
                MobileFinding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "LOCATION",
                    findingType = "System GPS Provider Enabled",
                    severity = "INFO",
                    confidence = 1.0f,
                    sourceItem = "Android:LocationManager",
                    location = "Settings:Location",
                    rawValue = "GPS Provider Active",
                    maskedValue = "GPS Provider Active",
                    explanation = "Hardware GPS satellite location service is switched on.",
                    risk = "Allows authorized applications to query high-precision satellite coordinates.",
                    recommendedAction = "Disable device location when travelling in sensitive areas if high privacy is needed.",
                    timestamp = System.currentTimeMillis().toString()
                )
            )
        }

        return findings
    }
}
