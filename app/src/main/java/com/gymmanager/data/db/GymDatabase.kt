package com.gymmanager.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymmanager.data.model.*

// ─────────────────────────────────────────────
//  TYPE CONVERTERS
// ─────────────────────────────────────────────
class Converters {
    @TypeConverter fun fromMemberStatus(v: MemberStatus): String = v.name
    @TypeConverter fun toMemberStatus(v: String): MemberStatus = MemberStatus.valueOf(v)

    @TypeConverter fun fromTimeShift(v: TimeShift): String = v.name
    @TypeConverter fun toTimeShift(v: String): TimeShift {
        return try {
            TimeShift.valueOf(v)
        } catch (e: IllegalArgumentException) {
            // Handle legacy values from refactoring
            when (v) {
                "MORNING" -> TimeShift.SHIFT_1
                "AFTERNOON" -> TimeShift.SHIFT_1
                "EVENING" -> TimeShift.SHIFT_2
                "NIGHT" -> TimeShift.SHIFT_3
                else -> TimeShift.SHIFT_1
            }
        }
    }
}

// ─────────────────────────────────────────────
//  DATABASE
// ─────────────────────────────────────────────
@Database(
    entities = [
        GymInfo::class,
        SubscriptionPlan::class,
        Member::class,
        Attendance::class,
        Payment::class,
        Expense::class,
        BackupLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun gymInfoDao(): GymInfoDao
    abstract fun subscriptionPlanDao(): SubscriptionPlanDao
    abstract fun memberDao(): MemberDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun backupLogDao(): BackupLogDao

    companion object {
        const val DATABASE_NAME = "gym_manager.db"

        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default subscription plans
                        db.execSQL("""
                            INSERT INTO subscription_plans (name, durationDays, price, description, isActive, createdAt)
                            VALUES
                            ('Monthly', 30, 3000, 'Standard monthly membership', 1, ${System.currentTimeMillis()}),
                            ('3 Months', 90, 8000, 'Quarterly membership — save 11%', 1, ${System.currentTimeMillis()}),
                            ('6 Months', 180, 14000, 'Half-year membership — save 22%', 1, ${System.currentTimeMillis()}),
                            ('Yearly', 365, 25000, 'Full year membership — best value', 1, ${System.currentTimeMillis()})
                        """.trimIndent())
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
