package com.gymmanager.data.db

import androidx.room.*
import com.gymmanager.data.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
//  GYM INFO DAO
// ─────────────────────────────────────────────
@Dao
interface GymInfoDao {
    @Query("SELECT * FROM gym_info WHERE id = 1 LIMIT 1")
    fun getGymInfo(): Flow<GymInfo?>

    @Query("SELECT * FROM gym_info WHERE id = 1 LIMIT 1")
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
    suspend fun getPlanById(id: Long): SubscriptionPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlan): Long

    @Update
    suspend fun updatePlan(plan: SubscriptionPlan)

    @Query("UPDATE subscription_plans SET isActive = 0 WHERE id = :id")
    suspend fun deletePlan(id: Long)
}

// ─────────────────────────────────────────────
//  MEMBERS DAO
// ─────────────────────────────────────────────
@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllMembersOnce(): List<Member>

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberById(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberByIdOnce(id: Long): Member?

    @Query("""
        SELECT * FROM members 
        WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1")
    fun getTotalMemberCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1 AND status = 'PAID'")
    fun getActivePaidCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1 AND status != 'PAID'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM members WHERE isActive = 1 AND timeShift = :shift ORDER BY name ASC")
    fun getMembersByShift(shift: TimeShift): Flow<List<Member>>

    @Query("SELECT SUM(amountDue) FROM members WHERE isActive = 1")
    fun getTotalDue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    // Soft delete
    @Query("UPDATE members SET isActive = 0 WHERE id = :id")
    suspend fun deleteMember(id: Long)

    // Hard delete (for data wipe)
    @Query("DELETE FROM members WHERE id = :id")
    suspend fun hardDeleteMember(id: Long)
}

// ─────────────────────────────────────────────
//  ATTENDANCE DAO
// ─────────────────────────────────────────────
@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY checkInTime DESC")
    fun getAttendanceByDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId ORDER BY date DESC LIMIT 30")
    fun getMemberAttendance(memberId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND date = :date LIMIT 1")
    suspend fun getAttendanceRecord(memberId: Long, date: String): Attendance?

    @Query("SELECT COUNT(*) FROM attendance WHERE memberId = :memberId AND date LIKE :monthPrefix || '%'")
    suspend fun getMonthlyAttendanceCount(memberId: Long, monthPrefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long)
}

// ─────────────────────────────────────────────
//  PAYMENTS DAO
// ─────────────────────────────────────────────
@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY paymentDate DESC")
    fun getMemberPayments(memberId: Long): Flow<List<Payment>>

    @Query("SELECT SUM(amount) FROM payments WHERE strftime('%Y-%m', datetime(paymentDate/1000,'unixepoch')) = :yearMonth")
    suspend fun getMonthlyRevenue(yearMonth: String): Double?

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: Long)
}

// ─────────────────────────────────────────────
//  EXPENSES DAO
// ─────────────────────────────────────────────
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', datetime(date/1000,'unixepoch')) = :yearMonth")
    suspend fun getMonthlyExpenses(yearMonth: String): Double?

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)
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
