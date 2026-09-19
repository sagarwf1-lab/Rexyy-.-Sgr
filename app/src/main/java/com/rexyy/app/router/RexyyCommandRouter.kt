package com.rexyy.app.router

import com.rexyy.app.network.provider.AiProviderType
import com.rexyy.app.task.TaskPlanner
import com.rexyy.app.voice.VoiceCommand
import java.util.regex.Pattern

object RexyyCommandRouter {

    /**
     * Common entry point for BOTH voice and text input.
     * Routes local commands directly to local actions with zero AI latency,
     * or yields an AiChat command when generative AI is needed.
     */
    fun route(input: String): VoiceCommand {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return VoiceCommand.AiChat("")

        val lower = trimmed.lowercase()

        // 1. Explicit AI Provider overrides (e.g., "Ask Gemini what is quantum physics")
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

        // 2. Check for multi-step task conjunctions ("and", "aur", "then", "phir")
        val plannedTask = TaskPlanner.planTask(trimmed) { singleQuery -> routeSingleAction(singleQuery) }
        if (plannedTask != null) {
            return VoiceCommand.MultiStepTask(
                steps = plannedTask.steps.map { it.command },
                description = plannedTask.title,
                rawInput = trimmed
            )
        }

        // 3. Single action routing
        return routeSingleAction(trimmed)
    }

    private fun routeSingleAction(trimmed: String): VoiceCommand {
        val lower = trimmed.lowercase()

        // --- A. Incoming Call & Telephony Control ---
        if (isWhoIsCallingCommand(lower)) {
            return VoiceCommand.CheckCaller(rawInput = trimmed)
        }
        if (isAnswerCallCommand(lower)) {
            return VoiceCommand.AnswerCall(rawInput = trimmed)
        }
        if (isRejectCallCommand(lower)) {
            return VoiceCommand.RejectCall(rawInput = trimmed)
        }

        // --- B. Phone Calls ---
        val callCommand = parseCallCommand(trimmed, lower)
        if (callCommand != null) return callCommand

        // --- C. WhatsApp Actions ---
        val whatsAppCommand = parseWhatsAppCommand(trimmed, lower)
        if (whatsAppCommand != null) return whatsAppCommand

        // --- D. Camera & Media ---
        if (isCameraCommand(lower)) {
            return VoiceCommand.OpenCamera(rawInput = trimmed)
        }

        // --- E. Audio & Volume ---
        val volumeCommand = parseVolumeCommand(trimmed, lower)
        if (volumeCommand != null) return volumeCommand

        // --- F. Brightness ---
        val brightnessCommand = parseBrightnessCommand(trimmed, lower)
        if (brightnessCommand != null) return brightnessCommand

        // --- G. System Settings, Wi-Fi & Bluetooth ---
        if (isBluetoothCommand(lower)) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            return VoiceCommand.BluetoothSettings(turnOn = turnOn, rawInput = trimmed)
        }
        if (isWifiCommand(lower)) {
            return VoiceCommand.WifiSettings(rawInput = trimmed)
        }
        if (isSettingsCommand(lower)) {
            return VoiceCommand.OpenSettings(rawInput = trimmed)
        }

        // --- H. Alarms & Timers ---
        val timerCommand = parseTimerCommand(trimmed, lower)
        if (timerCommand != null) return timerCommand

        val alarmCommand = parseAlarmCommand(trimmed, lower)
        if (alarmCommand != null) return alarmCommand

        // --- I. Calendar & Contacts ---
        if (isCalendarCommand(lower)) {
            return VoiceCommand.OpenCalendar(rawInput = trimmed)
        }
        if (isContactsCommand(lower)) {
            return VoiceCommand.OpenContacts(rawInput = trimmed)
        }

        // --- J. Device Info ---
        if (isDeviceInfoCommand(lower)) {
            return VoiceCommand.GetDeviceInfo(rawInput = trimmed)
        }

        // --- K. SMS Messaging ---
        val smsCommand = parseSmsCommand(trimmed, lower)
        if (smsCommand != null) return smsCommand

