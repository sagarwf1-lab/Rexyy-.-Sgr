package com.rexyy.app.voice

import com.rexyy.app.network.provider.AiProviderType
import java.util.regex.Pattern

object VoiceCommandParser {

    /**
     * Analyzes raw speech or text and determines whether it represents a local device action
     * or should be forwarded to the generative AI conversation pipeline.
     */
    fun parse(input: String): VoiceCommand {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return VoiceCommand.AiChat("")

        val lower = trimmed.lowercase()

        // Explicit AI Provider overrides (e.g. "Ask Gemini what black holes are")
        if (lower.startsWith("ask gemini ") || lower.startsWith("gemini se pucho ")) {
            val query = trimmed.replace("(?i)^ask gemini\\s+(to\\s+)?".toRegex(), "")
                .replace("(?i)^gemini se pucho\\s+".toRegex(), "")
                .trim()
            return VoiceCommand.AiChat(prompt = query.ifBlank { trimmed }, providerOverride = AiProviderType.GEMINI)
        }
        if (lower.startsWith("ask openai ") || lower.startsWith("ask chatgpt ") || lower.startsWith("openai se pucho ")) {
            val query = trimmed.replace("(?i)^ask (openai|chatgpt)\\s+(to\\s+)?".toRegex(), "")
                .replace("(?i)^openai se pucho\\s+".toRegex(), "")
                .trim()
            return VoiceCommand.AiChat(prompt = query.ifBlank { trimmed }, providerOverride = AiProviderType.OPENAI)
        }

        // 1. Bluetooth settings
        if (isBluetoothCommand(lower)) {
            val isTurnOn = lower.contains("on") || lower.contains("chalu") || lower.contains("kholo")
            return VoiceCommand.BluetoothSettings(turnOn = isTurnOn, rawInput = trimmed)
        }

        // 2. Open Settings
        if (isSettingsCommand(lower)) {
            return VoiceCommand.OpenSettings(rawInput = trimmed)
        }

        // 3. Volume Adjustments
        val volumeDirection = checkVolumeCommand(lower)
        if (volumeDirection != null) {
            return VoiceCommand.AdjustVolume(raise = volumeDirection, rawInput = trimmed)
        }

        // 4. Set Alarm
        val alarmCommand = parseAlarmCommand(trimmed, lower)
        if (alarmCommand != null) {
            return alarmCommand
        }

        // 5. Open YouTube / Common Apps
        val openAppCommand = parseOpenAppCommand(trimmed, lower)
        if (openAppCommand != null) {
            return openAppCommand
        }

        // 6. Google Search
        val searchCommand = parseSearchCommand(trimmed, lower)
        if (searchCommand != null) {
            return searchCommand
        }

        // 7. Call Contact / Phone
        val callCommand = parseCallCommand(trimmed, lower)
        if (callCommand != null) {
            return callCommand
        }

        // 8. Send Message / SMS
        val messageCommand = parseMessageCommand(trimmed, lower)
        if (messageCommand != null) {
            return messageCommand
        }

        // 9. Reminder
        if (isReminderCommand(lower)) {
            val title = extractReminderTitle(trimmed, lower)
            return VoiceCommand.SetReminder(title = title, rawInput = trimmed)
        }

        // Default: Forward to conversational AI model (e.g., weather, jokes, explanation, translation)
        return VoiceCommand.AiChat(prompt = trimmed)
    }

    private fun isBluetoothCommand(lower: String): Boolean {
        return lower.contains("bluetooth") && (
                lower.contains("on") || lower.contains("off") ||
                        lower.contains("turn") || lower.contains("kholo") ||
                        lower.contains("karo") || lower.contains("band") ||
                        lower.contains("chalu") || lower.contains("open")
                )
    }

    private fun isSettingsCommand(lower: String): Boolean {
        return (lower.startsWith("open settings") ||
                lower.startsWith("open setting") ||
                lower.contains("settings kholo") ||
                lower.contains("setting kholo") ||
                lower.contains("settings open karo") ||
                lower.contains("setting open karo") ||
                lower == "settings" || lower == "setting")
    }

