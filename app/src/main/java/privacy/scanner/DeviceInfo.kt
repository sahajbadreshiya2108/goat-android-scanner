package privacy.scanner

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class DeviceMetadata(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val securityPatch: String,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long
)

object DeviceInfo {
    fun getDeviceMetadata(context: Context): DeviceMetadata {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val availableBytes = stat.blockSizeLong * stat.availableBlocksLong

        return DeviceMetadata(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            totalStorageBytes = totalBytes,
            availableStorageBytes = availableBytes
        )
    }
}
