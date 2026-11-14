# Android Mobile Privacy Scanner Companion

**Part of the Privacy Leak Scanner Suite**  
*Scan Before You Share. Know Before You Publish.*

The Android Companion application audits Android smartphone devices for location leakage, over-privileged application permissions, media EXIF GPS records, and sensitive unencrypted files, integrating directly with the Windows desktop Privacy Leak Scanner via USB/ADB or versioned JSON report exports.

---

## 1. Android Architecture & Technology Stack

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose with Material 3 Dark Palette
- **Database:** AndroidX Room Database (`ScanDatabase.kt`, `ScanDao.kt`)
- **Storage & Media:** Android Storage Access Framework (SAF) & `androidx.exifinterface`
- **Minimum SDK:** Android 8.0 (API 26)  
- **Target SDK:** Android 15 (API 35)

---

## 2. Directory Structure

```
android-companion/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/privacy/scanner/
│   │       ├── MainActivity.kt          # Main Compose host & lifecycle
│   │       ├── Dashboard.kt             # Jetpack Compose mobile dashboard
│   │       ├── DeviceInfo.kt            # Model, manufacturer, storage audit
│   │       ├── PermissionAudit.kt       # Background location, camera, mic audit
│   │       ├── LocationAudit.kt         # GPS provider and location services status
│   │       ├── PhotoScanner.kt          # EXIF latitude, longitude, altitude parser
│   │       ├── VideoScanner.kt          # QuickTime/MP4 location metadata extractor
│   │       ├── DocumentScanner.kt       # Text & document PII & secret detector
│   │       ├── ArchiveScanner.kt        # Safe zip inspection (.env, id_rsa, keys)
│   │       ├── BackupImporter.kt        # GPX, KML, GeoJSON location track parser
│   │       ├── NetworkAudit.kt          # WiFi encryption & VPN status check
│   │       ├── ReportExporter.kt        # Versioned JSON export engine
│   │       ├── UsbConnection.kt         # USB connection state detector
│   │       ├── ScanDatabase.kt          # Room Entities & DAOs
│   │       └── ScanRepository.kt        # Scoring & persistence repository
│   ├── src/test/java/privacy/scanner/
│   │   └── ReportExporterTest.kt        # Unit tests for JSON export format
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 3. Privacy & Security Guarantees
1. **No Silent Location Tracking:** The application never queries or logs background GPS coordinates without user initiation.
2. **Offline Local Processing:** Device and media audits run 100% locally on the device; no telemetry or automated uploads.
3. **No Permission Exploitation:** All inspections use official Android APIs (`PackageManager`, `LocationManager`, `ExifInterface`).
4. **Data Redaction:** Sensitive findings and coordinates are masked before export.

---

## 4. USB / ADB Setup Instructions

To sync the Android Companion directly with the Windows desktop application:
1. On your Android device, go to **Settings > About Phone** and tap **Build Number** 7 times to enable Developer Options.
2. Open **Settings > Developer Options** and enable **USB Debugging**.
3. Connect the phone to your Windows PC via USB cable.
4. When prompted on the device, tap **Allow USB Debugging**.
5. In the Windows Privacy Leak Scanner application, open the **Android Mobile** tab and click **Refresh USB Devices**.
6. To revoke USB debugging at any time: Open Android **Settings > Developer Options > Revoke USB debugging authorizations**.
