package com.gymmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymmanager.data.model.*
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.utils.DateUtils
import com.gymmanager.viewmodel.GymViewModel

// ─────────────────────────────────────────────
//  FEE MANAGEMENT
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeManagementScreen(
    vm: GymViewModel,
    onMemberClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val members by vm.members.collectAsState()
    val totalDue by vm.totalDue.collectAsState()
    var filterStatus by remember { mutableStateOf<MemberStatus?>(null) }

    val filtered = members.filter { filterStatus == null || it.status == filterStatus }

    Column(Modifier.fillMaxSize().background(Zinc950)) {
        GymTopBar(title = "Fee Management", subtitle = "Track payments", onBack = onBack)

        // Summary
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Total Due", "Rs. ${"%,.0f".format(totalDue)}", Rose400, Modifier.weight(1f))
            StatCard("Paid Members", "${members.count { it.status == MemberStatus.PAID }}", Emerald400, Modifier.weight(1f))
        }

        // Filter chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(null to "All", MemberStatus.UNPAID to "Unpaid",
                MemberStatus.PARTIAL to "Partial", MemberStatus.PAID to "Paid").forEach { (s, label) ->
                FilterChip(
                    selected = filterStatus == s,
                    onClick = { filterStatus = s },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500, selectedLabelColor = Color.White,
                        containerColor = Zinc800, labelColor = Zinc400
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { member ->
                Surface(
                    color = Zinc900,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onMemberClick(member.id) }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        MemberAvatar(member.photoUri, member.name, size = 48)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(member.phone, style = MaterialTheme.typography.bodySmall, color = Zinc400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(member.status)
                            Spacer(Modifier.height(4.dp))
                            if (member.amountDue > 0) {
                                Text("Due: Rs. ${"%,.0f".format(member.amountDue)}",
                                    style = MaterialTheme.typography.labelSmall, color = Rose400)
                            } else {
                                Text("Paid ✓", style = MaterialTheme.typography.labelSmall, color = Emerald400)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─────────────────────────────────────────────
//  EXPENSES
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(vm: GymViewModel, onBack: () -> Unit) {
    val expenses by vm.expenses.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteId     by remember { mutableStateOf<String?>(null) }


    // Add form state
    var desc     by remember { mutableStateOf("") }
    var amount   by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    val totalExpenses = expenses.sumOf { it.amount }
    val categories = listOf("General", "Electricity", "Equipment", "Rent", "Salary", "Maintenance", "Other")

    Column(Modifier.fillMaxSize().background(Zinc950)) {
        GymTopBar(title = "Expenses", subtitle = "Track gym costs", onBack = onBack) {
            SquareIconButton(Icons.Default.Add, onClick = { showAddSheet = true }, tint = Emerald400)
        }

        // Total card
        Surface(
            color = Color(0x1AF43F5E),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingDown, null, tint = Rose400, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Total Expenses", style = MaterialTheme.typography.labelMedium, color = Rose400)
                    Text("Rs. ${"%,.0f".format(totalExpenses)}", style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Zinc600, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No expenses recorded", color = Zinc400)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    Surface(
                        color = Zinc900,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1AF43F5E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, null, tint = Rose400, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(expense.description, color = Color.White, style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(expense.category, color = Zinc400, style = MaterialTheme.typography.bodySmall)
                                    Text("•", color = Zinc600)
                                    Text(DateUtils.displayDate(expense.date), color = Zinc400, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${"%,.0f".format(expense.amount)}", color = Rose400, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Icon(
                                    Icons.Default.DeleteOutline, null,
                                    tint = Zinc600, modifier = Modifier.size(18.dp).clickable { deleteId = expense.id }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }


    // Add expense sheet
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = Zinc900) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add Expense", style = MaterialTheme.typography.titleLarge, color = Color.White)

                GymTextField(value = desc, onValueChange = { desc = it },
                    label = "Description *", placeholder = "e.g. Electricity Bill",
                    leadingIcon = Icons.Default.Description)

                GymTextField(value = amount, onValueChange = { amount = it },
                    label = "Amount (PKR) *", placeholder = "0",
                    leadingIcon = Icons.Default.AttachMoney,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

                Column {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = Zinc400,
                        modifier = Modifier.padding(bottom = 6.dp))
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(categories.size) { i ->
                            val cat = categories[i]
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Rose500, selectedLabelColor = Color.White,
                                    containerColor = Zinc800, labelColor = Zinc400
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                PrimaryButton(
                    text = "Add Expense",
                    enabled = desc.isNotBlank() && amount.toDoubleOrNull() != null,
                    onClick = {
                        vm.addExpense(desc.trim(), amount.toDouble(), category)
                        showAddSheet = false; desc = ""; amount = ""; category = "General"
                    },
                    icon = Icons.Default.Check,
                    color = Rose500
                )
            }
        }
    }

    // Delete confirm
    deleteId?.let { id ->
        DeleteConfirmDialog(
            title = "Delete Expense",
            message = "Remove this expense record?",
            onConfirm = { vm.deleteExpense(id); deleteId = null },
            onDismiss = { deleteId = null }
        )
    }
}

// ─────────────────────────────────────────────
//  SUBSCRIPTION PLANS
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(vm: GymViewModel, onBack: () -> Unit) {
    val plans by vm.plans.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteId     by remember { mutableStateOf<String?>(null) }


    // Form
    var planName  by remember { mutableStateOf("") }
    var duration  by remember { mutableStateOf("") }
    var price     by remember { mutableStateOf("") }
    var planDesc  by remember { mutableStateOf("") }


    Column(Modifier.fillMaxSize().background(Zinc950)) {
        GymTopBar(title = "Subscription Plans", onBack = onBack) {
            SquareIconButton(Icons.Default.Add, onClick = { showAddSheet = true }, tint = Emerald400)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(plans, key = { it.id }) { plan ->
                Surface(color = Zinc900, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1A10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CardMembership, null, tint = Emerald400)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(plan.name, color = Color.White, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text("${plan.durationDays} days", color = Zinc400, style = MaterialTheme.typography.bodySmall)
                                if (plan.description.isNotBlank())
                                    Text(plan.description, color = Zinc500, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${"%,.0f".format(plan.price)}", color = Emerald400,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Icon(Icons.Default.DeleteOutline, null, tint = Zinc600,
                                    modifier = Modifier.size(18.dp).clickable { deleteId = plan.id })
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = Zinc900) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add Plan", style = MaterialTheme.typography.titleLarge, color = Color.White)
                GymTextField(value = planName, onValueChange = { planName = it },
                    label = "Plan Name *", placeholder = "e.g. Monthly", leadingIcon = Icons.Default.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymTextField(value = duration, onValueChange = { duration = it },
                        label = "Duration (days) *", placeholder = "30",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    GymTextField(value = price, onValueChange = { price = it },
                        label = "Price (PKR) *", placeholder = "3000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                GymTextField(value = planDesc, onValueChange = { planDesc = it },
                    label = "Description (Optional)", placeholder = "e.g. Best value for monthly")
                PrimaryButton(
                    text = "Add Plan",
                    enabled = planName.isNotBlank() && duration.toIntOrNull() != null && price.toDoubleOrNull() != null,
                    onClick = {
                        val now = System.currentTimeMillis()
                        vm.addPlan(SubscriptionPlan(
                            name = planName.trim(), 
                            durationDays = duration.toInt(),
                            price = price.toDouble(), 
                            description = planDesc.trim(),
                            updatedAt = now,
                            deviceId = "local" // Ideally from VM, but for now
                        ))
                        showAddSheet = false; planName = ""; duration = ""; price = ""; planDesc = ""
                    },
                    icon = Icons.Default.Check
                )
            }
        }
    }

    deleteId?.let { id ->
        DeleteConfirmDialog("Delete Plan", "This will remove the plan but not affect existing members.",
            onConfirm = { vm.deletePlan(id); deleteId = null },
            onDismiss = { deleteId = null })
    }
}


