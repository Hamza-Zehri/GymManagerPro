package com.gymmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymmanager.data.model.GymInfo
import com.gymmanager.data.model.Member
import com.gymmanager.ui.Screen
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.viewmodel.GymViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: GymViewModel,
    onNavigate: (String) -> Unit
) {
    val gymInfo by vm.gymInfo.collectAsState()
    val totalMembers by vm.totalMembers.collectAsState()
    val paidMembers by vm.paidMembers.collectAsState()
    val pendingMembers by vm.pendingMembers.collectAsState()
    val totalDue by vm.totalDue.collectAsState()
    val expiringMembers by vm.expiringMembers.collectAsState()
    var showExpiringSheet by remember { mutableStateOf(false) }

    val greeting = remember {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            h < 12 -> "Good Morning"
            h < 17 -> "Good Afternoon"
            else   -> "Good Evening"
        }
    }
    val dateStr = remember {
        SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .verticalScroll(rememberScrollState())
    ) {
        // ── HEADER ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(gymInfo?.gymName ?: "Gym Manager Pro",
                    style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(greeting + " 👋", style = MaterialTheme.typography.bodySmall, color = Zinc400)
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Zinc600)
            }
            if (expiringMembers.isNotEmpty()) {
                Box {
                    SquareIconButton(Icons.Default.NotificationsActive, onClick = { showExpiringSheet = true }, tint = Amber500)
                    Surface(
                        Modifier.align(Alignment.TopEnd).padding(4.dp),
                        shape = CircleShape, color = Rose500
                    ) {
                        Text(expiringMembers.size.toString(), modifier = Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            SquareIconButton(Icons.Default.Settings, onClick = { onNavigate(Screen.Settings.route) })
        }

        // ── STATS ROW ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Total", totalMembers.toString(), Color.White, Modifier.weight(1f))
            StatCard("Active", paidMembers.toString(), Emerald400, Modifier.weight(1f))
            StatCard("Pending", pendingMembers.toString(), Rose400, Modifier.weight(1f))
        }

        // ── DUE AMOUNT BANNER ──────────────────
        if (totalDue > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = Color(0x1AF43F5E),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = Rose400, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Total Outstanding", style = MaterialTheme.typography.labelMedium, color = Rose400)
                        Text("Rs. ${"%,.0f".format(totalDue)}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── ACTION GRID ────────────────────────
        val actions = listOf(
            Triple("Add Member",   Icons.Default.PersonAdd,      Emerald500  ) to Screen.AddMember.route,
            Triple("Members",      Icons.Default.People,         Blue500     ) to Screen.MembersList.route,
            Triple("Attendance",   Icons.Default.CheckCircle,    Purple500   ) to Screen.Attendance.route,
            Triple("Fees",         Icons.Default.AttachMoney,    Amber500    ) to Screen.FeeManagement.route,
            Triple("Expenses",     Icons.Default.Receipt,        Rose500     ) to Screen.Expenses.route,
            Triple("Backup",       Icons.Default.CloudUpload,    Cyan500     ) to Screen.BackupRestore.route,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (triple, route) ->
                        ActionCard(
                            label   = triple.first,
                            icon    = triple.second,
                            iconBg  = triple.third,
                            onClick = { onNavigate(route) },
                            modifier = Modifier.weight(1f).height(140.dp)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showExpiringSheet) {
        ModalBottomSheet(onDismissRequest = { showExpiringSheet = false }, containerColor = Zinc900) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Expiring Soon", style = MaterialTheme.typography.titleLarge, color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp))
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                    items(expiringMembers) { member ->
                        Surface(
                            color = Zinc800,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showExpiringSheet = false
                                onNavigate(Screen.MemberProfile.createRoute(member.id))
                            }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).background(Zinc700, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = Zinc400)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(member.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Ends: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(member.subscriptionEnd ?: 0L))}",
                                        color = Zinc400, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Zinc500)
                            }
                        }
                    }
                }
            }
        }
    }
}
