package com.hamzaasad.gymmanagerpro.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.hamzaasad.gymmanagerpro.data.db.GymDatabase
import com.hamzaasad.gymmanagerpro.data.model.*
import com.hamzaasad.gymmanagerpro.data.repository.GymRepository
import com.hamzaasad.gymmanagerpro.sync.SyncManager
import com.hamzaasad.gymmanagerpro.sync.SyncResult
import com.hamzaasad.gymmanagerpro.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GymDatabase.getInstance(application)
    val repo = GymRepository(
        db.gymInfoDao(), db.subscriptionPlanDao(), db.memberDao(),
        db.attendanceDao(), db.paymentDao(), db.expenseDao(), db.backupLogDao()
    )

    private val syncManager = SyncManager(application, db)
    val lastSyncTime: StateFlow<Long> = syncManager.lastSyncTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // ── GYM INFO ──────────────────────────────
    val gymInfo: StateFlow<GymInfo?> = repo.getGymInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveGymInfo(info: GymInfo) = viewModelScope.launch { repo.saveGymInfo(info) }

    // ── PLANS ─────────────────────────────────
    val plans: StateFlow<List<SubscriptionPlan>> = repo.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPlan(plan: SubscriptionPlan) = viewModelScope.launch { repo.insertPlan(plan) }
    fun updatePlan(plan: SubscriptionPlan) = viewModelScope.launch { repo.updatePlan(plan) }
    fun deletePlan(id: String) = viewModelScope.launch { repo.deletePlan(id) }

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

    val expiringMembers: StateFlow<List<Member>> = repo.getAllMembers()
        .map { list ->
            val now = System.currentTimeMillis()
            val fiveDaysFromNow = now + (5 * 24 * 60 * 60 * 1000L)
            list.filter { 
                val end = it.subscriptionEnd ?: 0L
                end <= fiveDaysFromNow && end > (now - 86400000L) // Include today
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private val _selectedMemberId = MutableStateFlow<String?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMemberId
        .flatMapLatest { id -> if (id != null) repo.getMemberById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectMember(id: String) { _selectedMemberId.value = id }

    val blockedMembers: StateFlow<List<Member>> = repo.getBlockedMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun checkCnicExists(cnic: String): Member? {
        return repo.getMemberByCnic(cnic.trim())
    }

    fun addMember(
        name: String,
        phone: String,
        cnic: String,
        address: String,
        gender: String,
        photoUri: String?,
        timeShift: TimeShift,
        customShiftTime: String?,
        planId: String?,
        totalFee: Double
    ) = viewModelScope.launch {
        val plan = if (planId != null) db.subscriptionPlanDao().getPlanById(planId) else null
        val now = System.currentTimeMillis()
        
        val endDate = if (plan != null) {
            if (plan.durationDays == 30) DateUtils.addMonths(now, 1)
            else DateUtils.addDays(now, plan.durationDays)
        } else null

        val deviceId = getDeviceId(getApplication())
        val memberId = java.util.UUID.randomUUID().toString()

        // Handle profile photo saving to internal storage
        var localPhotoPath: String? = null
        var imageHash: String? = null
        if (photoUri != null) {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(photoUri)
            localPhotoPath = com.hamzaasad.gymmanagerpro.utils.FileUtils.saveImageToInternalStorage(context, uri, memberId)
            if (localPhotoPath != null) {
                imageHash = com.hamzaasad.gymmanagerpro.utils.FileUtils.getImageHash(localPhotoPath)
            }
        }

        val member = Member(
            id = memberId,
            name = name.trim(),
            phone = phone.trim(),
            cnic = cnic.trim(),
            address = address.trim(),
            gender = gender,
            photoUri = localPhotoPath,
            status = MemberStatus.UNPAID,
            planId = planId,
            planName = plan?.name,
            subscriptionStart = if (plan != null) now else null,
            subscriptionEnd = endDate,
            totalFee = totalFee,
            amountDue = totalFee,
            timeShift = timeShift,
            customShiftTime = customShiftTime,
            deviceId = deviceId,
            updatedAt = now,
            imageHash = imageHash
        )
        repo.insertMember(member)
    }

    fun updateMemberPhoto(id: String, uri: String) = viewModelScope.launch {
        val context = getApplication<Application>().applicationContext
        val localPhotoPath = com.hamzaasad.gymmanagerpro.utils.FileUtils.saveImageToInternalStorage(context, Uri.parse(uri), id)
        if (localPhotoPath != null) {
            val imageHash = com.hamzaasad.gymmanagerpro.utils.FileUtils.getImageHash(localPhotoPath)
            repo.getMemberByIdOnce(id)?.let { 
                repo.updateMember(it.copy(photoUri = localPhotoPath, imageHash = imageHash, updatedAt = System.currentTimeMillis())) 
            }
        }
    }

    fun updateMember(member: Member) = viewModelScope.launch {
        var updatedMember = member
        val context = getApplication<Application>().applicationContext

        // If photoUri is a content/file URI (from picker) and not already in internal storage
        if (member.photoUri != null && (member.photoUri.startsWith("content://") || member.photoUri.startsWith("file://"))) {
            val localPath = com.hamzaasad.gymmanagerpro.utils.FileUtils.saveImageToInternalStorage(context, Uri.parse(member.photoUri), member.id)
            if (localPath != null) {
                val imageHash = com.hamzaasad.gymmanagerpro.utils.FileUtils.getImageHash(localPath)
                updatedMember = member.copy(photoUri = localPath, imageHash = imageHash)
            }
        }

        val finalMember = updatedMember.copy(
            updatedAt = System.currentTimeMillis(),
            deviceId = getDeviceId(getApplication())
        )
        repo.updateMember(finalMember)
    }

    fun toggleBlockMember(id: String, block: Boolean) = viewModelScope.launch {
        repo.getMemberByIdOnce(id)?.let { 
            repo.updateMember(it.copy(
                isBlocked = block,
                updatedAt = System.currentTimeMillis(),
                deviceId = getDeviceId(getApplication())
            )) 
        }
    }

    fun deleteMember(id: String) = viewModelScope.launch {
        repo.deleteMember(id)
        if (_selectedMemberId.value == id) _selectedMemberId.value = null
    }

    // ── PAYMENTS ──────────────────────────────
    val payments: StateFlow<List<Payment>> = repo.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMemberPayments(memberId: String): Flow<List<Payment>> = repo.getMemberPayments(memberId)

    fun recordPayment(memberId: String, amount: Double, method: String, note: String) =
        viewModelScope.launch {
            val member = repo.getMemberByIdOnce(memberId) ?: return@launch
            val now = System.currentTimeMillis()
            val deviceId = getDeviceId(getApplication())
            
            val newPaid = member.amountPaid + amount
            val newDue = (member.totalFee - newPaid).coerceAtLeast(0.0)
            val newStatus = when {
                newDue <= 0.0 -> MemberStatus.PAID
                newPaid <= 0.0 -> MemberStatus.UNPAID
                else -> MemberStatus.PARTIAL
            }
            repo.updateMember(member.copy(
                amountPaid = newPaid, 
                amountDue = newDue, 
                status = newStatus,
                updatedAt = now,
                deviceId = deviceId
            ))
            repo.insertPayment(Payment(
                memberId = memberId, 
                memberName = member.name, 
                amount = amount, 
                paymentDate = now,
                method = method, 
                note = note,
                updatedAt = now,
                deviceId = deviceId
            ))
        }

    fun resubscribeMember(memberId: String, planId: String, totalFee: Double) = viewModelScope.launch {
        val member = repo.getMemberByIdOnce(memberId) ?: return@launch
        val plan = db.subscriptionPlanDao().getPlanById(planId) ?: return@launch
        val now = System.currentTimeMillis()
        val deviceId = getDeviceId(getApplication())

        // Calculate new end date: if exactly 30 days, add 1 calendar month to keep same day of month
        val newEnd = if (plan.durationDays == 30) DateUtils.addMonths(now, 1)
                    else DateUtils.addDays(now, plan.durationDays)

        // Add the new fee to the existing totals to track cumulative debt
        val updatedTotalFee = member.totalFee + totalFee
        val updatedAmountDue = member.amountDue + totalFee

        repo.updateMember(member.copy(
            planId = planId,
            planName = plan.name,
            subscriptionStart = now,
            subscriptionEnd = newEnd,
            totalFee = updatedTotalFee,
            amountDue = updatedAmountDue,
            status = if (updatedAmountDue > 0) MemberStatus.UNPAID else MemberStatus.PAID,
            updatedAt = now,
            deviceId = deviceId
        ))
    }

    fun deletePayment(paymentId: String) = viewModelScope.launch { repo.deletePayment(paymentId) }

    // ── ATTENDANCE ────────────────────────────
    private val _attendanceDate = MutableStateFlow(DateUtils.todayString())
    val attendanceDate: StateFlow<String> = _attendanceDate.asStateFlow()

    val attendanceForDate: StateFlow<List<Attendance>> = _attendanceDate
        .flatMapLatest { repo.getAttendanceByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAttendanceDate(date: String) { _attendanceDate.value = date }

    fun toggleAttendance(member: Member, date: String) = viewModelScope.launch {
        val existing = repo.getAttendanceRecord(member.id, date)
        val now = System.currentTimeMillis()
        val deviceId = getDeviceId(getApplication())
        
        if (existing != null) {
            repo.deleteAttendance(existing.id)
        } else {
            repo.insertAttendance(
                Attendance(
                    memberId = member.id,
                    memberName = member.name,
                    date = date,
                    timeShift = member.timeShift,
                    updatedAt = now,
                    deviceId = deviceId
                )
            )
            repo.updateMember(member.copy(
                lastAttendance = now,
                updatedAt = now,
                deviceId = deviceId
            ))
        }
    }

    fun getMemberAttendance(memberId: String): Flow<List<Attendance>> = repo.getMemberAttendance(memberId)

    // ── EXPENSES ──────────────────────────────
    val expenses: StateFlow<List<Expense>> = repo.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(description: String, amount: Double, category: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.insertExpense(Expense(
            description = description, 
            amount = amount, 
            category = category,
            updatedAt = now,
            deviceId = getDeviceId(getApplication())
        ))
    }

    fun deleteExpense(id: String) = viewModelScope.launch { repo.deleteExpense(id) }

    // ── BACKUP LOGS ───────────────────────────
    val backupHistory: StateFlow<List<BackupLog>> = repo.getBackupHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── SYNC ──────────────────────────────────
    fun startSyncServer() = syncManager.startServer()
    fun stopSyncServer() = syncManager.stopServer()

    private fun getDeviceId(context: android.content.Context): String {
        return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }
    
    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult = _syncResult.asStateFlow()

    fun syncWithServer(ip: String) = viewModelScope.launch {
        _syncResult.value = null
        _syncResult.value = syncManager.syncWithServer(ip)
    }

    // ── FACTORY ───────────────────────────────
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GymViewModel(application) as T
        }
    }
}
