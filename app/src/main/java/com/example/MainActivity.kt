package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.KycScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.RepaymentScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DurgaMaaLoanApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DurgaMaaLoanApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationsCount.collectAsState()
    val snackMessage by viewModel.snackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackMessage()
        }
    }

    // Back handling for custom navigation state
    BackHandler(enabled = currentScreen != AppScreen.AUTH && currentScreen != AppScreen.CUSTOMER_DASHBOARD && currentScreen != AppScreen.OWNER_DASHBOARD) {
        if (currentUser?.role == UserRole.OWNER) {
            viewModel.selectScreen(AppScreen.OWNER_DASHBOARD)
        } else {
            viewModel.selectScreen(AppScreen.CUSTOMER_DASHBOARD)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentUser != null && currentScreen != AppScreen.AUTH,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color.White
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Drawer Header with Durga Maa Loan branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Durga Maa Loan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Genuine Loan & Settlement",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Drawer", tint = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.fullName?.firstOrNull()?.uppercase() ?: "U",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentUser?.fullName ?: "Customer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "+91 ${currentUser?.phone ?: ""}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Navigation Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF0284C7)) },
                        label = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                        selected = currentScreen == AppScreen.CUSTOMER_DASHBOARD,
                        onClick = {
                            viewModel.selectScreen(AppScreen.CUSTOMER_DASHBOARD)
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF0284C7)) },
                        label = { Text("Repayment & UPI", fontWeight = FontWeight.SemiBold) },
                        selected = currentScreen == AppScreen.REPAYMENT,
                        onClick = {
                            viewModel.selectScreen(AppScreen.REPAYMENT)
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0284C7)) },
                        label = { Text("Consent-Based KYC", fontWeight = FontWeight.SemiBold) },
                        selected = currentScreen == AppScreen.KYC_SCREEN,
                        onClick = {
                            viewModel.selectScreen(AppScreen.KYC_SCREEN)
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFD97706)) },
                        label = { Text("Owner Portal", fontWeight = FontWeight.SemiBold) },
                        selected = currentScreen == AppScreen.OWNER_DASHBOARD,
                        onClick = {
                            if (currentUser?.role == UserRole.OWNER) {
                                viewModel.selectScreen(AppScreen.OWNER_DASHBOARD)
                            } else {
                                viewModel.setAuthRole(UserRole.OWNER)
                                viewModel.selectScreen(AppScreen.AUTH)
                            }
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF0284C7)) },
                        label = { Text("Alerts & Updates", fontWeight = FontWeight.SemiBold) },
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge { Text("$unreadNotifications") }
                            }
                        },
                        selected = currentScreen == AppScreen.NOTIFICATIONS,
                        onClick = {
                            viewModel.selectScreen(AppScreen.NOTIFICATIONS)
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444)) },
                        label = { Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentUser != null && currentUser?.role == UserRole.CUSTOMER && currentScreen != AppScreen.AUTH) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
                        containerColor = Color.White
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.CUSTOMER_DASHBOARD,
                            onClick = { viewModel.selectScreen(AppScreen.CUSTOMER_DASHBOARD) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Loans") },
                            label = { Text("Dashboard", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_item_loans")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.REPAYMENT,
                            onClick = { viewModel.selectScreen(AppScreen.REPAYMENT) },
                            icon = { Icon(Icons.Default.QrCode2, contentDescription = "Pay UPI") },
                            label = { Text("Repayment", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_item_repay")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.KYC_SCREEN,
                            onClick = { viewModel.selectScreen(AppScreen.KYC_SCREEN) },
                            icon = { Icon(Icons.Default.Security, contentDescription = "KYC") },
                            label = { Text("KYC", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_item_kyc")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.NOTIFICATIONS,
                            onClick = { viewModel.selectScreen(AppScreen.NOTIFICATIONS) },
                            icon = {
                                BadgedBox(badge = {
                                    if (unreadNotifications > 0) {
                                        Badge { Text("$unreadNotifications") }
                                    }
                                }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                }
                            },
                            label = { Text("Alerts", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_item_alerts")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.AUTH -> AuthScreen(viewModel = viewModel)
                    AppScreen.CUSTOMER_DASHBOARD -> CustomerDashboardScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                    AppScreen.REPAYMENT -> RepaymentScreen(viewModel = viewModel)
                    AppScreen.KYC_SCREEN -> KycScreen(viewModel = viewModel)
                    AppScreen.OWNER_DASHBOARD -> OwnerDashboardScreen(viewModel = viewModel)
                    AppScreen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
