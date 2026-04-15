package com.gymmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

// ─────────────────────────────────────────────
//  GYM INFO
// ─────────────────────────────────────────────
@Serializable
@Entity(tableName = "gym_info")
data class GymInfo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val gymName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val logoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0
)

// ─────────────────────────────────────────────
//  SUBSCRIPTION PLANS
// ─────────────────────────────────────────────
@Serializable
@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationDays: Int,
    val price: Double,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0
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

@Serializable
@Entity(
    tableName = "members",
    indices = [Index(value = ["cnic"], unique = true)]
)
data class Member(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val cnic: String, // Mandatory
    val address: String = "",
    val gender: String = "Male",
    val photoUri: String? = null,
    val status: MemberStatus = MemberStatus.UNPAID,
    val joinDate: Long = System.currentTimeMillis(),

    // Subscription
    val planId: String? = null,
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
    val isBlocked: Boolean = false, // Block system
    val createdAt: Long = System.currentTimeMillis(),
    
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0,
    val imageHash: String? = null
)

// ─────────────────────────────────────────────
//  ATTENDANCE
// ─────────────────────────────────────────────
@Serializable
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
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memberId: String,
    val memberName: String,
    val date: String,               // "yyyy-MM-dd"
    val timeShift: TimeShift,
    val checkInTime: Long = System.currentTimeMillis(),
    val isPresent: Boolean = true,
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0
)

// ─────────────────────────────────────────────
//  PAYMENTS
// ─────────────────────────────────────────────
@Serializable
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
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memberId: String,
    val memberName: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val method: String = "Cash",
    val note: String = "",
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0
)

// ─────────────────────────────────────────────
//  EXPENSES
// ─────────────────────────────────────────────
@Serializable
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: Double,
    val category: String = "General",
    val date: Long = System.currentTimeMillis(),
    val receipt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val isDeleted: Int = 0
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