        // --- L. Open App (Dynamic Installed Apps Matching) ---
        val openAppCommand = parseOpenAppCommand(trimmed, lower)
        if (openAppCommand != null) return openAppCommand

        // --- M. Web Search ---
        val searchCommand = parseSearchCommand(trimmed, lower)
        if (searchCommand != null) return searchCommand

        // --- N. Reminders ---
        if (isReminderCommand(lower)) {
            val title = extractReminderTitle(trimmed, lower)
            return VoiceCommand.SetReminder(title = title, rawInput = trimmed)
        }

        // Default: Forward to Generative AI model
        return VoiceCommand.AiChat(prompt = trimmed)
    }

    private fun isWhoIsCallingCommand(lower: String): Boolean {
        return lower.contains("who is calling") ||
                lower.contains("kaun call kar raha hai") ||
                lower.contains("kiska phone hai") ||
                lower.contains("kiska call hai") ||
                lower.contains("who is on the call") ||
                lower.contains("caller kaun hai")
    }

    private fun isAnswerCallCommand(lower: String): Boolean {
        return lower.contains("answer call") ||
                lower.contains("answer the call") ||
                lower.contains("call uthao") ||
                lower.contains("call receive karo") ||
                lower.contains("phone uthao") ||
                lower.contains("receive call")
    }

    private fun isRejectCallCommand(lower: String): Boolean {
        return lower.contains("reject call") ||
                lower.contains("reject the call") ||
                lower.contains("call reject karo") ||
                lower.contains("call kato") ||
                lower.contains("phone kato") ||
                lower.contains("cut call") ||
                lower.contains("decline call")
    }

    private fun parseCallCommand(raw: String, lower: String): VoiceCommand.CallContact? {
        val isCallTrigger = lower.startsWith("call ") ||
                lower.startsWith("phone ") ||
                lower.startsWith("dial ") ||
                lower.endsWith(" ko call karo") ||
                lower.endsWith(" ko call lagao") ||
                lower.endsWith(" ko phone lagao") ||
                lower.endsWith(" ko phone karo") ||
                lower.contains(" call karo")

        if (!isCallTrigger) return null

        var target = raw
        if (lower.startsWith("call ")) {
            target = raw.substring(5).trim()
        } else if (lower.startsWith("phone ")) {
            target = raw.substring(6).trim()
        } else if (lower.startsWith("dial ")) {
            target = raw.substring(5).trim()
        } else {
            target = raw.replace("(?i)\\s+(ko)?\\s+(call|phone)\\s+(karo|lagao|milao)".toRegex(), "").trim()
        }

        // Avoid triggering on "call control" phrases
        if (target.isBlank() || target.equals("reject", ignoreCase = true) || target.equals("answer", ignoreCase = true)) {
            return null
        }

        return VoiceCommand.CallContact(target = target, confirmedDirectCall = false, rawInput = raw)
    }

    private fun parseWhatsAppCommand(raw: String, lower: String): VoiceCommand? {
        if (!lower.contains("whatsapp") && !lower.contains("whats app")) return null

        // "Open WhatsApp" / "WhatsApp kholo"
        if (lower == "open whatsapp" || lower == "whatsapp kholo" || lower == "whatsapp open karo" ||
            lower == "open whats app" || lower == "whats app kholo" || lower == "whatsapp chalao" ||
            lower == "whatsapp"
        ) {
            return VoiceCommand.OpenWhatsApp(rawInput = raw)
        }

        // "Rahul ko WhatsApp message bhejo: Main aa raha hoon" or "Send WhatsApp message to Rahul: I am coming"
        // Patterns:
        // "<Target> ko whatsapp message bhejo: <Message>"
        // "whatsapp par <Target> ko message bhejo: <Message>"
        val colonSplit = raw.split(":")
        if (colonSplit.size >= 2) {
            val header = colonSplit[0].trim()
            val body = colonSplit.subList(1, colonSplit.size).joinToString(":").trim()
            val target = extractWhatsAppTarget(header)
            return VoiceCommand.WhatsAppMessage(target = target, body = body, rawInput = raw)
        }

        // "Rahul ko whatsapp par bolo main late ho jaunga"
        val boloPattern = Regex("(?i)^(.+?)\\s+ko\\s+whatsapp\\s+(par|pe)\\s+(bolo|bhejo)\\s+(.+)$")
        val boloMatch = boloPattern.find(raw)
        if (boloMatch != null) {
            val target = boloMatch.groupValues[1].trim()
            val message = boloMatch.groupValues[4].trim()
            return VoiceCommand.WhatsAppMessage(target = target, body = message, rawInput = raw)
        }

        // "Send WhatsApp message to Rahul" -> prepare message with empty body
        val sendToMatch = Regex("(?i)^(send|bhejo)\\s+whatsapp\\s+(message\\s+)?(to\\s+)?(.+)$").find(raw)
        if (sendToMatch != null) {
            val target = sendToMatch.groupValues[4].trim()
            return VoiceCommand.WhatsAppMessage(target = target, body = "", rawInput = raw)
        }

        // "Rahul ka whatsapp chat kholo" / "Open Rahul whatsapp chat"
        val chatMatch = Regex("(?i)^(.+?)\\s+(ka|ki)?\\s*whatsapp\\s+chat\\s+(kholo|open karo)$").find(raw)
        if (chatMatch != null) {
            val target = chatMatch.groupValues[1].trim()
            return VoiceCommand.WhatsAppChat(target = target, rawInput = raw)
        }

        return VoiceCommand.OpenWhatsApp(rawInput = raw)
    }

    private fun extractWhatsAppTarget(header: String): String {
        return header.replace("(?i)(send|bhejo|message|whatsapp|whats app|par|pe|ko|to)".toRegex(), "")
            .trim()
            .ifBlank { "Contact" }
    }

    private fun isCameraCommand(lower: String): Boolean {
        return lower.contains("camera kholo") ||
                lower.contains("camera open karo") ||
                lower.contains("open camera") ||
                lower.contains("photo khicho") ||
                lower.contains("take photo") ||
                lower.contains("take a picture") ||
                lower == "camera"
    }

    private fun parseVolumeCommand(raw: String, lower: String): VoiceCommand.AdjustVolume? {
        if (!lower.contains("volume") && !lower.contains("awaaz") && !lower.contains("awaz") && !lower.contains("sound")) {
            return null
        }

        if (lower.contains("mute") || lower.contains("awaaz band") || lower.contains("silent")) {
            return VoiceCommand.AdjustVolume(mute = true, rawInput = raw)
        }

        // Percentage check: "set volume to 50%" or "volume 50 percent karo"
        val percentMatcher = Pattern.compile("(\\d{1,3})\\s*(%|percent)").matcher(lower)
        if (percentMatcher.find()) {
            val p = percentMatcher.group(1)?.toIntOrNull()
            if (p != null) {
                return VoiceCommand.AdjustVolume(percent = p.coerceIn(0, 100), rawInput = raw)
            }
        }

        val isIncrease = lower.contains("increase") || lower.contains("up") ||
                lower.contains("raise") || lower.contains("badhao") ||
                lower.contains("jyada") || lower.contains("high")
        val isDecrease = lower.contains("decrease") || lower.contains("down") ||
                lower.contains("lower") || lower.contains("kam") ||
                lower.contains("ghatao") || lower.contains("low")

        return when {
            isIncrease -> VoiceCommand.AdjustVolume(raise = true, rawInput = raw)
            isDecrease -> VoiceCommand.AdjustVolume(raise = false, rawInput = raw)
            else -> VoiceCommand.AdjustVolume(raise = true, rawInput = raw)
        }
    }

    private fun parseBrightnessCommand(raw: String, lower: String): VoiceCommand.AdjustBrightness? {
        if (!lower.contains("brightness") && !lower.contains("roshni")) return null

        val percentMatcher = Pattern.compile("(\\d{1,3})\\s*(%|percent)").matcher(lower)
        if (percentMatcher.find()) {
            val p = percentMatcher.group(1)?.toIntOrNull()
            if (p != null) {
                return VoiceCommand.AdjustBrightness(percent = p.coerceIn(0, 100), rawInput = raw)
            }
        }

        val isIncrease = lower.contains("increase") || lower.contains("up") || lower.contains("badhao")
        val isDecrease = lower.contains("decrease") || lower.contains("down") || lower.contains("kam")

        return when {
            isIncrease -> VoiceCommand.AdjustBrightness(raise = true, rawInput = raw)
            isDecrease -> VoiceCommand.AdjustBrightness(raise = false, rawInput = raw)
            else -> VoiceCommand.AdjustBrightness(raise = true, rawInput = raw)
        }
    }

    private fun isBluetoothCommand(lower: String): Boolean {
        return lower.contains("bluetooth") && (
                lower.contains("on") || lower.contains("off") ||
                        lower.contains("kholo") || lower.contains("karo") ||
                        lower.contains("band") || lower.contains("chalu") ||
                        lower.contains("open") || lower.contains("settings")
                )
    }

    private fun isWifiCommand(lower: String): Boolean {
        return (lower.contains("wi-fi") || lower.contains("wifi")) && (
                lower.contains("kholo") || lower.contains("open") ||
                        lower.contains("settings") || lower.contains("on") ||
                        lower.contains("off")
                )
    }

    private fun isSettingsCommand(lower: String): Boolean {
        return lower == "settings" || lower == "setting" ||
                lower.contains("settings kholo") || lower.contains("setting kholo") ||
                lower.contains("open settings") || lower.contains("open setting") ||
                lower.contains("settings open karo")
    }

    private fun parseTimerCommand(raw: String, lower: String): VoiceCommand.SetTimer? {
        if (!lower.contains("timer")) return null

        val minMatcher = Pattern.compile("(\\d+)\\s*(minute|min|m)", Pattern.CASE_INSENSITIVE).matcher(lower)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toIntOrNull() ?: 1
            return VoiceCommand.SetTimer(seconds = mins * 60, message = "REXYY Timer", rawInput = raw)
        }

        val secMatcher = Pattern.compile("(\\d+)\\s*(second|sec|s)", Pattern.CASE_INSENSITIVE).matcher(lower)
        if (secMatcher.find()) {
            val secs = secMatcher.group(1)?.toIntOrNull() ?: 30
            return VoiceCommand.SetTimer(seconds = secs, message = "REXYY Timer", rawInput = raw)
        }

        return VoiceCommand.SetTimer(seconds = 300, message = "REXYY Timer", rawInput = raw)
    }

    private fun parseAlarmCommand(raw: String, lower: String): VoiceCommand.SetAlarm? {
        val isAlarmTrigger = lower.contains("alarm") || lower.contains("wake me up") ||
                lower.contains("baje uthao") || lower.contains("baje jagao")
        if (!isAlarmTrigger) return null

        val colonMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE).matcher(lower)
        if (colonMatcher.find()) {
            var hour = colonMatcher.group(1)?.toIntOrNull() ?: 7
            val min = colonMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = colonMatcher.group(3)?.lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return VoiceCommand.SetAlarm(hour = hour, minute = min, message = "REXYY Alarm", rawInput = raw)
        }

        val hourMatcher = Pattern.compile("(\\d{1,2})\\s*(am|pm|baje|o'clock)", Pattern.CASE_INSENSITIVE).matcher(lower)
        if (hourMatcher.find()) {
            var hour = hourMatcher.group(1)?.toIntOrNull() ?: 7
            val modifier = hourMatcher.group(2)?.lowercase()
            if (modifier == "pm" && hour < 12) hour += 12
            if (modifier == "am" && hour == 12) hour = 0
            return VoiceCommand.SetAlarm(hour = hour, minute = 0, message = "REXYY Alarm", rawInput = raw)
        }

        return VoiceCommand.SetAlarm(hour = 7, minute = 0, message = "REXYY Alarm", rawInput = raw)
    }

    private fun isCalendarCommand(lower: String): Boolean {
        return lower.contains("calendar kholo") ||
                lower.contains("open calendar") ||
                lower.contains("show calendar") ||
                lower == "calendar"
    }

    private fun isContactsCommand(lower: String): Boolean {
        return lower.contains("contacts kholo") ||
                lower.contains("open contacts") ||
                lower.contains("phonebook") ||
                lower == "contacts"
    }

    private fun isDeviceInfoCommand(lower: String): Boolean {
        return lower.contains("device information") ||
                lower.contains("device info") ||
                lower.contains("phone info") ||
                lower.contains("system info") ||
                lower.contains("phone details")
    }

    private fun parseSmsCommand(raw: String, lower: String): VoiceCommand.SendMessage? {
        val isSms = lower.startsWith("send sms ") ||
                lower.startsWith("send message ") ||
                lower.contains("sms bhejo") ||
                (lower.contains("ko message bhejo") && !lower.contains("whatsapp"))

        if (!isSms) return null

        val colonSplit = raw.split(":")
        if (colonSplit.size >= 2) {
            val target = colonSplit[0].replace("(?i)(send|sms|message|bhejo|ko|to)".toRegex(), "").trim()
            val body = colonSplit.subList(1, colonSplit.size).joinToString(":").trim()
            return VoiceCommand.SendMessage(target = target, body = body, rawInput = raw)
        }

        val target = raw.replace("(?i)(send|sms|message|bhejo|ko|to)".toRegex(), "").trim()
        return VoiceCommand.SendMessage(target = target.ifBlank { "Contact" }, body = "", rawInput = raw)
    }

    private fun parseOpenAppCommand(raw: String, lower: String): VoiceCommand.OpenApp? {
        // "Open <app>" or "Launch <app>"
        val openPrefixes = listOf("open ", "launch ", "start ")
        for (prefix in openPrefixes) {
            if (lower.startsWith(prefix)) {
                val app = raw.substring(prefix.length).trim()
                if (app.isNotBlank() && !app.equals("settings", ignoreCase = true) && !app.equals("bluetooth", ignoreCase = true)) {
                    return VoiceCommand.OpenApp(appName = app, rawInput = raw)
                }
            }
        }

        // "<app> kholo", "<app> open karo", "<app> chalao", "<app> khol do", "<app> chalu karo"
        val hinglishSuffixes = listOf(" kholo", " open karo", " chalao", " khol do", " chalu karo", " start karo")
        for (suffix in hinglishSuffixes) {
            if (lower.endsWith(suffix)) {
                val app = raw.substring(0, raw.length - suffix.length).trim()
                if (app.isNotBlank() && !app.equals("settings", ignoreCase = true) && !app.equals("bluetooth", ignoreCase = true)) {
                    return VoiceCommand.OpenApp(appName = app, rawInput = raw)
                }
            }
        }

        return null
    }

    private fun parseSearchCommand(raw: String, lower: String): VoiceCommand.GoogleSearch? {
        if (lower.startsWith("search google for ")) {
            return VoiceCommand.GoogleSearch(raw.substring(18).trim(), raw)
        }
        if (lower.startsWith("search for ")) {
            return VoiceCommand.GoogleSearch(raw.substring(11).trim(), raw)
        }
        if (lower.startsWith("search ")) {
            return VoiceCommand.GoogleSearch(raw.substring(7).trim(), raw)
        }
        if (lower.endsWith(" search karo") || lower.endsWith(" google karo")) {
            val q = raw.replace("(?i)(search|google)\\s+karo".toRegex(), "").trim()
            return VoiceCommand.GoogleSearch(q, raw)
        }
        return null
    }

    private fun isReminderCommand(lower: String): Boolean {
        return lower.startsWith("remind me ") || lower.contains("yaad dilao")
    }

    private fun extractReminderTitle(raw: String, lower: String): String {
        return raw.replace("(?i)^remind me to\\s+".toRegex(), "")
            .replace("(?i)^remind me\\s+".toRegex(), "")
            .replace("(?i)yaad dilao".toRegex(), "")
            .trim()
            .ifBlank { "Reminder" }
    }
}
