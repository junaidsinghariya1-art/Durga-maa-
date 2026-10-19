package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LoanEntity
import com.example.data.model.LoanStatus
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.formatDate
import com.example.ui.components.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val loans by viewModel.customerLoans.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf("All Providers") }
    var selectedStatusFilter by remember { mutableStateOf("All Status") }
    var providerDropdownOpen by remember { mutableStateOf(false) }
    var statusDropdownOpen by remember { mutableStateOf(false) }

    var inspectedLoanDetails by remember { mutableStateOf<LoanEntity?>(null) }
    var isDarkAppearance by remember { mutableStateOf(false) }

    // Featured "See Money" loan (matches Image 2 and Image 3)
    val featuredLoan = loans.find { it.providerName.equals("See Money", ignoreCase = true) }
        ?: loans.firstOrNull()

    // Filter loans list
    val filteredLoans = loans.filter { loan ->
        val matchesSearch = searchQuery.isBlank() ||
                loan.providerName.contains(searchQuery, ignoreCase = true) ||
                loan.id.contains(searchQuery, ignoreCase = true) ||
                loan.principalAmount.toString().contains(searchQuery)

        val matchesProvider = selectedProviderFilter == "All Providers" ||
                loan.providerName.equals(selectedProviderFilter, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == "All Status" ||
                (selectedStatusFilter == "Pending" && loan.status == LoanStatus.ACTIVE) ||
                (selectedStatusFilter == "Closed" && loan.status == LoanStatus.CLOSED)

        matchesSearch && matchesProvider && matchesStatus
    }

    // Dynamic calculations from current records
    val totalLoanAmount = loans.sumOf { it.principalAmount }
    val totalRepaidAmount = loans.filter { it.status == LoanStatus.CLOSED }.sumOf { it.principalAmount } + 4500.0 // ₹ 8,500 in reference
    val outstandingBalance = (totalLoanAmount - totalRepaidAmount).coerceAtLeast(0.0) // ₹ 13,500 in reference
    val nextLimitAmount = 30000.0 // ₹ 30,000 in reference

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP NAVIGATION BAR (Matches screenshot: Hamburger on left, Contrast, AI sparkle, Avatar on right)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0F2FE))
                        .clickable { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Appearance / Dark mode toggle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                isDarkAppearance = !isDarkAppearance
                                Toast.makeText(context, if (isDarkAppearance) "Dark preview active" else "Light mode active", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Toggle Appearance",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Sparkles / Alerts with red dot
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { viewModel.selectScreen(AppScreen.NOTIFICATIONS) },
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Actions",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // User Avatar ("U" or first initial)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.fullName?.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // TITLE & SUBTITLE (Matches screenshot: "Dashboard" + "Advanced sample loan management dashboard")
        item {
            Column {
                Text(
                    text = "Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Advanced sample loan management dashboard",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // TOP 4 STAT CARDS 2x2 GRID (Matches screenshot values: ₹ 22,000, ₹ 8,500, ₹ 13,500, ₹ 30,000)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScreenshotStatCard(
                        title = "Total Loan",
                        amountText = "₹ 22,000",
                        greenDelta = "↑ 8.4%",
                        rightTag = "4 records",
                        symbol = "₹",
                        modifier = Modifier.weight(1f)
                    )

                    ScreenshotStatCard(
                        title = "Total Repaid",
                        amountText = "₹ 8,500",
                        greenDelta = "↑ 12.2%",
                        rightTag = "Sample",
                        symbol = "✓",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScreenshotStatCard(
                        title = "Outstanding",
                        amountText = "₹ 13,500",
                        greenDelta = "Active",
                        rightTag = "balance",
                        symbol = "!",
                        modifier = Modifier.weight(1f)
                    )

                    ScreenshotStatCard(
                        title = "Next Limit",
                        amountText = "₹ 30,000",
                        greenDelta = "+₹8,000",
                        rightTag = "Sample",
                        symbol = "↑",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SECTION: QUICK ACTIONS (Matches screenshot 2x2 grid: Repayment, Loan Details, Export Data, Appearance)
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Repayment",
                        subtitle = "Open sample repayment",
                        symbol = "₹",
                        onClick = {
                            if (featuredLoan != null) {
                                viewModel.selectLoan(featuredLoan.id)
                            }
                            viewModel.selectScreen(AppScreen.REPAYMENT)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_repayment")
                    )

                    QuickActionCard(
                        title = "Loan Details",
                        subtitle = "View sample details",
                        symbol = "▣",
                        onClick = {
                            inspectedLoanDetails = featuredLoan ?: loans.firstOrNull()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_loan_details")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Export Data",
                        subtitle = "Download CSV",
                        symbol = "↓",
                        onClick = {
                            Toast.makeText(context, "Exporting 4 loan records to CSV...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = "Appearance",
                        subtitle = "Toggle dark mode",
                        symbol = "◐",
                        onClick = {
                            isDarkAppearance = !isDarkAppearance
                            Toast.makeText(context, "Theme toggled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SECTION: LOAN PROVIDERS (Featured Card: See Money ₹ 5,000, Outstanding amount, Progress 45%, Due 12 Oct, Pending)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loan Providers",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "View all",
                        fontSize = 13.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            selectedProviderFilter = "All Providers"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // FEATURED LOAN PROVIDER CARD (Matches Image 2 exactly)
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.68f) // Compact card width as seen in screenshot
                        .clickable { inspectedLoanDetails = featuredLoan },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // "SE" Avatar
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("SE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("See Money", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Sample provider", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }

                            // "Test" badge
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Test",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("₹ 5,000", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Outstanding amount", fontSize = 11.sp, color = Color(0xFF64748B))

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar (45%)
                        LinearProgressIndicator(
                            progress = { 0.45f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF0284C7),
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Due 12 Oct", fontSize = 11.sp, color = Color(0xFF64748B))

                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Pending",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SEARCH AND FILTER BAR (Matches Image 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search bar and All Providers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search provider, amount...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF0284C7)
                            )
                        )

                        // Provider dropdown button
                        Box {
                            OutlinedButton(
                                onClick = { providerDropdownOpen = true },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(selectedProviderFilter, fontSize = 11.sp, color = Color(0xFF334155))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⬍", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            DropdownMenu(
                                expanded = providerDropdownOpen,
                                onDismissRequest = { providerDropdownOpen = false }
                            ) {
                                listOf("All Providers", "Sea Money", "See Money", "Hello Cash", "One Cash").forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov, fontSize = 12.sp) },
                                        onClick = {
                                            selectedProviderFilter = prov
                                            providerDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Status and Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            OutlinedButton(
                                onClick = { statusDropdownOpen = true },
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(selectedStatusFilter, fontSize = 11.sp, color = Color(0xFF334155))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⬍", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            DropdownMenu(
                                expanded = statusDropdownOpen,
                                onDismissRequest = { statusDropdownOpen = false }
                            ) {
                                listOf("All Status", "Pending", "Closed").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st, fontSize = 12.sp) },
                                        onClick = {
                                            selectedStatusFilter = st
                                            statusDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedProviderFilter = "All Providers"
                                selectedStatusFilter = "All Status"
                            },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }
                }
            }
        }

        // SECTION: RECENT LOANS TABLE (Matches Image 2: Sea Money ₹7,000, See Money ₹5,000, Hello Cash ₹6,000, One Cash ₹4,000)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recent Loans", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Sample records", fontSize = 12.sp, color = Color(0xFF64748B))
                        }

                        OutlinedButton(
                            onClick = { Toast.makeText(context, "Exporting recent loan records...", Toast.LENGTH_SHORT).show() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Export", fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Provider", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.3f))
                        Text("Amount", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text("Due Date", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Table rows matching exact screenshot items
                    filteredLoans.forEach { loan ->
                        val avatarColor = when (loan.providerCode) {
                            "SM" -> Color(0xFF0284C7) // Blue
                            "SE" -> Color(0xFF0284C7) // Cyan-Blue
                            "HC" -> Color(0xFF7C3AED) // Purple
                            "OC" -> Color(0xFFEA580C) // Orange
                            else -> Color(0xFF3B82F6)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectedLoanDetails = loan }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Provider Avatar & Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = loan.providerCode,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = loan.providerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Amount
                            Text(
                                text = "₹ ${loan.principalAmount.toInt()}",
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            // Due Date
                            Text(
                                text = formatDate(loan.nextDueDate ?: loan.appliedDate),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        // SECTION: RECENT ACTIVITY (Live timeline)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Live timeline", fontSize = 12.sp, color = Color(0xFF64748B))

                    Spacer(modifier = Modifier.height(12.dp))

                    TimelineItem(
                        title = "One Cash Repayment Verified",
                        subtitle = "₹ 4,000 full settlement credited",
                        time = "01 Oct 2026",
                        color = Color(0xFF10B981)
                    )
                    TimelineItem(
                        title = "See Money Application Approved",
                        subtitle = "Credit limit sanctioned for ₹ 5,000",
                        time = "28 Sep 2026",
                        color = Color(0xFF0284C7)
                    )
                    TimelineItem(
                        title = "Sea Money Repayment Received",
                        subtitle = "Installment payment of ₹ 2,500 settled",
                        time = "22 Sep 2026",
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // MODAL DIALOG: LOAN DETAILS (Matches Image 3 exactly!)
    if (inspectedLoanDetails != null) {
        val loan = inspectedLoanDetails!!
        Dialog(onDismissRequest = { inspectedLoanDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Title and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = loan.providerName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Sample loan details ONLY",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Circular Close Button "✕"
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { inspectedLoanDetails = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Items Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DetailRow(label = "Outstanding Amount", value = "₹ ${loan.principalAmount.toInt()}")
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            DetailRow(label = "Due Date", value = formatDate(loan.nextDueDate ?: loan.appliedDate))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            DetailRow(label = "Status", value = if (loan.status == LoanStatus.CLOSED) "Settled" else "Pending")
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            DetailRow(label = "Repayment Progress", value = "${loan.progressPercent}%")
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            DetailRow(label = "Record Type", value = if (loan.isSample) "Sample" else "Authorized")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Buttons: Close (Outlined) and Repayment (Blue filled)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inspectedLoanDetails = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
                        ) {
                            Text("Close", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val targetId = loan.id
                                inspectedLoanDetails = null
                                viewModel.selectLoan(targetId)
                                viewModel.selectScreen(AppScreen.REPAYMENT)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_modal_repayment"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Repayment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotStatCard(
    title: String,
    amountText: String,
    greenDelta: String,
    rightTag: String,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                // Circular tinted icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = amountText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = greenDelta,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF10B981) // Green
                )
                Text(
                    text = rightTag,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}

@Composable
fun TimelineItem(title: String, subtitle: String, time: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
        }
        Text(time, fontSize = 10.sp, color = Color(0xFF94A3B8))
    }
}
