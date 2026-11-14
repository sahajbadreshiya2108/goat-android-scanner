package privacy.scanner

import android.content.Context
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter

object UsbConnection {

    fun isUsbConnected(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    fun getUsbStatusDescription(context: Context): String {
        return if (isUsbConnected(context)) {
            "USB Cable Connected. Ready for Windows Desktop synchronization via ADB."
        } else {
            "USB Cable Disconnected. Connect device to PC via USB and enable USB Debugging in Developer Options to sync."
        }
    }
}
