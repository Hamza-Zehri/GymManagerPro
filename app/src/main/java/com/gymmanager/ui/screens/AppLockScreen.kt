package com.gymmanager.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.fragment.app.FragmentActivity
import com.gymmanager.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    savedPin: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg   by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    // Try biometric on launch
    LaunchedEffect(Unit) {
        tryBiometric(context, onSuccess = onUnlocked)
    }

    suspend fun shake() {
        repeat(4) {
            shakeOffset.animateTo(10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
            shakeOffset.animateTo(-10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
        }
        shakeOffset.animateTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).offset(x = shakeOffset.value.dp)
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = GymYellow
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = GymBgDark, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Gym Manager Pro", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Enter your 4-digit PIN", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(48.dp))

            // PIN Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) GymYellow else GymBgElevated)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = StatusUnpaid, style = MaterialTheme.typography.labelMedium)
            } else {
                Spacer(Modifier.height(20.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Custom Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("F", "0", "D") // F for Fingerprint, D for Delete
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            KeypadButton(key) {
                                when (key) {
                                    "D" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMsg = ""
                                        }
                                    }
                                    "F" -> tryBiometric(context, onSuccess = onUnlocked)
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            errorMsg = ""
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            
                                            if (enteredPin.length == 4) {
                                                if (enteredPin == savedPin) {
                                                    onUnlocked()
                                                } else {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    errorMsg = "Incorrect PIN"
                                                    enteredPin = ""
                                                    // Trigger shake
                                                    scope.launch { shake() }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (key == "F" || key == "D") Color.Transparent else GymBgCard)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "D" -> Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            "F" -> Icon(Icons.Default.Fingerprint, null, tint = GymYellow, modifier = Modifier.size(32.dp))
            else -> Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

private fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

fun tryBiometric(context: Context, onSuccess: () -> Unit) {
    val activity = context.findActivity() ?: return
    val biometricManager = BiometricManager.from(context)
    
    // Only allow Strong Biometrics (Fingerprint/Face), exclude Device Credential (PIN/Pattern)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
    
    val canAuth = biometricManager.canAuthenticate(authenticators)
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    })
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Security Check")
        .setSubtitle("Use Fingerprint to unlock")
        .setAllowedAuthenticators(authenticators)
        .setNegativeButtonText("Use App PIN")
        .build()
    prompt.authenticate(promptInfo)
}
