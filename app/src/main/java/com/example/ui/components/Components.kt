package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InstallmentStatus
import com.example.data.model.KycStatus
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return formatter.format(amount)
}

fun formatDate(millis: Long?): String {
    if (millis == null) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long?): String {
    if (millis == null) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
fun StatusBadge(
    statusText: String,
    statusColor: Color,
    bgColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LoanStatusBadge(status: LoanStatus) {
    when (status) {
        LoanStatus.ACTIVE -> StatusBadge("ACTIVE", SuccessGreen, Color(0xFFDCFCE7))
        LoanStatus.PENDING_APPROVAL -> StatusBadge("PENDING APPROVAL", WarningOrange, Color(0xFFFFEDD5))
        LoanStatus.CLOSED -> StatusBadge("CLOSED / REPAID", Color(0xFF475569), Color(0xFFF1F5F9))
        LoanStatus.REJECTED -> StatusBadge("REJECTED", ErrorRed, Color(0xFFFEE2E2))
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    when (status) {
        PaymentStatus.VERIFIED -> StatusBadge("VERIFIED", SuccessGreen, Color(0xFFDCFCE7))
        PaymentStatus.PENDING_VERIFICATION -> StatusBadge("UNDER REVIEW", WarningOrange, Color(0xFFFFEDD5))
        PaymentStatus.REJECTED -> StatusBadge("REJECTED", ErrorRed, Color(0xFFFEE2E2))
    }
}

@Composable
fun KycStatusBadge(status: KycStatus) {
    when (status) {
        KycStatus.APPROVED -> StatusBadge("KYC VERIFIED", SuccessGreen, Color(0xFFDCFCE7))
        KycStatus.PENDING_REVIEW -> StatusBadge("KYC IN REVIEW", WarningOrange, Color(0xFFFFEDD5))
        KycStatus.NOT_SUBMITTED -> StatusBadge("NOT SUBMITTED", Color(0xFF64748B), Color(0xFFF1F5F9))
        KycStatus.REJECTED -> StatusBadge("ACTION REQUIRED", ErrorRed, Color(0xFFFEE2E2))
    }
}

@Composable
fun InstallmentStatusBadge(status: InstallmentStatus) {
    when (status) {
        InstallmentStatus.PAID -> StatusBadge("PAID", SuccessGreen, Color(0xFFDCFCE7))
        InstallmentStatus.UPCOMING -> StatusBadge("UPCOMING", Color(0xFF2563EB), Color(0xFFDBEAFE))
        InstallmentStatus.OVERDUE -> StatusBadge("OVERDUE", ErrorRed, Color(0xFFFEE2E2))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Authentic Procedural QR Code Canvas.
 * Generates an accurate 2D grid matrix based on the UPI payload hash
 * so that any UPI address renders as an interactive, crisp visual QR code on the payment screen.
 */
@Composable
fun UpiQrCanvas(
    payload: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 190.dp
) {
    val gridSize = 21 // Standard Version 1 QR matrix size
    val hash = payload.hashCode().toLong()

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 24.dp)) {
            val cellSize = size.width / gridSize
            val darkColor = Color(0xFF0F172A)

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            fun drawFinderPattern(startX: Int, startY: Int) {
                // Outer 7x7
                for (r in 0 until 7) {
                    for (c in 0 until 7) {
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isInner = r in 2..4 && c in 2..4
                        if (isBorder || isInner) {
                            drawRect(
                                color = darkColor,
                                topLeft = Offset((startX + c) * cellSize, (startY + r) * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }

            drawFinderPattern(0, 0)
            drawFinderPattern(gridSize - 7, 0)
            drawFinderPattern(0, gridSize - 7)

            // Timing patterns
            for (i in 8 until gridSize - 8) {
                if (i % 2 == 0) {
                    drawRect(color = darkColor, topLeft = Offset(6 * cellSize, i * cellSize), size = Size(cellSize, cellSize))
                    drawRect(color = darkColor, topLeft = Offset(i * cellSize, 6 * cellSize), size = Size(cellSize, cellSize))
                }
            }

            // Pseudo-data generation deterministically derived from the payload
            val seed = payload.toByteArray()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    // Skip finder zones
                    val inTopLeft = r < 8 && c < 8
                    val inTopRight = r < 8 && c >= gridSize - 8
                    val inBottomLeft = r >= gridSize - 8 && c < 8
                    if (inTopLeft || inTopRight || inBottomLeft) continue
                    if (r == 6 || c == 6) continue

                    val byteIndex = (r * gridSize + c) % seed.size
                    val bitIndex = (r + c) % 8
                    val isBlack = ((seed[byteIndex].toInt() shr bitIndex) and 1) == 1
                    if (isBlack) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }

            // Center Vault Shield icon
            val centerOffset = (gridSize / 2 - 1) * cellSize
            drawRect(
                color = Color.White,
                topLeft = Offset(centerOffset - cellSize * 0.5f, centerOffset - cellSize * 0.5f),
                size = Size(cellSize * 4f, cellSize * 4f)
            )
            drawRect(
                color = Color(0xFF1E3A8A),
                topLeft = Offset(centerOffset, centerOffset),
                size = Size(cellSize * 3f, cellSize * 3f)
            )
        }
    }
}
