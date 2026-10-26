package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AnimatedAiOrb
import com.example.ui.components.MessageBubble
import com.example.ui.components.OrbVisualState
import com.example.ui.components.VoiceControlBar
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldListening
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantUiState
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.allMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val inputQuery by viewModel.inputQuery.collectAsState()
    val pendingConfirmation by viewModel.pendingLaunchConfirmation.collectAsState()
    val activeLang by viewModel.activeLanguage.collectAsState()

    val listState = rememberLazyListState()

    // Scroll to latest message whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice commands / ভয়েস কমান্ডের জন্য মাইক্রোফোনের অনুমতি প্রয়োজন",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .statusBarsPadding()
    ) {
        // Futuristic Top App Bar
        TopAssistantBar(
            orbState = orbState,
            activeLanguage = activeLang,
            onSettingsClicked = onNavigateToSettings
        )

        // Error message banner if active
        if (uiState is AssistantUiState.Error) {
            val error = uiState as AssistantUiState.Error
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPink.copy(alpha = 0.2f))
                    .border(1.dp, NeonPink, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${error.messageEn} / ${error.messageBn}",
                    color = TextPrimary,
                    fontSize = 11.5.sp
                )
            }
        }

        // Hero AI Orb Banner (Compact & Futuristic)
        HeroAiOrbSection(
            orbState = orbState,
            audioRmsDb = audioRmsDb,
            uiState = uiState
        )

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    onSpeakClicked = { viewModel.speakMessage(it) },
                    onOpenWebClicked = { viewModel.openWebFallback(it) },
                    onOpenPlayStoreClicked = { viewModel.openInPlayStore(it) }
                )
            }
        }

        // Bottom Voice & Text Input Bar
        VoiceControlBar(
            inputText = inputQuery,
            onInputChanged = { viewModel.onInputChange(it) },
            onSubmitInput = { viewModel.submitCurrentInput() },
            isListening = orbState == OrbVisualState.LISTENING,
            isSpeaking = orbState == OrbVisualState.SPEAKING,
            onMicClicked = { handleMicClick() },
            onStopClicked = { viewModel.stopAllSpeech() },
            onQuickCommandClicked = { cmd ->
                viewModel.onInputChange(cmd)
                viewModel.submitCurrentInput()
            }
        )
    }

    // Sensitive Action Confirmation Dialog (App Launching)
    pendingConfirmation?.let { conf ->
        val app = conf.appTarget
        AlertDialog(
            onDismissRequest = { viewModel.dismissAppLaunchConfirmation() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm App Launch",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Would you like RIFAUL AI to open ${app.displayName} on your phone?",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "প্যাকেজ: ${app.packageName}\nনিরাপত্তা নীতি: ব্যবহারকারীর নিশ্চিতকরণ ব্যতীত অ্যাপ চালু করা হয় না।",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                ElevatedButton(
                    onClick = { viewModel.confirmAppLaunch(conf) },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("confirm_app_launch_btn")
                ) {
                    Text("Confirm Launch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAppLaunchConfirmation() },
                    modifier = Modifier.testTag("cancel_app_launch_btn")
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
private fun TopAssistantBar(
    orbState: OrbVisualState,
    activeLanguage: String,
    onSettingsClicked: () -> Unit
) {
    Surface(
        color = CyberSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (orbState) {
                                OrbVisualState.LISTENING -> EmeraldListening
                                OrbVisualState.THINKING -> PurpleGlow
                                OrbVisualState.SPEAKING -> NeonCyan
                                OrbVisualState.ERROR -> NeonPink
                                OrbVisualState.IDLE -> NeonCyan
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RIFAUL AI",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Real-time Status Badge & Settings Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Pill
                val statusText = when (orbState) {
                    OrbVisualState.LISTENING -> "LISTENING"
                    OrbVisualState.THINKING -> "THINKING"
                    OrbVisualState.SPEAKING -> "SPEAKING"
                    OrbVisualState.ERROR -> "ERROR"
                    OrbVisualState.IDLE -> "ONLINE"
                }
                val statusColor = when (orbState) {
                    OrbVisualState.LISTENING -> EmeraldListening
                    OrbVisualState.THINKING -> PurpleGlow
                    OrbVisualState.SPEAKING -> NeonCyan
                    OrbVisualState.ERROR -> NeonPink
                    OrbVisualState.IDLE -> TextMuted
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroAiOrbSection(
    orbState: OrbVisualState,
    audioRmsDb: Float,
    uiState: AssistantUiState
) {
    Surface(
        color = CyberDarkBg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated AI Orb
            AnimatedAiOrb(
                state = orbState,
                audioRmsDb = audioRmsDb,
                size = 140.dp
            )

            // Dynamic status helper text
            val guideText = when (orbState) {
                OrbVisualState.LISTENING -> "Listening to your voice... Speak now"
                OrbVisualState.THINKING -> "RIFAUL AI is processing with Gemini..."
                OrbVisualState.SPEAKING -> "RIFAUL AI is speaking..."
                OrbVisualState.ERROR -> "Service alert. Check internet connection"
                OrbVisualState.IDLE -> "Ready • Ask questions or command device apps in বাংলা / English"
            }

            Text(
                text = guideText,
                color = when (orbState) {
                    OrbVisualState.LISTENING -> EmeraldListening
                    OrbVisualState.THINKING -> PurpleGlow
                    OrbVisualState.SPEAKING -> NeonCyan
                    OrbVisualState.ERROR -> NeonPink
                    OrbVisualState.IDLE -> TextSecondary
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
