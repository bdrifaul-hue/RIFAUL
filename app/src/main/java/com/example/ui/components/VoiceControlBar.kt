package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldListening
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VoiceControlBar(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSubmitInput: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onQuickCommandClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickCommands = listOf(
        "YouTube চালু করো",
        "Facebook খোলো",
        "Chrome চালু করো",
        "এখন কয়টা বাজে?",
        "আজকের আবহাওয়া বলো",
        "হ্যালো RIFAUL AI",
        "Stop listening"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberSurface)
            .border(1.dp, CyberCardBorder)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Quick suggestion chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEach { cmd ->
                FilterChip(
                    selected = false,
                    onClick = { onQuickCommandClicked(cmd) },
                    label = { Text(cmd, fontSize = 11.5.sp, color = TextSecondary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CyberSurfaceElevated
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = CyberCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_cmd_${cmd.take(8).replace(' ', '_')}")
                )
            }
        }

        // Active State Banner (Listening / Speaking) with STOP control
        AnimatedVisibility(
            visible = isListening || isSpeaking,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isListening) EmeraldListening.copy(alpha = 0.15f)
                        else ElectricViolet.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isListening) EmeraldListening.copy(alpha = 0.4f)
                        else ElectricViolet.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening) EmeraldListening else ElectricViolet)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Listening to your voice..." else "RIFAUL AI Speaking...",
                        color = TextPrimary,
                        fontSize = 12.5.sp
                    )
                }

                // Visible Stop Button
                Surface(
                    onClick = onStopClicked,
                    shape = RoundedCornerShape(6.dp),
                    color = NeonPink.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink),
                    modifier = Modifier.testTag("stop_action_banner_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = NeonPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", color = NeonPink, fontSize = 11.5.sp)
                    }
                }
            }
        }

        // Input Controls Row: Mic Button, Text Input, Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Microphone Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (isListening) 12.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = if (isListening) EmeraldListening else NeonCyan
                    )
                    .clip(CircleShape)
                    .background(
                        if (isListening) {
                            Brush.radialGradient(listOf(EmeraldListening, Color(0xFF065F46)))
                        } else {
                            Brush.radialGradient(listOf(NeonCyan, ElectricViolet))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onMicClicked,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Start voice listening",
                        tint = if (isListening) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Field for typing commands
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        text = "Type or ask RIFAUL AI...",
                        color = TextMuted,
                        fontSize = 13.5.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurfaceElevated,
                    unfocusedContainerColor = CyberSurfaceElevated
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmitInput() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank()) NeonCyan else CyberSurfaceElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSubmitInput,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.size(44.dp).testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (inputText.isNotBlank()) Color.Black else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
