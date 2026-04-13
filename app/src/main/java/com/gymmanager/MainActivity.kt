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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.edit
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import com.gymmanager.ui.Screen
import com.gymmanager.ui.screens.*
import com.gymmanager.ui.theme.GymBgDark
import com.gymmanager.ui.theme.GymManagerTheme
import com.gymmanager.viewmodel.GymViewModel
import kotlinx.coroutines.flow.map

class MainActivity : FragmentActivity() {

    private val vm: GymViewModel by viewModels { GymViewModel.Factory(application) }

    private var unlocked by mutableStateOf(false)
    private var lastActiveTime by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Force lock on start
        unlocked = false

        // Register lifecycle observer to lock when app goes to background
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                lastActiveTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                val currentTime = System.currentTimeMillis()
                val fiveMinutesInMillis = 5 * 60 * 1000
                if (lastActiveTime != 0L && (currentTime - lastActiveTime > fiveMinutesInMillis)) {
                    unlocked = false
                }
            }
        })

        setContent {
            GymManagerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GymBgDark) {
                    val appSettings by dataStore.data.collectAsState(initial = null)

                    val appLockEnabled = appSettings?.get(DataStoreKeys.APP_LOCK) ?: false
                    val savedPin       = appSettings?.get(DataStoreKeys.APP_PIN) ?: ""

                    // Force unlock to false when app is explicitly started from launcher
                    // and ensure lifecycle observer handles backgrounding.
                    
                    // Security: Hide content in recent apps immediately if lock is enabled
                    // Removed FLAG_SECURE to allow screenshots
                    SideEffect {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }

                    if (appSettings == null) {
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
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                val dest = if (gymInfo != null) Screen.Dashboard.route else Screen.Setup.route
                navController.navigate(dest) { popUpTo(Screen.Splash.route) { inclusive = true } }
            })
        }

        composable(Screen.Setup.route) {
            SetupScreen(onComplete = { info, pin ->
                vm.saveGymInfo(info)
                // Save PIN and Enable App Lock during setup
                scope.launch {
                    context.dataStore.edit {
                        it[DataStoreKeys.APP_PIN] = pin
                        it[DataStoreKeys.APP_LOCK] = true
                    }
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Setup.route) { inclusive = true } }
                }
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
