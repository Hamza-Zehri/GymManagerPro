package com.hamzaasad.gymmanagerpro.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hamzaasad.gymmanagerpro.data.model.*

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
    version = 2,
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
                        val now = System.currentTimeMillis()
                        db.execSQL("""
                            INSERT INTO subscription_plans (id, name, durationDays, price, description, isActive, createdAt, updatedAt, deviceId, isDeleted)
                            VALUES
                            ('${java.util.UUID.randomUUID()}', 'Monthly', 30, 3000.0, 'Standard monthly membership', 1, $now, $now, 'system', 0),
                            ('${java.util.UUID.randomUUID()}', '3 Months', 90, 8000.0, 'Quarterly membership — save 11%', 1, $now, $now, 'system', 0),
                            ('${java.util.UUID.randomUUID()}', '6 Months', 180, 14000.0, 'Half-year membership — save 22%', 1, $now, $now, 'system', 0),
                            ('${java.util.UUID.randomUUID()}', 'Yearly', 365, 25000.0, 'Full year membership — best value', 1, $now, $now, 'system', 0)
                        """.trimIndent())
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Clean up
                db.execSQL("DROP TABLE IF EXISTS `gym_info_new`")
                db.execSQL("DROP TABLE IF EXISTS `subscription_plans_new`")
                db.execSQL("DROP TABLE IF EXISTS `members_new`")
                db.execSQL("DROP TABLE IF EXISTS `attendance_new`")
                db.execSQL("DROP TABLE IF EXISTS `payments_new`")
                db.execSQL("DROP TABLE IF EXISTS `expenses_new`")

                // --- 1. gym_info ---
                db.execSQL("CREATE TABLE `gym_info_new` (`id` TEXT NOT NULL, `gymName` TEXT NOT NULL, `ownerName` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `logoUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `gym_info_new` SELECT CAST(`id` AS TEXT), `gymName`, `ownerName`, `phone`, `address`, `logoUri`, COALESCE(`createdAt`, strftime('%s','now')*1000), COALESCE(`createdAt`, strftime('%s','now')*1000), '', 0 FROM `gym_info` ")
                db.execSQL("DROP TABLE `gym_info`")
                db.execSQL("ALTER TABLE `gym_info_new` RENAME TO `gym_info`")

                // --- 2. subscription_plans ---
                db.execSQL("CREATE TABLE `subscription_plans_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `durationDays` INTEGER NOT NULL, `price` REAL NOT NULL, `description` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `subscription_plans_new` SELECT `id`, `name`, `durationDays`, `price`, COALESCE(`description`, ''), CASE WHEN `isActive` THEN 1 ELSE 0 END, COALESCE(`createdAt`, strftime('%s','now')*1000), COALESCE(`createdAt`, strftime('%s','now')*1000), '', 0 FROM `subscription_plans` ")
                db.execSQL("DROP TABLE `subscription_plans`")
                db.execSQL("ALTER TABLE `subscription_plans_new` RENAME TO `subscription_plans`")

                // --- 3. members ---
                db.execSQL("CREATE TABLE `members_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `cnic` TEXT NOT NULL, `address` TEXT NOT NULL, `gender` TEXT NOT NULL, `photoUri` TEXT, `status` TEXT NOT NULL, `joinDate` INTEGER NOT NULL, `planId` TEXT, `planName` TEXT, `subscriptionStart` INTEGER, `subscriptionEnd` INTEGER, `totalFee` REAL NOT NULL, `amountPaid` REAL NOT NULL, `amountDue` REAL NOT NULL, `timeShift` TEXT NOT NULL, `customShiftTime` TEXT, `lastAttendance` INTEGER, `isActive` INTEGER NOT NULL, `isBlocked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `imageHash` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `members_new` SELECT `id`, `name`, `phone`, `phone`, COALESCE(`address`, ''), COALESCE(`gender`, 'Male'), `photoUri`, `status`, `joinDate`, `planId`, `planName`, `subscriptionStart`, `subscriptionEnd`, `totalFee`, `amountPaid`, `amountDue`, `timeShift`, NULL, `lastAttendance`, 1, 0, `joinDate`, `joinDate`, '', 0, NULL FROM `members` ")
                db.execSQL("DROP TABLE `members`")
                db.execSQL("ALTER TABLE `members_new` RENAME TO `members`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_members_cnic` ON `members` (`cnic`)")

                // --- 4. attendance ---
                db.execSQL("CREATE TABLE `attendance_new` (`id` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `date` TEXT NOT NULL, `timeShift` TEXT NOT NULL, `checkInTime` INTEGER NOT NULL, `isPresent` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `attendance_new` SELECT `id`, `memberId`, `memberName`, `date`, `timeShift`, `checkInTime`, CASE WHEN `isPresent` THEN 1 ELSE 0 END, `checkInTime`, '', 0 FROM `attendance` ")
                db.execSQL("DROP TABLE `attendance`")
                db.execSQL("ALTER TABLE `attendance_new` RENAME TO `attendance`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_memberId` ON `attendance` (`memberId`)")

                // --- 5. payments ---
                db.execSQL("CREATE TABLE `payments_new` (`id` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `amount` REAL NOT NULL, `paymentDate` INTEGER NOT NULL, `method` TEXT NOT NULL, `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `payments_new` SELECT `id`, `memberId`, `memberName`, `amount`, `paymentDate`, COALESCE(`method`, 'Cash'), COALESCE(`note`, ''), `paymentDate`, '', 0 FROM `payments` ")
                db.execSQL("DROP TABLE `payments`")
                db.execSQL("ALTER TABLE `payments_new` RENAME TO `payments`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_memberId` ON `payments` (`memberId`)")

                // --- 6. expenses ---
                db.execSQL("CREATE TABLE `expenses_new` (`id` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `date` INTEGER NOT NULL, `receipt` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `expenses_new` SELECT `id`, `description`, `amount`, COALESCE(`category`, 'General'), `date`, `receipt`, COALESCE(`createdAt`, `date`), COALESCE(`createdAt`, `date`), '', 0 FROM `expenses` ")
                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
            }
        }
    }
}
