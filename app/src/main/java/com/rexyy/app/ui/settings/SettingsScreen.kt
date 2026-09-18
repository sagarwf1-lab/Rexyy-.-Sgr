package com.rexyy.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexyy.app.ui.theme.RexyyCyanPrimary
import com.rexyy.app.ui.theme.RexyyDarkBackground
import com.rexyy.app.ui.theme.RexyyDarkBorder
import com.rexyy.app.ui.theme.RexyyDarkSurface
import com.rexyy.app.ui.theme.RexyyDarkSurfaceVariant
import com.rexyy.app.ui.theme.RexyyErrorRed
import com.rexyy.app.ui.theme.RexyyNeonGreen
import com.rexyy.app.ui.theme.RexyyTextMuted
import com.rexyy.app.ui.theme.RexyyTextPrimary
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.rexyy.app.data.local.SecureStorage
import com.rexyy.app.ui.theme.RexyyTextSecondary
import com.rexyy.app.utils.SecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentMaskedKey: String,
    currentModel: String,
    isVoiceCommandsEnabled: Boolean = true,
    isVoiceRepliesEnabled: Boolean = true,
    voiceLanguage: String = SecureStorage.VOICE_LANG_DEFAULT,
    onBackClick: () -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onUpdateModel: (String) -> Unit,
    onUpdateVoiceCommandsEnabled: (Boolean) -> Unit = {},
    onUpdateVoiceRepliesEnabled: (Boolean) -> Unit = {},
    onUpdateVoiceLanguage: (String) -> Unit = {},
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var newApiKey by remember { mutableStateOf("") }
    var isNewKeyVisible by remember { mutableStateOf(false) }
    var showClearKeyDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val availableModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(currentModel) }

    var voiceCommandsOn by remember { mutableStateOf(isVoiceCommandsEnabled) }
    var voiceRepliesOn by remember { mutableStateOf(isVoiceRepliesEnabled) }
    var selectedLanguage by remember { mutableStateOf(voiceLanguage) }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

    val languageOptions = listOf(
        SecureStorage.VOICE_LANG_DEFAULT to "System Default",
        SecureStorage.VOICE_LANG_EN to "English (US)",
        SecureStorage.VOICE_LANG_HI to "Hindi (India)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = RexyyTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = RexyyTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RexyyDarkSurface,
                    titleContentColor = RexyyTextPrimary
                )
            )
        },
        containerColor = RexyyDarkBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Current Key Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RexyyDarkSurface),
                border = BorderStroke(1.dp, RexyyDarkBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = "Key Icon",
                            tint = RexyyCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stored API Key",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = RexyyTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (currentMaskedKey.isNotBlank()) currentMaskedKey else "No API key configured",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (currentMaskedKey.isNotBlank()) RexyyNeonGreen else RexyyTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Encrypted in Android KeyStore (AES-256 GCM). Complete key is never shown or logged.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RexyyTextMuted)
                    )

                    if (currentMaskedKey.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showClearKeyDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RexyyErrorRed
                            ),
                            border = BorderStroke(1.dp, RexyyErrorRed.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("delete_api_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = "Clear Key",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Stored Key")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Replace / Update Key Section
            Card(
                colors = CardDefaults.cardColors(containerColor = RexyyDarkSurface),
                border = BorderStroke(1.dp, RexyyDarkBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Update API Key",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RexyyTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newApiKey,
                        onValueChange = { newApiKey = it },
                        placeholder = { Text("Paste new API key here") },
                        label = { Text("New Key") },
                        singleLine = true,
                        visualTransformation = if (isNewKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = RexyyCyanPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isNewKeyVisible = !isNewKeyVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNewKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isNewKeyVisible) "Hide key" else "Show key",
                                    tint = RexyyTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = RexyyDarkSurfaceVariant,
                            unfocusedContainerColor = RexyyDarkSurfaceVariant,
                            focusedBorderColor = RexyyCyanPrimary,
                            unfocusedBorderColor = RexyyDarkBorder,
                            focusedTextColor = RexyyTextPrimary,
                            unfocusedTextColor = RexyyTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (SecurityUtils.isValidApiKey(newApiKey)) {
                                onUpdateApiKey(newApiKey.trim())
                                newApiKey = ""
                                Toast.makeText(context, "API Key updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid key: must be at least 10 characters", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = newApiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RexyyCyanPrimary,
                            contentColor = RexyyDarkBackground
                        ),
                        modifier = Modifier.testTag("save_new_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = "Save Key",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Key")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Model Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RexyyDarkSurface),
                border = BorderStroke(1.dp, RexyyDarkBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = "Model",
                            tint = RexyyCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Model",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = RexyyTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownExpanded,
                        onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = RexyyDarkSurfaceVariant,
                                unfocusedContainerColor = RexyyDarkSurfaceVariant,
                                focusedBorderColor = RexyyCyanPrimary,
                                unfocusedBorderColor = RexyyDarkBorder,
                                focusedTextColor = RexyyTextPrimary,
                                unfocusedTextColor = RexyyTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier.background(RexyyDarkSurface)
                        ) {
                            availableModels.forEach { modelName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = modelName,
                                            color = if (modelName == selectedModel) RexyyCyanPrimary else RexyyTextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedModel = modelName
                                        onUpdateModel(modelName)
                                        isModelDropdownExpanded = false
                                        Toast.makeText(context, "Model set to $modelName", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voice & Speech Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RexyyDarkSurface),
                border = BorderStroke(1.dp, RexyyDarkBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Voice Settings",
                            tint = RexyyCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice & Speech",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = RexyyTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Commands Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Commands",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = RexyyTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Recognize device actions (Open YouTube, set alarm, adjust volume)",
                                style = MaterialTheme.typography.bodySmall.copy(color = RexyyTextSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = voiceCommandsOn,
                            onCheckedChange = { isChecked ->
                                voiceCommandsOn = isChecked
                                onUpdateVoiceCommandsEnabled(isChecked)
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Voice commands enabled" else "Voice commands disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RexyyDarkBackground,
                                checkedTrackColor = RexyyCyanPrimary,
                                uncheckedThumbColor = RexyyTextMuted,
                                uncheckedTrackColor = RexyyDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("voice_commands_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Replies (TTS) Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Replies (TTS)",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = RexyyTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Read AI responses and command confirmations aloud",
                                style = MaterialTheme.typography.bodySmall.copy(color = RexyyTextSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = voiceRepliesOn,
                            onCheckedChange = { isChecked ->
                                voiceRepliesOn = isChecked
                                onUpdateVoiceRepliesEnabled(isChecked)
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Voice replies enabled" else "Voice replies disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RexyyDarkBackground,
                                checkedTrackColor = RexyyCyanPrimary,
                                uncheckedThumbColor = RexyyTextMuted,
                                uncheckedTrackColor = RexyyDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("voice_replies_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speech Language Selection
                    Text(
                        text = "Speech Language",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = RexyyTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isLanguageDropdownExpanded,
                        onExpandedChange = { isLanguageDropdownExpanded = !isLanguageDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentLangLabel = languageOptions.find { it.first == selectedLanguage }?.second ?: "System Default"

                        OutlinedTextField(
                            value = currentLangLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLanguageDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = RexyyDarkSurfaceVariant,
                                unfocusedContainerColor = RexyyDarkSurfaceVariant,
                                focusedBorderColor = RexyyCyanPrimary,
                                unfocusedBorderColor = RexyyDarkBorder,
                                focusedTextColor = RexyyTextPrimary,
                                unfocusedTextColor = RexyyTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("voice_language_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = isLanguageDropdownExpanded,
                            onDismissRequest = { isLanguageDropdownExpanded = false },
                            modifier = Modifier.background(RexyyDarkSurface)
                        ) {
                            languageOptions.forEach { (langCode, langLabel) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = langLabel,
                                            color = if (langCode == selectedLanguage) RexyyCyanPrimary else RexyyTextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedLanguage = langCode
                                        onUpdateVoiceLanguage(langCode)
                                        isLanguageDropdownExpanded = false
                                        Toast.makeText(context, "Voice language: $langLabel", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Conversation History Cleanup
            Card(
                colors = CardDefaults.cardColors(containerColor = RexyyDarkSurface),
                border = BorderStroke(1.dp, RexyyDarkBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data & Privacy",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RexyyTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All conversation history is stored locally in Room database on your device.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = RexyyTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showClearHistoryDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RexyyErrorRed),
                        border = BorderStroke(1.dp, RexyyErrorRed.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = "Clear Chat History",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Messages")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showClearKeyDialog) {
        AlertDialog(
            onDismissRequest = { showClearKeyDialog = false },
            title = { Text("Remove API Key?") },
            text = { Text("Are you sure you want to remove your stored API key? You will need to enter it again to interact with REXYY.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearApiKey()
                        showClearKeyDialog = false
                        Toast.makeText(context, "API Key removed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Remove", color = RexyyErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearKeyDialog = false }) {
                    Text("Cancel", color = RexyyTextPrimary)
                }
            },
            containerColor = RexyyDarkSurface,
            titleContentColor = RexyyTextPrimary,
            textContentColor = RexyyTextSecondary
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("This will permanently delete all messages stored in your local conversation history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Conversation history cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = RexyyErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = RexyyTextPrimary)
                }
            },
            containerColor = RexyyDarkSurface,
            titleContentColor = RexyyTextPrimary,
            textContentColor = RexyyTextSecondary
        )
    }
}
