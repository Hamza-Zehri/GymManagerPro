package com.gymmanager.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.model.*
import com.gymmanager.data.repository.GymRepository
import com.gymmanager.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GymDatabase.getInstance(application)
    val repo = GymRepository(
        db.gymInfoDao(), db.subscriptionPlanDao(), db.memberDao(),
        db.attendanceDao(), db.paymentDao(), db.expenseDao(), db.backupLogDao()
    )

    // ── GYM INFO ──────────────────────────────
    val gymInfo: StateFlow<GymInfo?> = repo.getGymInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveGymInfo(info: GymInfo) = viewModelScope.launch { repo.saveGymInfo(info) }

    // ── PLANS ─────────────────────────────────
    val plans: StateFlow<List<SubscriptionPlan>> = repo.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPlan(plan: SubscriptionPlan) = viewModelScope.launch { repo.insertPlan(plan) }
    fun updatePlan(plan: SubscriptionPlan) = viewModelScope.launch { repo.updatePlan(plan) }
    fun deletePlan(id: Long) = viewModelScope.launch { repo.deletePlan(id) }

    // ── MEMBERS ───────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val members: StateFlow<List<Member>> = _searchQuery
        .flatMapLatest { q ->
            if (q.isBlank()) repo.getAllMembers()
            else repo.searchMembers(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMembers: StateFlow<Int> = repo.getTotalMemberCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val paidMembers: StateFlow<Int> = repo.getActivePaidCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val pendingMembers: StateFlow<Int> = repo.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalDue: StateFlow<Double> = repo.getTotalDue()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMemberId
        .flatMapLatest { id -> if (id != null) repo.getMemberById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectMember(id: Long) { _selectedMemberId.value = id }

    fun addMember(
        name: String,
        phone: String,
        address: String,
        gender: String,
        age: Int,
        weight: String,
        height: String,
        goal: String,
        photoUri: String?,
        timeShift: TimeShift,
        customShiftTime: String?,
        planId: Long?,
        totalFee: Double
    ) = viewModelScope.launch {
        val planIdSafe = planId
        val plan = if (planIdSafe != null) db.subscriptionPlanDao().getPlanById(planIdSafe) else null
        val now = System.currentTimeMillis()
        val endDate = if (plan != null) now + (plan.durationDays.toLong() * 86_400_000L) else null

        val member = Member(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            gender = gender,
            age = age,
            weight = weight,
            height = height,
            goal = goal,
            photoUri = photoUri,
            status = MemberStatus.UNPAID,
            planId = planId,
            planName = plan?.name,
            subscriptionStart = if (plan != null) now else null,
            subscriptionEnd = endDate,
            totalFee = totalFee,
            amountDue = totalFee,
            timeShift = timeShift,
            customShiftTime = customShiftTime
        )
        repo.insertMember(member)
    }

    fun updateMemberPhoto(id: Long, uri: String) = viewModelScope.launch {
        repo.getMemberByIdOnce(id)?.let { repo.updateMember(it.copy(photoUri = uri)) }
    }

    fun updateMember(member: Member) = viewModelScope.launch {
        repo.updateMember(member)
    }

    fun deleteMember(id: Long) = viewModelScope.launch {
        repo.deleteMember(id)
        if (_selectedMemberId.value == id) _selectedMemberId.value = null
    }

    // ── PAYMENTS ──────────────────────────────
    val payments: StateFlow<List<Payment>> = repo.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMemberPayments(memberId: Long): Flow<List<Payment>> = repo.getMemberPayments(memberId)

    fun recordPayment(memberId: Long, amount: Double, method: String, note: String) =
        viewModelScope.launch {
            val member = repo.getMemberByIdOnce(memberId) ?: return@launch
            val newPaid = member.amountPaid + amount
            val newDue = (member.totalFee - newPaid).coerceAtLeast(0.0)
            val newStatus = when {
                newDue <= 0.0 -> MemberStatus.PAID
                newPaid <= 0.0 -> MemberStatus.UNPAID
                else -> MemberStatus.PARTIAL
            }
            repo.updateMember(member.copy(amountPaid = newPaid, amountDue = newDue, status = newStatus))
            repo.insertPayment(Payment(memberId = memberId, memberName = member.name, amount = amount, method = method, note = note))
        }

    fun deletePayment(paymentId: Long) = viewModelScope.launch { repo.deletePayment(paymentId) }

    // ── ATTENDANCE ────────────────────────────
    private val _attendanceDate = MutableStateFlow(DateUtils.todayString())
    val attendanceDate: StateFlow<String> = _attendanceDate.asStateFlow()

    val attendanceForDate: StateFlow<List<Attendance>> = _attendanceDate
        .flatMapLatest { repo.getAttendanceByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAttendanceDate(date: String) { _attendanceDate.value = date }

    fun toggleAttendance(member: Member, date: String) = viewModelScope.launch {
        val existing = repo.getAttendanceRecord(member.id, date)
        if (existing != null) {
            repo.deleteAttendance(existing.id)
        } else {
            repo.insertAttendance(
                Attendance(
                    memberId = member.id,
                    memberName = member.name,
                    date = date,
                    timeShift = member.timeShift
                )
            )
            repo.updateMember(member.copy(lastAttendance = System.currentTimeMillis()))
        }
    }

    fun getMemberAttendance(memberId: Long): Flow<List<Attendance>> = repo.getMemberAttendance(memberId)

    // ── EXPENSES ──────────────────────────────
    val expenses: StateFlow<List<Expense>> = repo.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(description: String, amount: Double, category: String) = viewModelScope.launch {
        repo.insertExpense(Expense(description = description, amount = amount, category = category))
    }

    fun deleteExpense(id: Long) = viewModelScope.launch { repo.deleteExpense(id) }

    // ── BACKUP LOGS ───────────────────────────
    val backupHistory: StateFlow<List<BackupLog>> = repo.getBackupHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── FACTORY ───────────────────────────────
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GymViewModel(application) as T
        }
    }
}
