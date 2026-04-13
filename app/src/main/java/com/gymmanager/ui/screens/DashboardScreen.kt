package com.gymmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymmanager.data.model.GymInfo
import com.gymmanager.ui.Screen
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

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
}
