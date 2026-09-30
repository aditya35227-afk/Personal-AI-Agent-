package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.local.dao.PermissionLogDao
import com.example.data.local.entity.PermissionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

enum class AppPermission(
    val key: String,
    val title: String,
    val androidPermission: String?,
    val usageDescription: String,
    val isSensitive: Boolean = true
) {
    MICROPHONE(
        key = "microphone",
        title = "Microphone",
        androidPermission = Manifest.permission.RECORD_AUDIO,
        usageDescription = "Used for voice commands and hands-free doubt solving. Never records in background."
    ),
    CAMERA(
        key = "camera",
        title = "Camera",
        androidPermission = Manifest.permission.CAMERA,
        usageDescription = "Used only when you scan homework questions or take photos of study notes."
    ),
    CONTACTS(
        key = "contacts",
        title = "Contacts",
        androidPermission = Manifest.permission.READ_CONTACTS,
        usageDescription = "Used solely to lookup a name when you command to call or text someone. Never uploaded."
    ),
    PHONE(
        key = "phone",
        title = "Phone / Calls",
        androidPermission = Manifest.permission.CALL_PHONE,
        usageDescription = "Used strictly when you explicitly ask to initiate a phone call."
    ),
    SMS(
        key = "sms",
        title = "SMS",
        androidPermission = Manifest.permission.SEND_SMS,
        usageDescription = "Used only after explicit confirmation when you ask to send a text message."
    ),
    NOTIFICATIONS(
        key = "notifications",
        title = "Notifications",
        androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null,
        usageDescription = "Used to alert you when your study timer, break timer, or quiz reminder finishes."
    ),
    CALENDAR(
        key = "calendar",
        title = "Calendar",
        androidPermission = Manifest.permission.READ_CALENDAR,
        usageDescription = "Used to view exam dates and study schedules when requested."
    ),
    LOCATION(
        key = "location",
        title = "Location",
        androidPermission = Manifest.permission.ACCESS_COARSE_LOCATION,
        usageDescription = "Optional. Used for localized weather or campus queries if explicitly asked."
    ),
    PHOTOS_VIDEOS(
        key = "photos_picker",
        title = "Photos / Videos",
        androidPermission = null, // System Photo Picker requires 0 permissions!
        usageDescription = "Uses Android Photo Picker (Zero-permission). The app never scans your private gallery."
    ),
    FILES(
        key = "files_picker",
        title = "Documents & Files",
        androidPermission = null, // System Storage Access Framework requires 0 permissions!
        usageDescription = "Uses Android System Document Picker. Only files you explicitly select are read."
    )
}

interface PermissionManager {
    fun isGranted(permission: AppPermission): Boolean
    fun logEvent(permission: AppPermission, action: String, details: String)
    fun getLogs(): Flow<List<PermissionLogEntity>>
    fun openAppSettings(context: Context)
}

class AndroidPermissionManager(
    private val context: Context,
    private val permissionLogDao: PermissionLogDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PermissionManager {

    override fun isGranted(permission: AppPermission): Boolean {
        if (permission.androidPermission == null) {
            // Photo picker / SAF have built-in zero-permission security
            return true
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission.androidPermission
        ) == PackageManager.PERMISSION_GRANTED

        logEvent(
            permission,
            "CHECKED",
            if (granted) "Permission active" else "Permission not granted"
        )
        return granted
    }

    override fun logEvent(permission: AppPermission, action: String, details: String) {
        scope.launch {
            try {
                permissionLogDao.insertLog(
                    PermissionLogEntity(
                        permissionKey = permission.key,
                        actionType = action,
                        details = details,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    override fun getLogs(): Flow<List<PermissionLogEntity>> {
        return permissionLogDao.getLogs()
    }

    override fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
