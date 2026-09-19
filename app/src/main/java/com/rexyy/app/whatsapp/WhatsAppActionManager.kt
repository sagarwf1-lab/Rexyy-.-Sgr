package com.rexyy.app.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rexyy.app.launcher.AppLauncher
import com.rexyy.app.telecom.CallerIdentityResolver
import java.net.URLEncoder

sealed class WhatsAppActionResult {
    data class Success(val message: String) : WhatsAppActionResult()
    data class NotInstalled(val message: String = "WhatsApp is not installed on this device.") : WhatsAppActionResult()
    data class RequiresConfirmation(
        val targetName: String,
        val phoneNumber: String?,
        val messageText: String,
        val confirmationPrompt: String
    ) : WhatsAppActionResult()
    data class Failure(val error: String) : WhatsAppActionResult()
}

class WhatsAppActionManager(private val context: Context) {

    private val whatsAppPackage = "com.whatsapp"

    fun isWhatsAppInstalled(): Boolean {
        return context.packageManager.getLaunchIntentForPackage(whatsAppPackage) != null
    }

    fun openWhatsApp(): WhatsAppActionResult {
        return if (AppLauncher.launchPackage(context, whatsAppPackage)) {
            WhatsAppActionResult.Success("Opening WhatsApp...")
        } else {
            WhatsAppActionResult.NotInstalled("WhatsApp installed nahi hai.")
        }
    }

    fun prepareMessage(target: String, messageText: String): WhatsAppActionResult {
        if (!isWhatsAppInstalled()) {
            return WhatsAppActionResult.NotInstalled("WhatsApp is not installed on this device.")
        }

        val resolvedPhone = CallerIdentityResolver.findPhoneNumberByName(context, target)
        val prompt = if (!resolvedPhone.isNullOrBlank()) {
            "$target ($resolvedPhone) ko ye WhatsApp message bheju?\n\"$messageText\""
        } else {
            "$target ko ye WhatsApp message bheju?\n\"$messageText\""
        }

        return WhatsAppActionResult.RequiresConfirmation(
            targetName = target,
            phoneNumber = resolvedPhone,
            messageText = messageText,
            confirmationPrompt = prompt
        )
    }

    fun executeSendMessage(target: String, phoneNumber: String?, messageText: String): WhatsAppActionResult {
        if (!isWhatsAppInstalled()) {
            return WhatsAppActionResult.NotInstalled()
        }

        val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "")?.replace("+", "")

        return try {
            if (!cleanPhone.isNullOrBlank()) {
                val encodedText = URLEncoder.encode(messageText, "UTF-8")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(whatsAppPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                WhatsAppActionResult.Success("Opening WhatsApp chat with $target...")
            } else {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(whatsAppPackage)
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
                WhatsAppActionResult.Success("Sending WhatsApp message for $target...")
            }
        } catch (e: Exception) {
            WhatsAppActionResult.Failure("Failed to send WhatsApp message: ${e.localizedMessage}")
        }
    }

    fun openChat(target: String): WhatsAppActionResult {
        if (!isWhatsAppInstalled()) {
            return WhatsAppActionResult.NotInstalled()
        }

        val resolvedPhone = CallerIdentityResolver.findPhoneNumberByName(context, target)
        val cleanPhone = resolvedPhone?.replace(Regex("[^0-9+]"), "")?.replace("+", "")

        return try {
            if (!cleanPhone.isNullOrBlank()) {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(whatsAppPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                WhatsAppActionResult.Success("Opening chat with $target...")
            } else {
                openWhatsApp()
            }
        } catch (e: Exception) {
            WhatsAppActionResult.Failure("Could not open WhatsApp chat: ${e.localizedMessage}")
        }
    }
}
