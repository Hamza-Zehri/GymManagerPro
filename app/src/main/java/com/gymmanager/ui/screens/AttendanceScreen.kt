package com.gymmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymmanager.data.model.Member
import com.gymmanager.data.model.TimeShift
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.utils.DateUtils
import com.gymmanager.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    vm: GymViewModel,
    onBack: () -> Unit
) {
    val members        by vm.members.collectAsState()
    val attendanceDate by vm.attendanceDate.collectAsState()
    val attendanceList by vm.attendanceForDate.collectAsState()
    var selectedShift  by remember { mutableStateOf<TimeShift?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val presentIds = attendanceList.map { it.memberId }.toSet()

    // Filter members by shift
    val filteredMembers = members.filter { m ->
        selectedShift == null || m.timeShift == selectedShift
    }
    val presentCount = filteredMembers.count { it.id in presentIds }
    val absentCount  = filteredMembers.count { it.id !in presentIds }

    val displayDate = remember(attendanceDate) {
        SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault())
            .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(attendanceDate)!!)
    }

    Column(Modifier.fillMaxSize().background(Zinc950)) {
        GymTopBar(
            title = "Attendance",
            subtitle = displayDate,
            onBack = onBack
        ) {
            SquareIconButton(Icons.Default.DateRange, onClick = { showDatePicker = true })
        }

        // ── SHIFT FILTER ───────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                ShiftChip("All Shifts", selectedShift == null) { selectedShift = null }
            }
            items(TimeShift.entries) { shift ->
                val label = when(shift) {
                    TimeShift.SHIFT_1   -> "☀️ 4 PM – 6 PM"
                    TimeShift.SHIFT_2   -> "🌤 6 PM – 8 PM"
                    TimeShift.SHIFT_3   -> "🌇 8 PM – 10 PM"
                    TimeShift.SHIFT_4   -> "🌙 10 PM – 12 AM"
                    TimeShift.CUSTOM    -> "🕐 Custom"
                }
                ShiftChip(label, selectedShift == shift) { selectedShift = shift }
            }
        }

        // ── STATS ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color(0x1A10B981),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("$presentCount", style = MaterialTheme.typography.headlineMedium, color = Emerald400)
                    Text("Present", style = MaterialTheme.typography.labelSmall, color = Emerald400.copy(alpha = 0.7f))
                }
            }
            Surface(
                color = Color(0x1AF43F5E),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("$absentCount", style = MaterialTheme.typography.headlineMedium, color = Rose400)
                    Text("Absent", style = MaterialTheme.typography.labelSmall, color = Rose400.copy(alpha = 0.7f))
                }
            }
            Surface(
                color = Zinc900,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${filteredMembers.size}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                }
            }
        }

        // ── MEMBER LIST ────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMembers, key = { it.id }) { member ->
                val isPresent = member.id in presentIds
                AttendanceMemberRow(
                    member = member,
                    isPresent = isPresent,
                    onToggle = { vm.toggleAttendance(member, attendanceDate) }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── DATE PICKER ────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.millisFromDateStr(attendanceDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        vm.setAttendanceDate(fmt.format(Date(millis)))
                    }
                    showDatePicker = false
                }) { Text("OK", color = Emerald500) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Zinc400)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Zinc900,
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Zinc400,
                subheadContentColor = Zinc400,
                dayContentColor = Color.White,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Emerald500,
                todayDateBorderColor = Emerald500,
                todayContentColor = Emerald400
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─────────────────────────────────────────────
//  ATTENDANCE ROW CARD
// ─────────────────────────────────────────────
@Composable
fun AttendanceMemberRow(
    member: Member,
    isPresent: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = if (isPresent) Color(0x0D10B981) else Zinc900,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(photoUri = member.photoUri, name = member.name, size = 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                val shiftLabel = when (member.timeShift) {
                    TimeShift.SHIFT_1   -> "☀️ 4 PM – 6 PM"
                    TimeShift.SHIFT_2   -> "🌤 6 PM – 8 PM"
                    TimeShift.SHIFT_3   -> "🌇 8 PM – 10 PM"
                    TimeShift.SHIFT_4   -> "🌙 10 PM – 12 AM"
                    TimeShift.CUSTOM    -> "🕐 ${member.customShiftTime ?: "Custom"}"
                }
                Text(shiftLabel, style = MaterialTheme.typography.bodySmall, color = Zinc400)
            }
            // Toggle button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPresent) Emerald500 else Zinc800)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (isPresent) "Present" else "Absent",
                    tint = if (isPresent) Color.White else Zinc600,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
