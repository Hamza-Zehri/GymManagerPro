package com.gymmanager.sync

import com.gymmanager.data.model.*
import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(
    val members: List<Member> = emptyList(),
    val attendance: List<Attendance> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val plans: List<SubscriptionPlan> = emptyList(),
    val gymInfo: List<GymInfo> = emptyList(),
    val timestamp: Long
)

@Serializable
data class SyncResponse(
    val payload: SyncPayload,
    val serverDeviceId: String
)
