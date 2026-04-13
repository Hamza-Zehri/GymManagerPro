package com.gymmanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.gymmanager.ui.Screen
import com.gymmanager.ui.screens.*
import com.gymmanager.ui.theme.GymBgDark
import com.gymmanager.ui.theme.GymManagerTheme
import com.gymmanager.viewmodel.GymViewModel
import kotlinx.coroutines.flow.map

class MainActivity : FragmentActivity() {

    private val vm: GymViewModel by viewModels { GymViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            GymManagerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GymBgDark) {
                    val appSettings by dataStore.data.collectAsState(initial = null)
                    var unlocked by remember { mutableStateOf(false) }

                    // Reset unlocked state when app is backgrounded
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) {
                                unlocked = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    val appLockEnabled = appSettings?.get(DataStoreKeys.APP_LOCK) ?: false
                    val savedPin       = appSettings?.get(DataStoreKeys.APP_PIN) ?: ""

                    // Security: Hide content in recent apps immediately if lock is enabled
                    SideEffect {
                        if (appLockEnabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }

                    if (appSettings == null) {
                        // Loading state - keep splash-like background to prevent peeking
                        Box(Modifier.fillMaxSize().background(GymBgDark))
                    } else if (appLockEnabled && savedPin.isNotEmpty() && !unlocked) {
                        AppLockScreen(
                            savedPin = savedPin,
                            onUnlocked = { unlocked = true }
                        )
                    } else {
                        GymApp(vm = vm)
                    }
                }
            }
        }
    }
}

@Composable
fun GymApp(vm: GymViewModel) {
    val navController = rememberNavController()
    val gymInfo by vm.gymInfo.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                val dest = if (gymInfo != null) Screen.Dashboard.route else Screen.Setup.route
                navController.navigate(dest) { popUpTo(Screen.Splash.route) { inclusive = true } }
            })
        }

        composable(Screen.Setup.route) {
            SetupScreen(onComplete = { info ->
                vm.saveGymInfo(info)
                navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Setup.route) { inclusive = true } }
            })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(vm = vm, onNavigate = { navController.navigate(it) })
        }

        composable(Screen.AddMember.route) {
            AddMemberScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.EditMember.route,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("memberId") ?: return@composable
            LaunchedEffect(id) { vm.selectMember(id) }
            EditMemberScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.MembersList.route) {
            MembersListScreen(vm = vm,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() })
        }

        composable(Screen.MemberProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("memberId") ?: return@composable
            LaunchedEffect(id) { vm.selectMember(id) }
            MemberProfileScreen(vm = vm,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() })
        }

        composable(Screen.Attendance.route) {
            AttendanceScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.FeeManagement.route) {
            FeeManagementScreen(vm = vm,
                onMemberClick = { id ->
                    vm.selectMember(id)
                    navController.navigate(Screen.MemberProfile.createRoute(id))
                },
                onBack = { navController.popBackStack() })
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.SubscriptionPlans.route) {
            SubscriptionPlansScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(vm = vm,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() })
        }

        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
