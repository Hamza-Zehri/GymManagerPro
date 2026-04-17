package com.hamzaasad.gymmanagerpro.ui

sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object Setup           : Screen("setup")
    object Dashboard       : Screen("dashboard")
    object AddMember       : Screen("add_member")
    object EditMember      : Screen("edit_member/{memberId}") {
        fun createRoute(id: String) = "edit_member/$id"
    }
    object MembersList     : Screen("members_list")
    object MemberProfile   : Screen("member_profile/{memberId}") {
        fun createRoute(id: String) = "member_profile/$id"
    }
    object FeeManagement   : Screen("fee_management")
    object SubscriptionPlans: Screen("subscription_plans")
    object Attendance      : Screen("attendance")
    object Expenses        : Screen("expenses")
    object Settings        : Screen("settings")
    object BackupRestore   : Screen("backup_restore")
    object Sync            : Screen("sync")
}
