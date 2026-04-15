package com.gymmanager.sync

import android.content.Context
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.model.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SyncServer(private val context: Context, private val db: GymDatabase) {
    private var server: ApplicationEngine? = null

    private var isRunning = false

    fun start(port: Int = 8080): Boolean {
        if (isRunning) return true
        
        return try {
            server = embeddedServer(CIO, port = port) {
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    get("/sync") {
                        val lastSyncTime = call.request.queryParameters["lastSyncTime"]?.toLongOrNull() ?: 0L
                        // Use a 10-second safety buffer to ensure no records are missed due to processing time
                        val bufferedTime = if (lastSyncTime > 10000) lastSyncTime - 10000 else 0L
                        val payload = withContext(Dispatchers.IO) { getSyncPayload(bufferedTime) }
                        call.respond(SyncResponse(payload, getDeviceId(this@SyncServer.context)))
                    }

                    get("/image/{memberId}") {
                        val memberId = call.parameters["memberId"]
                        if (memberId != null) {
                            val file = File(this@SyncServer.context.filesDir, "images/$memberId.jpg")
                            if (file.exists()) {
                                call.respondFile(file)
                            } else {
                                call.respond(io.ktor.http.HttpStatusCode.NotFound)
                            }
                        }
                    }
                }
            }.start(wait = false)
            isRunning = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            false
        }
    }

    fun stop() {
        try {
            server?.stop(500, 1000)
            server = null
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getSyncPayload(lastSyncTime: Long): SyncPayload = withContext(Dispatchers.IO) {
        SyncPayload(
            members = db.memberDao().getChanges(lastSyncTime),
            attendance = db.attendanceDao().getChanges(lastSyncTime),
            payments = db.paymentDao().getChanges(lastSyncTime),
            expenses = db.expenseDao().getChanges(lastSyncTime),
            plans = db.subscriptionPlanDao().getChanges(lastSyncTime),
            gymInfo = listOfNotNull(db.gymInfoDao().getGymInfoOnce()),
            timestamp = System.currentTimeMillis()
        )
    }
}

fun getDeviceId(context: Context): String {
    return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
}
