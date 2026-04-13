package com.gymmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────
//  GYM INFO
// ─────────────────────────────────────────────
@Entity(tableName = "gym_info")
data class GymInfo(
    @PrimaryKey val id: Int = 1,
    val gymName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val logoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  SUBSCRIPTION PLANS
// ─────────────────────────────────────────────
@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationDays: Int,
    val price: Double,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  MEMBERS
// ─────────────────────────────────────────────
enum class MemberStatus { PAID, UNPAID, PARTIAL }
enum class TimeShift(val label: String, val startHour: Int) {
    SHIFT_1("4 PM – 6 PM", 16),
    SHIFT_2("6 PM – 8 PM", 18),
    SHIFT_3("8 PM – 10 PM", 20),
    SHIFT_4("10 PM – 12 AM", 22),
    CUSTOM("Custom Time", -1)
}

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val gender: String = "Male",
    val age: Int = 0,
    val weight: String = "",
    val height: String = "",
    val goal: String = "",
    val photoUri: String? = null,
    val status: MemberStatus = MemberStatus.UNPAID,
    val joinDate: Long = System.currentTimeMillis(),

    // Subscription
    val planId: Long? = null,
    val planName: String? = null,
    val subscriptionStart: Long? = null,
    val subscriptionEnd: Long? = null,

    // Fee
    val totalFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val amountDue: Double = 0.0,

    // Time shift (e.g. SHIFT_1 = 4 PM slot)
    val timeShift: TimeShift = TimeShift.SHIFT_1,
    val customShiftTime: String? = null,   // "HH:mm" if CUSTOM

    val lastAttendance: Long? = null,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  ATTENDANCE
// ─────────────────────────────────────────────
@Entity(
    tableName = "attendance",
    foreignKeys = [ForeignKey(
        entity = Member::class,
        parentColumns = ["id"],
        childColumns = ["memberId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("memberId")]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val memberName: String,
    val date: String,               // "yyyy-MM-dd"
    val timeShift: TimeShift,
    val checkInTime: Long = System.currentTimeMillis(),
    val isPresent: Boolean = true
)

// ─────────────────────────────────────────────
//  PAYMENTS
// ─────────────────────────────────────────────
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = Member::class,
        parentColumns = ["id"],
        childColumns = ["memberId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("memberId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val method: String = "Cash",
    val note: String = ""
)

// ─────────────────────────────────────────────
//  EXPENSES
// ─────────────────────────────────────────────
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: String = "General",
    val date: Long = System.currentTimeMillis(),
    val receipt: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  BACKUP LOG
// ─────────────────────────────────────────────
@Entity(tableName = "backup_log")
data class BackupLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val driveFileId: String? = null,
    val localPath: String? = null,
    val sizeBytes: Long = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
