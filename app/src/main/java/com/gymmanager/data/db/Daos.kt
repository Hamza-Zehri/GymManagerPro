package com.gymmanager.data.db

import androidx.room.*
import com.gymmanager.data.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
//  GYM INFO DAO
// ─────────────────────────────────────────────
@Dao
interface GymInfoDao {
    @Query("SELECT * FROM gym_info LIMIT 1")
    fun getGymInfo(): Flow<GymInfo?>

    @Query("SELECT * FROM gym_info LIMIT 1")
    suspend fun getGymInfoOnce(): GymInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGymInfo(gymInfo: GymInfo)
}

// ─────────────────────────────────────────────
//  SUBSCRIPTION PLANS DAO
// ─────────────────────────────────────────────
@Dao
interface SubscriptionPlanDao {
    @Query("SELECT * FROM subscription_plans WHERE isActive = 1 ORDER BY price ASC")
    fun getAllPlans(): Flow<List<SubscriptionPlan>>

    @Query("SELECT * FROM subscription_plans WHERE id = :id")
    suspend fun getPlanById(id: String): SubscriptionPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlan)

    @Update
    suspend fun updatePlan(plan: SubscriptionPlan)

    @Query("UPDATE subscription_plans SET isActive = 0, isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun deletePlan(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM subscription_plans WHERE updatedAt > :lastSync")
    suspend fun getChanges(lastSync: Long): List<SubscriptionPlan>
}

// ─────────────────────────────────────────────
//  MEMBERS DAO
// ─────────────────────────────────────────────
@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isDeleted = 0 AND isBlocked = 0 ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE isDeleted = 0 AND isBlocked = 0 ORDER BY name ASC")
    suspend fun getAllMembersOnce(): List<Member>

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberById(id: String): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberByIdOnce(id: String): Member?

    @Query("""
        SELECT * FROM members 
        WHERE isDeleted = 0 AND isBlocked = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR cnic LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members WHERE isDeleted = 0 AND isBlocked = 0")
    fun getTotalMemberCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE isDeleted = 0 AND isBlocked = 0 AND status = 'PAID'")
    fun getActivePaidCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE isDeleted = 0 AND isBlocked = 0 AND status != 'PAID'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM members WHERE isDeleted = 0 AND isBlocked = 0 AND timeShift = :shift ORDER BY name ASC")
    fun getMembersByShift(shift: TimeShift): Flow<List<Member>>

    @Query("SELECT SUM(amountDue) FROM members WHERE isDeleted = 0 AND isBlocked = 0")
    fun getTotalDue(): Flow<Double?>

    @Query("SELECT * FROM members WHERE cnic = :cnic LIMIT 1")
    suspend fun getMemberByCnic(cnic: String): Member?

    @Query("SELECT * FROM members WHERE isBlocked = 1 AND isDeleted = 0")
    fun getBlockedMembers(): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Update
    suspend fun updateMember(member: Member)

    // Soft delete for sync
    @Query("UPDATE members SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun deleteMember(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM members WHERE updatedAt > :lastSync")
    suspend fun getChanges(lastSync: Long): List<Member>

    // Hard delete (for data wipe)
    @Query("DELETE FROM members WHERE id = :id")
    suspend fun hardDeleteMember(id: String)
}

// ─────────────────────────────────────────────
//  ATTENDANCE DAO
// ─────────────────────────────────────────────
@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY checkInTime DESC")
    fun getAttendanceByDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND isDeleted = 0 ORDER BY date DESC LIMIT 30")
    fun getMemberAttendance(memberId: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND date = :date AND isDeleted = 0 LIMIT 1")
    suspend fun getAttendanceRecord(memberId: String, date: String): Attendance?

    @Query("SELECT COUNT(*) FROM attendance WHERE memberId = :memberId AND date LIKE :monthPrefix || '%' AND isDeleted = 0")
    suspend fun getMonthlyAttendanceCount(memberId: String, monthPrefix: String): Int

    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceRecordById(id: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query("UPDATE attendance SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun deleteAttendance(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM attendance WHERE updatedAt > :lastSync")
    suspend fun getChanges(lastSync: Long): List<Attendance>
}

// ─────────────────────────────────────────────
//  PAYMENTS DAO
// ─────────────────────────────────────────────
@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun getMemberPayments(memberId: String): Flow<List<Payment>>

    @Query("SELECT SUM(amount) FROM payments WHERE isDeleted = 0 AND strftime('%Y-%m', datetime(paymentDate/1000,'unixepoch')) = :yearMonth")
    suspend fun getMonthlyRevenue(yearMonth: String): Double?

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: String): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("UPDATE payments SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun deletePayment(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM payments WHERE updatedAt > :lastSync")
    suspend fun getChanges(lastSync: Long): List<Payment>
}

// ─────────────────────────────────────────────
//  EXPENSES DAO
// ─────────────────────────────────────────────
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE isDeleted = 0 AND strftime('%Y-%m', datetime(date/1000,'unixepoch')) = :yearMonth")
    suspend fun getMonthlyExpenses(yearMonth: String): Double?

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun deleteExpense(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expenses WHERE updatedAt > :lastSync")
    suspend fun getChanges(lastSync: Long): List<Expense>
}

// ─────────────────────────────────────────────
//  BACKUP LOG DAO
// ─────────────────────────────────────────────
@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_log ORDER BY timestamp DESC LIMIT 20")
    fun getBackupHistory(): Flow<List<BackupLog>>

    @Query("SELECT * FROM backup_log WHERE isSuccess = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulBackup(): BackupLog?

    @Insert
    suspend fun insertBackupLog(log: BackupLog)
}