    private fun checkVolumeCommand(lower: String): Boolean? {
        if (!lower.contains("volume") && !lower.contains("awaaz") && !lower.contains("awaz")) {
            return null
        }
        val isIncrease = lower.contains("increase") || lower.contains("up") ||
                lower.contains("raise") || lower.contains("badhao") ||
                lower.contains("jyada") || lower.contains("high")
        val isDecrease = lower.contains("decrease") || lower.contains("down") ||
                lower.contains("lower") || lower.contains("kam") ||
                lower.contains("ghatao") || lower.contains("low")

        return when {
            isIncrease -> true
            isDecrease -> false
            else -> null
        }
    }

    private fun parseAlarmCommand(raw: String, lower: String): VoiceCommand.SetAlarm? {
        val isAlarmTrigger = lower.contains("alarm") || lower.contains("wake me up") ||
                lower.contains("baje uthao") || lower.contains("baje jagao")

        if (!isAlarmTrigger) return null

        // Pattern 1: e.g. "7:30 AM", "07:30 pm", "7:30"
        val timeColonPattern = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val colonMatcher = timeColonPattern.matcher(lower)
        if (colonMatcher.find()) {
            var hour = colonMatcher.group(1)?.toIntOrNull() ?: 7
            val min = colonMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = colonMatcher.group(3)?.lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return VoiceCommand.SetAlarm(hour = hour, minute = min, message = "REXYY Alarm", rawInput = raw)
        }

        // Pattern 2: e.g. "7 AM", "7 PM", "7 baje", "7 o'clock"
        val simpleHourPattern = Pattern.compile("(\\d{1,2})\\s*(am|pm|baje|o'clock|hour)", Pattern.CASE_INSENSITIVE)
        val hourMatcher = simpleHourPattern.matcher(lower)
        if (hourMatcher.find()) {
            var hour = hourMatcher.group(1)?.toIntOrNull() ?: 7
            val modifier = hourMatcher.group(2)?.lowercase()
            if (modifier == "pm" && hour < 12) hour += 12
            if (modifier == "am" && hour == 12) hour = 0
            return VoiceCommand.SetAlarm(hour = hour, minute = 0, message = "REXYY Alarm", rawInput = raw)
        }

        // Generic alarm fallback: default 7:00 AM
        return VoiceCommand.SetAlarm(hour = 7, minute = 0, message = "REXYY Alarm", rawInput = raw)
    }

    private fun parseOpenAppCommand(raw: String, lower: String): VoiceCommand.OpenApp? {
        // Specific prominent YouTube matching
        if (lower.contains("youtube") && (
                    lower.contains("open") || lower.contains("kholo") ||
                            lower.contains("start") || lower.contains("launch") ||
                            lower.contains("chalao")
                    )) {
            return VoiceCommand.OpenApp(appName = "YouTube", rawInput = raw)
        }

        // General "open <app>" or "<app> kholo"
        val openPrefix = "open "
        if (lower.startsWith(openPrefix)) {
            val appCandidate = raw.substring(openPrefix.length).trim()
            if (appCandidate.isNotBlank() && !appCandidate.equals("settings", ignoreCase = true) && !appCandidate.equals("bluetooth", ignoreCase = true)) {
                return VoiceCommand.OpenApp(appName = appCandidate, rawInput = raw)
            }
        }

        val launchPrefix = "launch "
        if (lower.startsWith(launchPrefix)) {
            val appCandidate = raw.substring(launchPrefix.length).trim()
            if (appCandidate.isNotBlank()) {
                return VoiceCommand.OpenApp(appName = appCandidate, rawInput = raw)
            }
        }

        if (lower.endsWith(" kholo") || lower.endsWith(" open karo") || lower.endsWith(" chalao")) {
            val cleaned = lower
                .replace(" kholo", "")
                .replace(" open karo", "")
                .replace(" chalao", "")
                .trim()
            if (cleaned.isNotBlank() && cleaned != "settings" && cleaned != "bluetooth") {
                return VoiceCommand.OpenApp(appName = cleaned, rawInput = raw)
            }
        }

        return null
    }

