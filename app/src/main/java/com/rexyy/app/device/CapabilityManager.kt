package com.rexyy.app.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

object CapabilityManager {

    fun isRootAvailable(): Boolean {
        return try {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            paths.any { File(it).exists() }
        } catch (_: Exception) {
            false
        }
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getCapabilities(context: Context): List<DeviceCapability> {
        val hasMic = hasPermission(context, Manifest.permission.RECORD_AUDIO)
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val isRooted = isRootAvailable()
        val isA11y = isAccessibilityEnabled(context)

        return listOf(
            DeviceCapability(
                id = "mic",
                name = "Microphone & Speech",
                description = "Speech recognition and voice-driven assistant commands",
                status = if (hasMic) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
                requiresPermission = true,
                requiredPermissionName = Manifest.permission.RECORD_AUDIO
            ),
            DeviceCapability(
                id = "notif",
                name = "Notifications",
                description = "Status updates, task completion alerts, and reminders",
                status = if (hasNotif) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
                requiresPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                requiredPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            ),
            DeviceCapability(
                id = "app_launch",
                name = "App Navigation",
                description = "Launch installed apps, YouTube, WhatsApp, and browser safely via Android Intent",
                status = CapabilityStatus.AVAILABLE
            ),
            DeviceCapability(
                id = "settings_control",
                name = "System Settings & Alarms",
                description = "Open device settings, Bluetooth panel, Wi-Fi, and set alarms",
                status = CapabilityStatus.AVAILABLE
            ),
            DeviceCapability(
                id = "audio_volume",
                name = "Volume Management",
                description = "Adjust media and voice volume via system AudioManager",
                status = CapabilityStatus.AVAILABLE
            ),
            DeviceCapability(
                id = "phone_call",
                name = "Phone & Dialing",
                description = "Prepare phone dialer for specified contact or number",
                status = CapabilityStatus.AVAILABLE,
                requiresConfirmation = true
            ),
            DeviceCapability(
                id = "accessibility_nav",
                name = "Accessibility Service",
                description = "Assisted screen reading and automated UI navigation (requires user activation)",
                status = if (isA11y) CapabilityStatus.AVAILABLE else CapabilityStatus.ACCESSIBILITY_REQUIRED,
                requiresAccessibility = true
            ),
            DeviceCapability(
                id = "root_sys",
                name = "Root Capabilities",
                description = "Advanced system-level modifications (safely restricted; only active if device is rooted)",
                status = if (isRooted) CapabilityStatus.AVAILABLE else CapabilityStatus.ROOT_REQUIRED,
                requiresRoot = true,
                requiresConfirmation = true
            )
        )
    }
}
