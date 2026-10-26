package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AppControlManager
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldListening
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val appControl = remember { AppControlManager(context) }
    val activeLang by viewModel.activeLanguage.collectAsState()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val showClearDialog by viewModel.showClearHistoryDialog.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to assistant",
                    tint = NeonCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SETTINGS & CONTROLS",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        // Settings Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Language Preference Card
            SettingSectionCard(title = "LANGUAGE / ভাষা", icon = Icons.Default.Language) {
                Text(
                    text = "Select preferred conversation & speech recognition language:",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf("auto" to "Auto-Detect", "bn" to "বাংলা (Bengali)", "en" to "English")
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = activeLang == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyan,
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = activeLang == code,
                                selectedBorderColor = NeonCyan,
                                borderColor = CyberCardBorder
                            ),
                            modifier = Modifier.testTag("lang_chip_$code")
                        )
                    }
                }
            }

            // 2. Voice & Speech Synthesis Card
            SettingSectionCard(title = "TEXT-TO-SPEECH (TTS)", icon = Icons.AutoMirrored.Filled.VolumeUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spoken Answers",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Read AI answers aloud automatically in Bengali or English",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = CyberSurfaceElevated,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.testTag("tts_toggle_switch")
                    )
                }

                if (isTtsEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Speech Rate: ${String.format(java.util.Locale.US, "%.2f", speechRate)}x",
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.75f..1.5f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CyberCardBorder
                        ),
                        modifier = Modifier.testTag("speech_rate_slider")
                    )
                }
            }

            // 3. Android App Control Status
            SettingSectionCard(title = "DEVICE APP CONTROLS", icon = Icons.Default.PhoneAndroid) {
                Text(
                    text = "Status of supported apps on this Android device:",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                AppControlManager.SUPPORTED_APPS.forEach { app ->
                    val isInstalled = appControl.isAppInstalled(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.displayName,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isInstalled) EmeraldListening else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInstalled) "Installed" else "Web Available",
                                color = if (isInstalled) EmeraldListening else TextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // 4. Privacy, Safety & Security Rules
            SettingSectionCard(title = "PRIVACY & PLATFORM SAFETY", icon = Icons.Default.Lock) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "• Android Security: RIFAUL AI never reads private messages, passwords, or personal credentials without explicit authorization.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Text(
                        text = "• Action Verification: App launches require user confirmation. RIFAUL AI never fakes success if an intent fails.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Text(
                        text = "• Network Connectivity: Gemini AI natural language chat and cloud speech models require an active internet connection. Offline actions will provide clear feedback.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // 5. Data Management: Clear Chat History
            ElevatedButton(
                onClick = { viewModel.setShowClearHistoryDialog(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_history_btn"),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = NeonPink.copy(alpha = 0.15f),
                    contentColor = NeonPink
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Conversation History", fontSize = 13.sp)
            }

            // About footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RIFAUL AI v1.0 • Powered by Google Gemini 3.8 Flash",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearHistoryDialog(false) },
            title = {
                Text("Clear History?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete all saved conversation messages from this device? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearHistory() },
                    modifier = Modifier.testTag("confirm_clear_history_btn")
                ) {
                    Text("Clear All", color = NeonPink)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowClearHistoryDialog(false) }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
