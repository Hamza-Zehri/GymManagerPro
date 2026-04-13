package com.gymmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.gymmanager.data.model.MemberStatus
import com.gymmanager.data.model.TimeShift
import com.gymmanager.ui.Screen
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.utils.DateUtils
import com.gymmanager.viewmodel.GymViewModel

@Composable
fun MembersListScreen(
    vm: GymViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val members by vm.members.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    var filterStatus by remember { mutableStateOf<MemberStatus?>(null) }
    var filterShift by remember { mutableStateOf<TimeShift?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val filtered = members.filter { m ->
        (filterStatus == null || m.status == filterStatus) &&
        (filterShift  == null || m.timeShift == filterShift)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Zinc950)
    ) {
        GymTopBar(
            title = "Members",
            subtitle = "${filtered.size} members",
            onBack = onBack
        ) {
            SquareIconButton(Icons.Default.FilterList, onClick = { showFilters = !showFilters })
        }

        // ── SEARCH BAR ──────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { vm.setSearchQuery(it) },
            placeholder = { Text("Search name or phone…", color = Zinc600) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Zinc400) },
            trailingIcon = if (searchQuery.isNotEmpty()) ({
                IconButton(onClick = { vm.setSearchQuery("") }) {
                    Icon(Icons.Default.Clear, null, tint = Zinc400)
                }
            }) else null,
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Zinc900,
                unfocusedContainerColor = Zinc900,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Zinc800,
                cursorColor = Emerald500
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        )

        // ── FILTER CHIPS ────────────────────────
        if (showFilters) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Status", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "All", MemberStatus.PAID to "Paid",
                        MemberStatus.UNPAID to "Unpaid", MemberStatus.PARTIAL to "Partial").forEach { (s, label) ->
                        FilterChip(
                            selected = filterStatus == s,
                            onClick = { filterStatus = s },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500,
                                selectedLabelColor = Color.White,
                                containerColor = Zinc800,
                                labelColor = Zinc400
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Shift", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "All", TimeShift.SHIFT_1 to "4-6 PM",
                        TimeShift.SHIFT_2 to "6-8 PM", TimeShift.SHIFT_3 to "8-10 PM",
                        TimeShift.SHIFT_4 to "10-12 AM").forEach { (s, label) ->
                        FilterChip(
                            selected = filterShift == s,
                            onClick = { filterShift = s },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue500,
                                selectedLabelColor = Color.White,
                                containerColor = Zinc800,
                                labelColor = Zinc400
                            )
                        )
                    }
                }
            }
        }

        // ── LIST ────────────────────────────────
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PeopleOutline, null, tint = Zinc600, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No members found", color = Zinc400, style = MaterialTheme.typography.titleMedium)
                    Text("Add your first member", color = Zinc600, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { member ->
                    MemberListCard(
                        member = member,
                        onClick = {
                            vm.selectMember(member.id)
                            onNavigate(Screen.MemberProfile.createRoute(member.id))
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // FAB
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        ExtendedFloatingActionButton(
            onClick = { onNavigate(Screen.AddMember.route) },
            modifier = Modifier.padding(20.dp),
            containerColor = Emerald500,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.PersonAdd, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Member")
        }
    }
}

// ─────────────────────────────────────────────
//  MEMBER CARD ITEM
// ─────────────────────────────────────────────
@Composable
fun MemberListCard(member: Member, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = Zinc900,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(photoUri = member.photoUri, name = member.name, size = 56)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(member.phone, style = MaterialTheme.typography.bodySmall, color = Zinc400)
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift badge
                    val shiftLabel = when (member.timeShift) {
                        TimeShift.SHIFT_1    -> "☀️ 4 PM – 6 PM"
                        TimeShift.SHIFT_2    -> "🌤 6 PM – 8 PM"
                        TimeShift.SHIFT_3    -> "🌇 8 PM – 10 PM"
                        TimeShift.SHIFT_4    -> "🌙 10 PM – 12 AM"
                        TimeShift.CUSTOM    -> "🕐 ${member.customShiftTime ?: "Custom"}"
                    }
                    Surface(color = Zinc800, shape = RoundedCornerShape(6.dp)) {
                        Text(shiftLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = Zinc300)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(member.status)
                Spacer(Modifier.height(6.dp))
                if (member.amountDue > 0) {
                    Text("Rs. ${"%,.0f".format(member.amountDue)}", style = MaterialTheme.typography.labelSmall, color = Rose400)
                } else if (member.subscriptionEnd != null) {
                    val days = DateUtils.daysUntil(member.subscriptionEnd)
                    Text("${days}d left", style = MaterialTheme.typography.labelSmall,
                        color = if (days < 7) Amber500 else Emerald400)
                }
            }
        }
    }
}

