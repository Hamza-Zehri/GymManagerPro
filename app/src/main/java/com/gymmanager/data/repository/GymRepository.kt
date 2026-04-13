package com.gymmanager.data.repository

import com.gymmanager.data.db.*
import com.gymmanager.data.model.*
import kotlinx.coroutines.flow.Flow

class GymRepository(
    private val gymInfoDao: GymInfoDao,
    private val subscriptionPlanDao: SubscriptionPlanDao,
    private val memberDao: MemberDao,
    private val attendanceDao: AttendanceDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val backupLogDao: BackupLogDao
) {
    // ── GYM INFO ──────────────────────────────
    fun getGymInfo(): Flow<GymInfo?> = gymInfoDao.getGymInfo()
    suspend fun getGymInfoOnce(): GymInfo? = gymInfoDao.getGymInfoOnce()
    suspend fun saveGymInfo(info: GymInfo) = gymInfoDao.saveGymInfo(info)

    // ── PLANS ─────────────────────────────────
    fun getAllPlans(): Flow<List<SubscriptionPlan>> = subscriptionPlanDao.getAllPlans()
    suspend fun insertPlan(plan: SubscriptionPlan): Long = subscriptionPlanDao.insertPlan(plan)
    suspend fun updatePlan(plan: SubscriptionPlan) = subscriptionPlanDao.updatePlan(plan)
    suspend fun deletePlan(id: Long) = subscriptionPlanDao.deletePlan(id)

    // ── MEMBERS ───────────────────────────────
    fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()
    suspend fun getAllMembersOnce(): List<Member> = memberDao.getAllMembersOnce()
    fun getMemberById(id: Long): Flow<Member?> = memberDao.getMemberById(id)
    suspend fun getMemberByIdOnce(id: Long): Member? = memberDao.getMemberByIdOnce(id)
    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)
    fun getTotalMemberCount(): Flow<Int> = memberDao.getTotalMemberCount()
    fun getActivePaidCount(): Flow<Int> = memberDao.getActivePaidCount()
    fun getPendingCount(): Flow<Int> = memberDao.getPendingCount()
    fun getMembersByShift(shift: TimeShift): Flow<List<Member>> = memberDao.getMembersByShift(shift)
    fun getTotalDue(): Flow<Double?> = memberDao.getTotalDue()
    suspend fun insertMember(member: Member): Long = memberDao.insertMember(member)
    suspend fun updateMember(member: Member) = memberDao.updateMember(member)
    suspend fun deleteMember(id: Long) = memberDao.deleteMember(id)    // soft delete
    suspend fun hardDeleteMember(id: Long) = memberDao.hardDeleteMember(id)

    // ── ATTENDANCE ────────────────────────────
    fun getAttendanceByDate(date: String): Flow<List<Attendance>> = attendanceDao.getAttendanceByDate(date)
    fun getMemberAttendance(memberId: Long): Flow<List<Attendance>> = attendanceDao.getMemberAttendance(memberId)
    suspend fun getAttendanceRecord(memberId: Long, date: String): Attendance? = attendanceDao.getAttendanceRecord(memberId, date)
    suspend fun insertAttendance(attendance: Attendance) = attendanceDao.insertAttendance(attendance)
    suspend fun deleteAttendance(id: Long) = attendanceDao.deleteAttendance(id)

    // ── PAYMENTS ──────────────────────────────
    fun getAllPayments(): Flow<List<Payment>> = paymentDao.getAllPayments()
    fun getMemberPayments(memberId: Long): Flow<List<Payment>> = paymentDao.getMemberPayments(memberId)
    suspend fun getMonthlyRevenue(yearMonth: String): Double? = paymentDao.getMonthlyRevenue(yearMonth)
    suspend fun insertPayment(payment: Payment): Long = paymentDao.insertPayment(payment)
    suspend fun deletePayment(id: Long) = paymentDao.deletePayment(id)

    // ── EXPENSES ──────────────────────────────
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    suspend fun getMonthlyExpenses(yearMonth: String): Double? = expenseDao.getMonthlyExpenses(yearMonth)
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(id: Long) = expenseDao.deleteExpense(id)

    // ── BACKUP LOGS ───────────────────────────
    fun getBackupHistory(): Flow<List<BackupLog>> = backupLogDao.getBackupHistory()
    suspend fun getLastSuccessfulBackup(): BackupLog? = backupLogDao.getLastSuccessfulBackup()
    suspend fun insertBackupLog(log: BackupLog) = backupLogDao.insertBackupLog(log)
}
