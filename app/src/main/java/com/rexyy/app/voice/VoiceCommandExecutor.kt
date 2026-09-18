package com.rexyy.app.voice

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import java.net.URLEncoder

object VoiceCommandExecutor {

    fun execute(command: VoiceCommand, context: Context): VoiceCommandResult {
        return try {
            when (command) {
                is VoiceCommand.OpenApp -> executeOpenApp(command.appName, context)
                is VoiceCommand.GoogleSearch -> executeGoogleSearch(command.query, context)
                is VoiceCommand.SetAlarm -> executeSetAlarm(command.hour, command.minute, command.message, context)
                is VoiceCommand.OpenSettings -> executeOpenSettings(context)
                is VoiceCommand.BluetoothSettings -> executeBluetoothSettings(context)
                is VoiceCommand.AdjustVolume -> executeAdjustVolume(command.raise, context)
                is VoiceCommand.CallContact -> executeCallContact(command.target, context)
                is VoiceCommand.SendMessage -> executeSendMessage(command.target, command.body, context)
                is VoiceCommand.SetReminder -> executeSetReminder(command.title, context)
                is VoiceCommand.AiChat -> VoiceCommandResult.ForwardToAi(command.prompt, command.providerOverride)
            }
        } catch (e: Exception) {
            VoiceCommandResult.Error("Unable to perform command: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun executeOpenApp(appName: String, context: Context): VoiceCommandResult {
        val lower = appName.lowercase().trim()
        val packageManager = context.packageManager

        // 1. YouTube handling
        if (lower.contains("youtube")) {
            val ytIntent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (ytIntent != null) {
                ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytIntent)
                return VoiceCommandResult.Handled("Opening YouTube...")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return VoiceCommandResult.Handled("Opening YouTube in browser...")
        }

        // 2. WhatsApp
        if (lower.contains("whatsapp")) {
            val waIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (waIntent != null) {
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(waIntent)
                return VoiceCommandResult.Handled("Opening WhatsApp...")
            }
        }

        // 3. Chrome / Browser
        if (lower.contains("chrome") || lower.contains("browser")) {
            val chromeIntent = packageManager.getLaunchIntentForPackage("com.android.chrome")
            if (chromeIntent != null) {
                chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chromeIntent)
                return VoiceCommandResult.Handled("Opening Chrome...")
            }
        }

        // 4. Search installed applications by label
        try {
            val installedApps = packageManager.getInstalledApplications(0)
            for (appInfo in installedApps) {
                val label = packageManager.getApplicationLabel(appInfo).toString()
                if (label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return VoiceCommandResult.Handled("Opening $label...")
                    }
                }
            }
        } catch (_: Exception) {
            // Continue to fallback
        }

        // 5. Fallback: Search on Play Store / Web
        val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + URLEncoder.encode(appName, "UTF-8"))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(searchIntent)
            VoiceCommandResult.Handled("Searching for $appName...")
        } catch (_: Exception) {
            val webSearch = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(appName, "UTF-8"))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webSearch)
            VoiceCommandResult.Handled("Searching for $appName...")
        }
    }

    private fun executeGoogleSearch(query: String, context: Context): VoiceCommandResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            VoiceCommandResult.Handled("Searching Google for: $query")
        } catch (_: Exception) {
            val webUrl = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            VoiceCommandResult.Handled("Searching Google for: $query")
        }
    }

    private fun executeSetAlarm(hour: Int, minute: Int, message: String, context: Context): VoiceCommandResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val timeFormatted = String.format("%02d:%02d", hour, minute)
            VoiceCommandResult.Handled("Setting alarm for $timeFormatted.")
        } catch (_: Exception) {
            // Open Clock app if direct alarm intent isn't supported
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(clockIntent)
                VoiceCommandResult.Handled("Opening Clock to set alarm.")
            } catch (_: Exception) {
                VoiceCommandResult.Error("Could not open clock or set alarm on this device.")
            }
        }
    }

    private fun executeOpenSettings(context: Context): VoiceCommandResult {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return VoiceCommandResult.Handled("Opening system settings...")
    }

    private fun executeBluetoothSettings(context: Context): VoiceCommandResult {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return VoiceCommandResult.Handled("Opening Bluetooth settings...")
    }

    private fun executeAdjustVolume(raise: Boolean, context: Context): VoiceCommandResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val actionText = if (raise) "Increasing volume" else "Decreasing volume"
            return VoiceCommandResult.Handled(actionText)
        }
        return VoiceCommandResult.Error("Audio service unavailable")
    }

    private fun executeCallContact(target: String, context: Context): VoiceCommandResult {
        // Safe dialer invocation - requires user confirmation to initiate actual call
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(target)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return VoiceCommandResult.Handled("Opening dialer for $target.")
    }

    private fun executeSendMessage(target: String, body: String, context: Context): VoiceCommandResult {
        // Safe messaging invocation - requires user review in messaging app
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(target)}")
            if (body.isNotBlank()) {
                putExtra("sms_body", body)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            VoiceCommandResult.Handled("Opening messages for $target.")
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send message").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            VoiceCommandResult.Handled("Opening messaging apps...")
        }
    }

    private fun executeSetReminder(title: String, context: Context): VoiceCommandResult {
        val eventTitle = title.ifBlank { "REXYY Reminder" }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, eventTitle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            VoiceCommandResult.Handled("Opening calendar to set reminder: $eventTitle")
        } catch (_: Exception) {
            val fallbackSettings = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackSettings)
            VoiceCommandResult.Handled("Reminder setup opened.")
        }
    }
}
