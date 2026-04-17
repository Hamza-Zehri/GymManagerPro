package com.hamzaasad.gymmanagerpro.ui.screens

import android.net.wifi.WifiManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzaasad.gymmanagerpro.sync.SyncResult
import com.hamzaasad.gymmanagerpro.ui.theme.*
import com.hamzaasad.gymmanagerpro.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(vm: GymViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val syncResult by vm.syncResult.collectAsState()
    
    var isServerRunning by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces.toList()) {
                // Prioritize wlan0 (WiFi) and ap0 (Hotspot) interfaces
                if (networkInterface.name.contains("wlan") || networkInterface.name.contains("ap")) {
                    for (address in networkInterface.inetAddresses.toList()) {
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress ?: "192.168.43.1"
                        }
                    }
                }
            }
            // Fallback: Check any non-loopback IPv4
            for (networkInterface in interfaces.toList()) {
                for (address in networkInterface.inetAddresses.toList()) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "192.168.43.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Most Android hotspots default to this
        return "192.168.43.1"
    }

    LaunchedEffect(syncResult) { if (syncResult != null) isLoading = false }

    Column(Modifier.fillMaxSize().background(GymBgDark).verticalScroll(rememberScrollState())) {
        // Header
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GymBgCard)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GymYellow)
            }
            Text("Copy Data to New Phone", Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        // Help Card
        Surface(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            color = GymYellow.copy(0.1f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GymYellow.copy(0.2f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = GymYellow)
                Spacer(Modifier.width(12.dp))
                Text("Step 1: Open Hotspot on Phone A.\nStep 2: Connect Phone B to that Hotspot.", color = TextPrimary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Phone A Section
        SyncChoiceCard(
            title = "PHONE A (Sender)",
            subtitle = "Use this if you want to SEND data from this phone.",
            icon = Icons.Default.FileUpload,
            isActive = isServerRunning,
            onClick = {
                isServerRunning = !isServerRunning
                if (isServerRunning) {
                    serverIp = getLocalIpAddress()
                    vm.startSyncServer()
                } else {
                    vm.stopSyncServer()
                }
            }
        ) {
            if (isServerRunning) {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).background(GymBgDark).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Type this number on the other phone:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(serverIp, color = GymYellow, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("OR", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Phone B Section
        SyncChoiceCard(
            title = "PHONE B (Receiver)",
            subtitle = "Use this if this is your NEW phone and you want to GET data.",
            icon = Icons.Default.FileDownload,
            isActive = targetIp.isNotEmpty() || isLoading,
            onClick = {} // Controlled by input
        ) {
            Column(Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    value = targetIp,
                    onValueChange = { targetIp = it },
                    label = { Text("Enter number from Phone A", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymYellow, unfocusedBorderColor = GymBgBorder)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { if(targetIp.isNotEmpty()){ isLoading=true; vm.syncWithServer(targetIp) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GymYellow),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && targetIp.isNotEmpty()
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = GymBgDark)
                    else Text("START COPYING DATA", color = GymBgDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Result Status
        syncResult?.let { result ->
            val isSuccess = result is SyncResult.Success
            Surface(
                Modifier.padding(20.dp).fillMaxWidth(),
                color = (if (isSuccess) GymGreen else StatusUnpaid).copy(0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSuccess) GymGreen else StatusUnpaid)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (isSuccess) GymGreen else StatusUnpaid)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isSuccess) "Success! All data moved." else "Error: Try reconnecting hotspot.", color = if (isSuccess) GymGreen else StatusUnpaid, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SyncChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        color = GymBgCard,
        shape = RoundedCornerShape(20.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, GymYellow) else null
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) GymYellow else GymBgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (isActive) GymBgDark else GymYellow)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, color = if (isActive) GymYellow else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
            content()
        }
    }
}
