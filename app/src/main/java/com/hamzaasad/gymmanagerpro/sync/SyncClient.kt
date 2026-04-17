package com.hamzaasad.gymmanagerpro.sync

import android.content.Context
import com.hamzaasad.gymmanagerpro.data.db.GymDatabase
import com.hamzaasad.gymmanagerpro.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SyncClient(private val context: Context, private val db: GymDatabase) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun syncWithServer(serverIp: String, lastSyncTime: Long): SyncResult = withContext(Dispatchers.IO) {
        try {
            val response: SyncResponse = client.get("http://$serverIp:8080/sync?lastSyncTime=$lastSyncTime").body()
            val payload = response.payload
            
            var recordsSynced = 0
            
            // 1. Sync Gym Info
            payload.gymInfo.forEach { db.gymInfoDao().saveGymInfo(it) }
            
            // 2. Sync Plans
            payload.plans.forEach { plan ->
                val local = db.subscriptionPlanDao().getPlanById(plan.id)
                if (local == null || plan.updatedAt > local.updatedAt) {
                    db.subscriptionPlanDao().insertPlan(plan)
                    recordsSynced++
                }
            }
            
            // 3. Sync Members
            payload.members.forEach { member ->
                val localById = db.memberDao().getMemberByIdOnce(member.id)
                val localByCnic = db.memberDao().getMemberByCnic(member.cnic)

                when {
                    localById == null && localByCnic == null -> {
                        // Truly new member
                        db.memberDao().insertMember(member)
                        recordsSynced++
                        if (member.imageHash != null) downloadImage(serverIp, member.id)
                    }
                    localById != null -> {
                        // ID exists - update if newer
                        if (member.updatedAt > localById.updatedAt) {
                            db.memberDao().insertMember(member)
                            recordsSynced++
                            if (member.imageHash != null && member.imageHash != localById.imageHash) {
                                downloadImage(serverIp, member.id)
                            }
                        }
                    }
                    localByCnic != null -> {
                        // CNIC exists but with different ID - Merge them to avoid duplicate CNIC error
                        if (member.updatedAt > localByCnic.updatedAt) {
                            val merged = member.copy(id = localByCnic.id)
                            db.memberDao().insertMember(merged)
                            recordsSynced++
                            if (member.imageHash != null && member.imageHash != localByCnic.imageHash) {
                                downloadImage(serverIp, localByCnic.id)
                            }
                        }
                    }
                }
            }
            
            // 4. Sync Attendance
            payload.attendance.forEach { record ->
                val local = db.attendanceDao().getAttendanceRecordById(record.id)
                if (local == null || record.updatedAt > local.updatedAt) {
                    db.attendanceDao().insertAttendance(record)
                    recordsSynced++
                }
            }
            
            // 5. Sync Payments
            payload.payments.forEach { payment ->
                val local = db.paymentDao().getPaymentById(payment.id)
                if (local == null || payment.updatedAt > local.updatedAt) {
                    db.paymentDao().insertPayment(payment)
                    recordsSynced++
                }
            }
            
            // 6. Sync Expenses
            payload.expenses.forEach { expense ->
                val local = db.expenseDao().getExpenseById(expense.id)
                if (local == null || expense.updatedAt > local.updatedAt) {
                    db.expenseDao().insertExpense(expense)
                    recordsSynced++
                }
            }

            SyncResult.Success(recordsSynced, payload.timestamp)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun downloadImage(serverIp: String, memberId: String) {
        try {
            val response = client.get("http://$serverIp:8080/image/$memberId")
            if (response.status.value == 200) {
                val bytes = response.readBytes()
                val file = File(context.filesDir, "images/$memberId.jpg")
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { it.write(bytes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

sealed class SyncResult {
    data class Success(val count: Int, val serverTime: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
