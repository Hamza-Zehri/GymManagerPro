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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import com.gymmanager.ui.Screen
import com.gymmanager.ui.screens.*
import com.gymmanager.ui.theme.GymBgDark
import com.gymmanager.ui.theme.GymManagerTheme
import com.gymmanager.viewmodel.GymViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class MainActivity : FragmentActivity() {

    private val vm: GymViewModel by viewModels { GymViewModel.Factory(application) }

    private var unlocked by mutableStateOf(false)
    private var lastActiveTime by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Force lock on start
        unlocked = false

        // Register lifecycle observer to lock when app goes to background or screen turns off
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    lastActiveTime = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val currentTime = System.currentTimeMillis()
                    val twoMinutesInMillis = 2 * 60 * 1000
                    if (lastActiveTime != 0L && (currentTime - lastActiveTime > twoMinutesInMillis)) {
                        unlocked = false
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    // Lock immediately if phone is locked (screen turns off)
                    unlocked = false
                }
                else -> {}
            }
        })

        // Request permissions for WiFi Sync and Camera
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CAMERA
        )
        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        }

        setContent {
            GymManagerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GymBgDark) {
                    val appSettings by dataStore.data.collectAsState(initial = null)

                    val appLockEnabled = appSettings?.get(DataStoreKeys.APP_LOCK) ?: false
                    val savedPin       = appSettings?.get(DataStoreKeys.APP_PIN) ?: ""

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
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("memberId") ?: return@composable
            LaunchedEffect(id) { vm.selectMember(id) }
            EditMemberScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.MembersList.route) {
            MembersListScreen(vm = vm,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() })
        }

        composable(Screen.MemberProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("memberId") ?: return@composable
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

        composable(Screen.Sync.route) {
            SyncScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
