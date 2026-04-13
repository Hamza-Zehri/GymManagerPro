package com.gymmanager.ui.screens

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.gymmanager.ui.theme.*

@Composable
fun AppLockScreen(
    savedPin: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg   by remember { mutableStateOf("") }
    var attempts   by remember { mutableStateOf(0) }

    // Try biometric on launch
    LaunchedEffect(Unit) {
        tryBiometric(context, onSuccess = onUnlocked)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(GymBgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            // Logo
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(30.dp)).background(GymYellow),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FitnessCenter, null, tint = GymBgDark, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Gym Manager Pro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Enter PIN to continue", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).background(
                            if (index < enteredPin.length) GymYellow else GymBgElevated
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = StatusUnpaid, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))

            // PIN Keypad
            val keys = listOf(
                listOf("1","2","3"),
                listOf("4","5","6"),
                listOf("7","8","9"),
                listOf("⌫","0","✓")
            )
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(36.dp))
                                .background(when(key) {
                                    "✓" -> GymYellow
                                    "⌫" -> GymBgElevated
                                    else -> GymBgCard
                                })
                                .then(Modifier.padding(0.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    when(key) {
                                        "⌫" -> if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMsg = ""
                                        }
                                        "✓" -> {
                                            if (enteredPin == savedPin) {
                                                onUnlocked()
                                            } else {
                                                attempts++
                                                errorMsg = "Wrong PIN. Try again."
                                                enteredPin = ""
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                errorMsg = ""
                                                // Auto-submit when 4 digits entered
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == savedPin) {
                                                        onUnlocked()
                                                    } else {
                                                        attempts++
                                                        errorMsg = "Wrong PIN. Try again."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = when(key) {
                                        "✓" -> GymBgDark
                                        "⌫" -> TextSecondary
                                        else -> TextPrimary
                                    })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Fingerprint button
            OutlinedButton(
                onClick = { tryBiometric(context, onSuccess = onUnlocked) },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GymBgBorder)
            ) {
                Icon(Icons.Default.Fingerprint, null, tint = GymYellow, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Use Fingerprint", color = TextSecondary)
            }
        }
    }
}

fun tryBiometric(context: Context, onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    })
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Gym Manager Pro")
        .setSubtitle("Use fingerprint or PIN to unlock")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(promptInfo)
}
