package com.gymmanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import android.content.Context
import coil.compose.AsyncImage
import com.gymmanager.data.model.*
import com.gymmanager.ui.Screen
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.utils.DateUtils
import com.gymmanager.viewmodel.GymViewModel
import com.gymmanager.dataStore
import com.gymmanager.DataStoreKeys
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileScreen(vm: GymViewModel, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val member by vm.selectedMember.collectAsState()
    val lastSyncTime by vm.lastSyncTime.collectAsState()
    val m = member ?: return

    val payments   by vm.getMemberPayments(m.id).collectAsState(emptyList())
    val attendance by vm.getMemberAttendance(m.id).collectAsState(emptyList())

    val whatsappEnabled by context.dataStore.data.map { it[DataStoreKeys.WHATSAPP_ENABLED] ?: true }.collectAsState(true)
    val smsEnabled      by context.dataStore.data.map { it[DataStoreKeys.SMS_ENABLED] ?: false }.collectAsState(false)

    var showPaymentSheet  by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var selectedTab       by remember { mutableStateOf(0) }
    var paymentAmount     by remember { mutableStateOf("") }
    var paymentMethod     by remember { mutableStateOf("Cash") }
    var paymentNote       by remember { mutableStateOf("") }

    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var isPressingPhoto   by remember { mutableStateOf(false) }
    val photoScale        by animateFloatAsState(if (isPressingPhoto) 1.2f else 1f)

    fun sendWhatsApp() {
        val msg = "السلام علیکم ${m.name}! آپ کی جم فیس باقی ہے Rs. ${"%,.0f".format(m.amountDue)}۔ براہ کرم جلد ادا کریں۔ شکریہ - Gym Manager"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/92${m.phone.replace("+92","").replace(" ","")}?text=${Uri.encode(msg)}"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try { context.startActivity(intent) } catch(e: Exception) { }
    }

    fun sendSms() {
        val msg = "Dear ${m.name}, Your gym fee Rs. ${"%,.0f".format(m.amountDue)} is pending. Please pay soon. - Gym Manager"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${m.phone}"))
        intent.putExtra("sms_body", msg)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try { context.startActivity(intent) } catch(e: Exception) { }
    }

    Column(Modifier.fillMaxSize().background(GymBgDark)) {
        // Header
        Box(Modifier.fillMaxWidth().background(GymBgCard)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GymBgElevated)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GymYellow)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Member Profile", style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleBlockMember(m.id, !m.isBlocked) },
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (m.isBlocked) Color(0x1A4CAF50) else Color(0x1AEF5350))) {
                        Icon(if (m.isBlocked) Icons.Default.LockOpen else Icons.Default.Block, null, tint = if (m.isBlocked) GymGreenBright else StatusUnpaid)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onNavigate(Screen.EditMember.createRoute(m.id)) },
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1AFFD600))) {
                        Icon(Icons.Default.Edit, null, tint = GymYellow)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1AEF5350))) {
                        Icon(Icons.Default.Delete, null, tint = StatusUnpaid)
                    }
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(GymYellow))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card
            item {
                Surface(color = GymBgCard, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(48.dp))
                            .scale(photoScale)
                            .background(GymBgElevated)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressingPhoto = true
                                        tryAwaitRelease()
                                        isPressingPhoto = false
                                    },
                                    onLongPress = { showEnlargedPhoto = true }
                                )
                            },
                            contentAlignment = Alignment.Center) {
                            if (!m.photoUri.isNullOrBlank()) {
                                AsyncImage(m.photoUri, m.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text(m.name.take(1).uppercase(), fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold, color = GymYellow)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(m.name, style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(m.phone, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Status badge
                            val (statusText, statusBg, statusFg) = when(m.status) {
                                MemberStatus.PAID    -> Triple("✓ Paid & Active",   Color(0x1A66BB6A), StatusPaid)
                                MemberStatus.UNPAID  -> Triple("✗ Payment Pending", Color(0x1AEF5350), StatusUnpaid)
                                MemberStatus.PARTIAL -> Triple("◑ Partial Payment", Color(0x1AFFD600), StatusPartial)
                            }
                            if (m.isBlocked) {
                                Surface(color = Color(0x1AEF5350), shape = RoundedCornerShape(8.dp)) {
                                    Text("BLOCKED", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium, color = StatusUnpaid, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
                                    Text(statusText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium, color = statusFg, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Shift badge
                            Surface(color = GymBgElevated, shape = RoundedCornerShape(8.dp)) {
                                Text(when(m.timeShift) {
                                    TimeShift.SHIFT_1   -> "☀️ 4 PM – 6 PM"
                                    TimeShift.SHIFT_2   -> "🌤 6 PM – 8 PM"
                                    TimeShift.SHIFT_3   -> "🌇 8 PM – 10 PM"
                                    TimeShift.SHIFT_4   -> "🌙 10 PM – 12 AM"
                                    TimeShift.CUSTOM    -> "🕐 ${m.customShiftTime ?: "Custom"}"
                                }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            // Sync status badge
                            val isSynced = m.updatedAt <= lastSyncTime
                            Surface(color = if (isSynced) Color(0x1A4CAF50) else Color(0x1AFFD600), shape = RoundedCornerShape(8.dp)) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                        null, modifier = Modifier.size(14.dp),
                                        tint = if (isSynced) GymGreenBright else GymYellow
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isSynced) "Synced" else "Pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSynced) GymGreenBright else GymYellow)
                                }
                            }
                        }
                    }
                }
            }

            // Fee Summary
            item {
                Surface(color = GymBgCard, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(GymYellow))
                            Spacer(Modifier.width(8.dp))
                            Text("FEE SUMMARY", style = MaterialTheme.typography.labelSmall,
                                color = GymYellow, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeeRow("Total Fee",   "Rs. ${"%,.0f".format(m.totalFee)}", TextPrimary)
                        FeeRow("Amount Paid", "Rs. ${"%,.0f".format(m.amountPaid)}", StatusPaid)
                        HorizontalDivider(color = GymBgBorder, modifier = Modifier.padding(vertical = 8.dp))
                        FeeRow("Remaining",   "Rs. ${"%,.0f".format(m.amountDue)}",
                            if (m.amountDue > 0) StatusUnpaid else StatusPaid, isBold = true)
                    }
                }
            }

            // Subscription
            if (m.planName != null) {
                item {
                    Surface(color = GymBgCard, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(GymGreen))
                                Spacer(Modifier.width(8.dp))
                                Text("SUBSCRIPTION", style = MaterialTheme.typography.labelSmall,
                                    color = GymGreenBright, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            FeeRow("Plan", m.planName, TextPrimary)
                            if (m.subscriptionStart != null) FeeRow("Start", DateUtils.displayDate(m.subscriptionStart), TextSecondary)
                            if (m.subscriptionEnd != null) {
                                val days = DateUtils.daysUntil(m.subscriptionEnd)
                                val expired = DateUtils.isExpired(m.subscriptionEnd)
                                FeeRow("Expiry", DateUtils.displayDate(m.subscriptionEnd),
                                    if (expired) StatusUnpaid else if (days < 7) StatusPartial else StatusPaid)
                                if (!expired) FeeRow("Days Left", "$days days",
                                    if (days < 7) StatusPartial else StatusPaid, isBold = true)
                                else FeeRow("Status", "EXPIRED ⚠️", StatusUnpaid, isBold = true)
                            }
                            if (m.lastAttendance != null)
                                FeeRow("Last Seen", DateUtils.displayDate(m.lastAttendance), TextSecondary)
                        }
                    }
                }
            }

            // Member Details
            item {
                Surface(color = GymBgCard, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(GymGrey))
                            Spacer(Modifier.width(8.dp))
                            Text("MEMBER DETAILS", style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeeRow("CNIC", m.cnic, TextPrimary, isBold = true)
                        if (m.address.isNotBlank()) FeeRow("Address", m.address, TextSecondary)
                        FeeRow("Joined", DateUtils.displayDate(m.joinDate), TextSecondary)
                    }
                }
            }

            // Action Buttons
            item {
                // Pay Fee
                Button(
                    onClick = { showPaymentSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GymYellow)
                ) {
                    Icon(Icons.Default.AttachMoney, null, tint = GymBgDark)
                    Spacer(Modifier.width(8.dp))
                    Text("Pay Fee", color = GymBgDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item {
                // Mark Attendance
                Button(
                    onClick = { vm.toggleAttendance(m, DateUtils.todayString()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GymGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Mark Attendance Today", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item {
                // Send Reminder - uses WhatsApp or SMS based on settings
                Button(
                    onClick = {
                        if (whatsappEnabled) sendWhatsApp()
                        else if (smsEnabled) sendSms()
                        else sendWhatsApp() // default to whatsapp
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (whatsappEnabled) Color(0xFF25D366) else Color(0xFF1565C0)
                    )
                ) {
                    Icon(
                        if (whatsappEnabled) Icons.Default.Message else Icons.Default.Sms,
                        null, tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (whatsappEnabled) "Send WhatsApp Reminder" else "Send SMS Reminder",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }

            item {
                // Delete Member
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaid)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Member", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Tabs: Payments | Attendance
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GymBgCard,
                    contentColor = GymYellow,
                    indicator = { tabPositions ->
                        Box(Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp).background(GymYellow))
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Payments (${payments.size})",
                            color = if (selectedTab == 0) GymYellow else TextSecondary) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Attendance (${attendance.size})",
                            color = if (selectedTab == 1) GymYellow else TextSecondary) })
                }
            }

            if (selectedTab == 0) {
                if (payments.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        Text("No payments recorded", color = TextMuted) } }
                } else {
                    items(payments, key = { it.id }) { payment ->
                        Surface(color = GymBgCard, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x1AFFD600)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AttachMoney, null, tint = GymYellow)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(payment.method, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(DateUtils.displayDate(payment.paymentDate), color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Rs. ${"%,.0f".format(payment.amount)}", color = GymYellow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                if (attendance.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        Text("No attendance recorded", color = TextMuted) } }
                } else {
                    items(attendance, key = { it.id }) { rec ->
                        Surface(color = GymBgCard, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x1A4CAF50)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, null, tint = GymGreenBright)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(DateUtils.displayDate(rec.date), color = TextPrimary)
                                    Text(rec.timeShift.label.substringBefore(" ("), color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text(DateUtils.displayTime(rec.checkInTime), color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Enlarged Photo Dialog
    if (showEnlargedPhoto && !m.photoUri.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showEnlargedPhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { showEnlargedPhoto = false },
                contentAlignment = Alignment.Center) {
                AsyncImage(m.photoUri, m.name, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
            }
        }
    }

    // Pay Fee Sheet
    if (showPaymentSheet) {
        ModalBottomSheet(onDismissRequest = { showPaymentSheet = false }, containerColor = GymBgCard) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Record Payment", style = MaterialTheme.typography.titleLarge,
                    color = GymYellow, fontWeight = FontWeight.Bold)
                Text("Remaining: Rs. ${"%,.0f".format(m.amountDue)}", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(20.dp))
                GymTextField(value = paymentAmount, onValueChange = { paymentAmount = it },
                    label = "Amount (PKR)", placeholder = "Enter amount",
                    leadingIcon = Icons.Default.AttachMoney,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Spacer(Modifier.height(12.dp))
                Column {
                    Text("Payment Method", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash","Bank","Easypaisa","JazzCash").forEach { method ->
                            FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method },
                                label = { Text(method, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GymYellow, selectedLabelColor = GymBgDark,
                                    containerColor = GymBgElevated, labelColor = TextSecondary))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                GymTextField(value = paymentNote, onValueChange = { paymentNote = it },
                    label = "Note (Optional)", placeholder = "e.g. April fee",
                    leadingIcon = Icons.Default.Note)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        paymentAmount.toDoubleOrNull()?.let { amt ->
                            vm.recordPayment(m.id, amt, paymentMethod, paymentNote)
                            showPaymentSheet = false; paymentAmount = ""; paymentNote = ""
                        }
                    },
                    enabled = paymentAmount.toDoubleOrNull() != null && paymentAmount.toDoubleOrNull()!! > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GymYellow,
                        disabledContainerColor = GymYellow.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Check, null, tint = GymBgDark)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Payment", color = GymBgDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = GymBgCard,
            title = { Text("Delete Member", color = StatusUnpaid, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${m.name}? All payment and attendance records will also be removed.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.deleteMember(m.id); showDeleteDialog = false; onBack() }) {
                    Text("Delete", color = StatusUnpaid, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun FeeRow(label: String, value: String, valueColor: Color, isBold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}
