package com.gymmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gymmanager.data.model.SubscriptionPlan
import com.gymmanager.data.model.TimeShift
import com.gymmanager.data.model.Member
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.viewmodel.GymViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(vm: GymViewModel, onBack: () -> Unit) {
    val member by vm.selectedMember.collectAsState()
    val plans by vm.plans.collectAsState()

    // Form state
    var name          by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var address       by remember { mutableStateOf("") }
    var ageStr        by remember { mutableStateOf("") }
    var weight        by remember { mutableStateOf("") }
    var height        by remember { mutableStateOf("") }
    var goal          by remember { mutableStateOf("") }
    var photoUri      by remember { mutableStateOf<String?>(null) }
    var selectedShift by remember { mutableStateOf(TimeShift.SHIFT_1) }
    var customTime    by remember { mutableStateOf("") }
    var selectedPlan  by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var customFee     by remember { mutableStateOf("") }
    var showPlanSheet by remember { mutableStateOf(false) }
    var notes         by remember { mutableStateOf("") }

    // Initialize form with member data
    LaunchedEffect(member) {
        member?.let { m ->
            name = m.name
            phone = m.phone
            address = m.address
            ageStr = m.age.toString()
            weight = m.weight
            height = m.height
            goal = m.goal
            photoUri = m.photoUri
            selectedShift = m.timeShift
            customTime = m.customShiftTime ?: ""
            selectedPlan = plans.find { it.id == m.planId }
            customFee = m.totalFee.toString()
            notes = m.notes
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { photoUri = it.toString() }
    }

    val isValid = name.isNotBlank() && phone.isNotBlank()
    val totalFee = selectedPlan?.price ?: customFee.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GymBgDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GymBgCard)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GymBgElevated)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = GymYellow)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Edit Member", style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Update member details", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(GymYellow))

        Spacer(Modifier.height(16.dp))

        // Profile Photo
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(55.dp))
                    .background(GymBgElevated)
                    .clickable { imageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(photoUri, "Photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, null, tint = GymYellow, modifier = Modifier.size(52.dp))
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .size(30.dp).clip(RoundedCornerShape(15.dp)).background(GymYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = GymBgDark, modifier = Modifier.size(16.dp))
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            SectionLabel("PERSONAL INFORMATION")

            GymTextField(value = name, onValueChange = { name = it },
                label = "Full Name *", placeholder = "Enter member name", leadingIcon = Icons.Default.Person)

            GymTextField(value = phone, onValueChange = { phone = it },
                label = "Phone Number *", placeholder = "+92 300 1234567", leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymTextField(value = ageStr, onValueChange = { ageStr = it },
                    label = "Age", placeholder = "25",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f))
                GymTextField(value = weight, onValueChange = { weight = it },
                    label = "Weight (kg)", placeholder = "75",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymTextField(value = height, onValueChange = { height = it },
                    label = "Height (cm)", placeholder = "175",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
                GymTextField(value = goal, onValueChange = { goal = it },
                    label = "Fitness Goal", placeholder = "Build muscle",
                    modifier = Modifier.weight(1f))
            }

            GymTextField(value = address, onValueChange = { address = it },
                label = "Address", placeholder = "Member address",
                leadingIcon = Icons.Default.LocationOn, singleLine = false, maxLines = 2)

            GymTextField(value = notes, onValueChange = { notes = it },
                label = "Notes (Optional)", placeholder = "Any special notes",
                leadingIcon = Icons.Default.Note, singleLine = false, maxLines = 2)

            HorizontalDivider(color = GymBgBorder)

            SectionLabel("GYM TIME SHIFT")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeShift.values().forEach { shift ->
                    val isSelected = selectedShift == shift
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { selectedShift = shift },
                        color = if (isSelected) Color(0x33FFD600) else GymBgCard,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when(shift) {
                                TimeShift.SHIFT_1    -> Icons.Default.WbSunny
                                TimeShift.SHIFT_2    -> Icons.Default.WbCloudy
                                TimeShift.SHIFT_3    -> Icons.Default.Bedtime
                                TimeShift.SHIFT_4    -> Icons.Default.NightsStay
                                TimeShift.CUSTOM     -> Icons.Default.AccessTime
                            }
                            Icon(icon, null, tint = if (isSelected) GymYellow else TextSecondary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(shift.label, style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) TextPrimary else TextSecondary, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(GymYellow),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, null, tint = GymBgDark, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = GymBgBorder)

            SectionLabel("FEE & SUBSCRIPTION")

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { showPlanSheet = true },
                color = GymBgCard, shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardMembership, null, tint = GymYellow, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(selectedPlan?.name ?: "Select Plan (Optional)",
                        color = if (selectedPlan != null) TextPrimary else TextMuted, modifier = Modifier.weight(1f))
                }
            }

            GymTextField(value = customFee, onValueChange = { customFee = it },
                label = "Total Fee Amount (PKR)", placeholder = "e.g. 3000",
                leadingIcon = Icons.Default.AttachMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    member?.let { m ->
                        val updated = m.copy(
                            name = name, phone = phone, address = address,
                            age = ageStr.toIntOrNull() ?: 0, weight = weight, height = height,
                            goal = goal, photoUri = photoUri, timeShift = selectedShift,
                            customShiftTime = if (selectedShift == TimeShift.CUSTOM) customTime else null,
                            planId = selectedPlan?.id, planName = selectedPlan?.name ?: m.planName,
                            totalFee = totalFee, amountDue = (totalFee - m.amountPaid).coerceAtLeast(0.0),
                            notes = notes
                        )
                        vm.updateMember(updated)
                        onBack()
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymYellow)
            ) {
                Icon(Icons.Default.Save, null, tint = GymBgDark)
                Spacer(Modifier.width(8.dp))
                Text("Save Changes", color = GymBgDark, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showPlanSheet) {
        ModalBottomSheet(onDismissRequest = { showPlanSheet = false }, containerColor = GymBgCard) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Select Plan", style = MaterialTheme.typography.titleLarge, color = GymYellow, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                plans.forEach { plan ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(16.dp)).clickable { selectedPlan = plan; showPlanSheet = false },
                        color = if (selectedPlan?.id == plan.id) Color(0x33FFD600) else GymBgElevated,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Rs. ${"%,.0f".format(plan.price)}", color = GymYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = GymYellow,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
}
