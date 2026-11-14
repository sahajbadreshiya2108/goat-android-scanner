package privacy.scanner

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID

data class AppPermissionSummary(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val hasFineLocation: Boolean,
    val hasBackgroundLocation: Boolean,
    val hasCamera: Boolean,
    val hasMicrophone: Boolean,
    val hasContacts: Boolean
)

object PermissionAudit {

    fun auditInstalledApps(context: Context, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()
        val pm = context.packageManager

        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            emptyList()
        }

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) continue // Focus primarily on user-installed apps

            val requestedPermissions = pkg.requestedPermissions ?: continue
            val appLabel = pm.getApplicationLabel(appInfo).toString()

            val hasFineLocation = requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
            val hasBgLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestedPermissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else false
            val hasCamera = requestedPermissions.contains(Manifest.permission.CAMERA)
            val hasMicrophone = requestedPermissions.contains(Manifest.permission.RECORD_AUDIO)

            if (hasBgLocation) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "PERMISSION",
                        findingType = "App Declared Background Location",
                        severity = "HIGH",
                        confidence = 0.95f,
                        sourceItem = pkg.packageName,
                        location = "AppManifest:$appLabel",
                        rawValue = "${pkg.packageName} -> ACCESS_BACKGROUND_LOCATION",
                        maskedValue = "$appLabel (${pkg.packageName})",
                        explanation = "Application '$appLabel' declares background location access capability.",
                        risk = "Background location enables continual movement tracking even when app is closed.",
                        recommendedAction = "Inspect App Permissions in Android Settings and set location to 'Only while using app'.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            } else if (hasFineLocation) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "PERMISSION",
                        findingType = "App Declared Precise Location",
                        severity = "MEDIUM",
                        confidence = 0.90f,
                        sourceItem = pkg.packageName,
                        location = "AppManifest:$appLabel",
                        rawValue = "${pkg.packageName} -> ACCESS_FINE_LOCATION",
                        maskedValue = "$appLabel (${pkg.packageName})",
                        explanation = "Application '$appLabel' requests precise GPS location permission.",
                        risk = "Exposes exact geographical coordinates when active.",
                        recommendedAction = "Review whether this app requires precise vs approximate location.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            }

            if (hasCamera && hasMicrophone) {
                findings.add(
                    MobileFinding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "PERMISSION",
                        findingType = "Combined Camera & Audio Permissions",
                        severity = "LOW",
                        confidence = 0.85f,
                        sourceItem = pkg.packageName,
                        location = "AppManifest:$appLabel",
                        rawValue = "${pkg.packageName} -> CAMERA + RECORD_AUDIO",
                        maskedValue = "$appLabel (${pkg.packageName})",
                        explanation = "Application '$appLabel' requests both Camera and Microphone permissions.",
                        risk = "Audio and video capture capabilities.",
                        recommendedAction = "Verify if media recording is necessary for the app's functionality.",
                        timestamp = System.currentTimeMillis().toString()
                    )
                )
            }
        }

        return findings
    }
}