    private fun parseSearchCommand(raw: String, lower: String): VoiceCommand.GoogleSearch? {
        // "search google for <query>"
        val searchGoogleFor = "search google for "
        if (lower.startsWith(searchGoogleFor)) {
            val q = raw.substring(searchGoogleFor.length).trim()
            return VoiceCommand.GoogleSearch(query = q, rawInput = raw)
        }

        // "search on google for <query>"
        val searchOnGoogle = "search on google for "
        if (lower.startsWith(searchOnGoogle)) {
            val q = raw.substring(searchOnGoogle.length).trim()
            return VoiceCommand.GoogleSearch(query = q, rawInput = raw)
        }

        // "google par <query> search karo"
        if (lower.contains("google par") && (lower.contains("search karo") || lower.contains("khojo"))) {
            val q = raw.replace("(?i)google par".toRegex(), "")
                .replace("(?i)search karo".toRegex(), "")
                .replace("(?i)khojo".toRegex(), "")
                .trim()
            return VoiceCommand.GoogleSearch(query = q.ifBlank { raw }, rawInput = raw)
        }

        // "google <query>" (only if more than 1 word)
        if (lower.startsWith("google ") && lower.length > 7 && !lower.contains("kholo")) {
            val q = raw.substring(7).trim()
            return VoiceCommand.GoogleSearch(query = q, rawInput = raw)
        }

        // "<query> google karo"
        if (lower.endsWith(" google karo") || lower.endsWith(" search karo")) {
            val q = raw.replace("(?i) google karo".toRegex(), "")
                .replace("(?i) search karo".toRegex(), "")
                .trim()
            if (q.isNotBlank()) {
                return VoiceCommand.GoogleSearch(query = q, rawInput = raw)
            }
        }

        return null
    }

    private fun parseCallCommand(raw: String, lower: String): VoiceCommand.CallContact? {
        // "call <name>"
        if (lower.startsWith("call ") && lower.length > 5) {
            val target = raw.substring(5).trim()
            return VoiceCommand.CallContact(target = target, rawInput = raw)
        }

        // "<name> ko call karo" / "<name> ko phone lagao"
        if (lower.contains("ko call karo") || lower.contains("ko phone lagao") || lower.contains("ko call lagao")) {
            val target = raw.replace("(?i)ko call karo".toRegex(), "")
                .replace("(?i)ko phone lagao".toRegex(), "")
                .replace("(?i)ko call lagao".toRegex(), "")
                .trim()
            if (target.isNotBlank()) {
                return VoiceCommand.CallContact(target = target, rawInput = raw)
            }
        }

        return null
    }

    private fun parseMessageCommand(raw: String, lower: String): VoiceCommand.SendMessage? {
        // "send a message to <name>" / "send message to <name>"
        val msgPrefixes = listOf("send a message to ", "send message to ", "text ", "message ")
        for (prefix in msgPrefixes) {
            if (lower.startsWith(prefix)) {
                val remainder = raw.substring(prefix.length).trim()
                return VoiceCommand.SendMessage(target = remainder, rawInput = raw)
            }
        }

        // "<name> ko message bhejo"
        if (lower.contains("ko message bhejo") || lower.contains("ko sms bhejo")) {
            val target = raw.replace("(?i)ko message bhejo".toRegex(), "")
                .replace("(?i)ko sms bhejo".toRegex(), "")
                .trim()
            if (target.isNotBlank()) {
                return VoiceCommand.SendMessage(target = target, rawInput = raw)
            }
        }

        return null
    }

    private fun isReminderCommand(lower: String): Boolean {
        return lower.contains("reminder") || lower.contains("yaad dilao") || lower.contains("remind me")
    }

    private fun extractReminderTitle(raw: String, lower: String): String {
        return raw.replace("(?i)set a reminder for ".toRegex(), "")
            .replace("(?i)set reminder for ".toRegex(), "")
            .replace("(?i)set a reminder".toRegex(), "")
            .replace("(?i)set reminder".toRegex(), "")
            .replace("(?i)remind me to ".toRegex(), "")
            .replace("(?i)reminder lagao".toRegex(), "")
            .replace("(?i)yaad dilao".toRegex(), "")
            .trim()
    }
}
