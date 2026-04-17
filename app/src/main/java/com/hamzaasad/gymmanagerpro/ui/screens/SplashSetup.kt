package com.hamzaasad.gymmanagerpro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.hamzaasad.gymmanagerpro.data.model.GymInfo
import com.hamzaasad.gymmanagerpro.ui.components.*
import com.hamzaasad.gymmanagerpro.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
//  SPLASH SCREEN
// ─────────────────────────────────────────────
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(1600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .size(120.dp)
                    .background(Emerald500, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Gym Manager Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = alpha.value)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Manage your gym effortlessly",
                fontSize = 14.sp,
                color = Zinc400.copy(alpha = alpha.value)
            )
        }

        // Bottom credit
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Developed by", fontSize = 12.sp, color = Zinc600)
            Text("Hamza Asad", fontSize = 14.sp, color = Emerald400, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────
//  SETUP SCREEN
// ─────────────────────────────────────────────
@Composable
fun SetupScreen(onComplete: (GymInfo, String) -> Unit) {
    var gymName   by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var address   by remember { mutableStateOf("") }
    var pin       by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val isValid = gymName.isNotBlank() && ownerName.isNotBlank() && phone.isNotBlank() && 
                  pin.length == 4 && pin == confirmPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Hero icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Emerald500, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FitnessCenter, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("Setup Your Gym", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("Enter your gym details to get started", style = MaterialTheme.typography.bodyMedium, color = Zinc400)
        Spacer(Modifier.height(36.dp))

        GymTextField(
            value = gymName,
            onValueChange = { gymName = it },
            label = "Gym Name",
            placeholder = "e.g. Power Gym",
            leadingIcon = Icons.Default.FitnessCenter
        )
        Spacer(Modifier.height(16.dp))

        GymTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = "Owner Name",
            placeholder = "Your full name",
            leadingIcon = Icons.Default.Person
        )
        Spacer(Modifier.height(16.dp))

        GymTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            placeholder = "+92 300 1234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(16.dp))

        GymTextField(
            value = address,
            onValueChange = { address = it },
            label = "Address (Optional)",
            placeholder = "Gym location",
            leadingIcon = Icons.Default.LocationOn,
            singleLine = false,
            maxLines = 3
        )
        Spacer(Modifier.height(24.dp))
        
        Text("Security PIN", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text("This PIN will be required to open the app.", style = MaterialTheme.typography.bodySmall, color = Zinc400)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) {
                GymTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = "Set 4-Digit PIN",
                    placeholder = "1234",
                    leadingIcon = Icons.Default.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Box(Modifier.weight(1f)) {
                GymTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = "Confirm PIN",
                    placeholder = "1234",
                    leadingIcon = Icons.Default.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }
        
        if (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin) {
            Text("PINs do not match", color = StatusUnpaid, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(32.dp))

        PrimaryButton(
            text = "Get Started",
            onClick = {
                onComplete(
                    GymInfo(gymName = gymName.trim(), ownerName = ownerName.trim(), phone = phone.trim(), address = address.trim()),
                    pin
                )
            },
            enabled = isValid,
            icon = Icons.AutoMirrored.Filled.ArrowForward
        )

        Spacer(Modifier.height(24.dp))
    }
}
