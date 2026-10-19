package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.UpiQrCanvas
import com.example.ui.components.formatDate
import com.example.ui.components.formatInr

enum class RepaymentStep {
    LOAN_INFO,    // Images 4 & 5
    GATEWAY,      // Image 7
    UTR_UPLOAD    // Image 8
}

@Composable
fun RepaymentScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val loans by viewModel.customerLoans.collectAsState()
    val upiConfig by viewModel.upiConfig.collectAsState()
    val selectedLoanId by viewModel.selectedLoanId.collectAsState()

    // Find target loan (defaults to See Money or first active loan)
    val targetLoan = loans.find { it.id == selectedLoanId }
        ?: loans.find { it.providerName.equals("See Money", ignoreCase = true) }
        ?: loans.firstOrNull()

    val providerName = targetLoan?.providerName ?: "See Money"
    val repayAmount = if (providerName.equals("See Money", ignoreCase = true)) 5200.0 else (targetLoan?.totalRepaymentAmount ?: 5200.0)
    val applicationAmount = if (providerName.equals("See Money", ignoreCase = true)) 5200.0 else (targetLoan?.principalAmount ?: 5000.0)
    val expireDate = "2026-10-07"

    val centralUpiId = upiConfig?.upiId ?: "nr853950-1@okicici"
    val centralReceiver = upiConfig?.receiverName ?: "Durga Maa Loan Financial Services"

    // Flow steps
    var currentStep by remember { mutableStateOf(RepaymentStep.LOAN_INFO) }
    var showConfirmModal by remember { mutableStateOf(false) } // Image 6
    var showReceiptCheckModal by remember { mutableStateOf(false) }

    // UTR and Screenshot inputs
    var utrInput by remember { mutableStateOf("") }
    var attachedScreenshotUri by remember { mutableStateOf<String?>(null) }
    var screenshotFileName by remember { mutableStateOf("no file selected") }

    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedScreenshotUri = uri.toString()
            screenshotFileName = "receipt_${System.currentTimeMillis()}.jpg"
            Toast.makeText(context, "Screenshot selected", Toast.LENGTH_SHORT).show()
        }
    }

    val upiPayload = "upi://pay?pa=$centralUpiId&pn=${Uri.encode(centralReceiver)}&am=$repayAmount&cu=INR&tn=DurgaMaaLoan_${targetLoan?.id ?: "PAY"}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TOP HEADER BAR WITH BACK BUTTON
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable {
                            when (currentStep) {
                                RepaymentStep.UTR_UPLOAD -> currentStep = RepaymentStep.GATEWAY
                                RepaymentStep.GATEWAY -> currentStep = RepaymentStep.LOAN_INFO
                                RepaymentStep.LOAN_INFO -> viewModel.selectScreen(AppScreen.CUSTOMER_DASHBOARD)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", fontSize = 22.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Repayment",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(36.dp)) // balance center
            }
        }

        // =========================================================================
        // STEP 1: LOAN INFORMATION SCREEN (Matches Images 4 & 5)
        // =========================================================================
        if (currentStep == RepaymentStep.LOAN_INFO) {
            // Top Blue Header Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)) // Vibrant Blue
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Inner White Floating Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Repay Amount",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹ ${repayAmount.toInt()}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Payment date  $expireDate",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Due Date",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }

            // Subtext under top banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "You can borrow ", fontSize = 13.sp, color = Color(0xFF334155))
                    Text(text = "₹ 15,000 ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    Text(text = "next time after repayment on time", fontSize = 13.sp, color = Color(0xFF334155))
                }
            }

            // SECTION: LOAN INFORMATION (Matches Images 4 & 5)
            item {
                Column {
                    Text(
                        text = "Loan information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Row 1: Lending institutions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lending institutions", fontSize = 13.sp, color = Color(0xFF64748B))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("♟ ", fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text(providerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 2: Application Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Application Amount", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("₹ ${applicationAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 3: Expire Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expire Date", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(expireDate, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 4: Transfer receipt [Check]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Transfer receipt", fontSize = 13.sp, color = Color(0xFF64748B))
                                OutlinedButton(
                                    onClick = { showReceiptCheckModal = true },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                                ) {
                                    Text("Check", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 5: Bill has been settled [Fill in the UTR]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bill has been settled", fontSize = 13.sp, color = Color(0xFF64748B))
                                OutlinedButton(
                                    onClick = { currentStep = RepaymentStep.UTR_UPLOAD },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                                ) {
                                    Text("Fill in the UTR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "After completing the transfer, please fill in the UTR to settle the loan.",
                        fontSize = 12.sp,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            // SECTION: REPAYMENT INSTRUCTIONS (Matches Images 4 & 5)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0F2FE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Repayment Instructions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. The repayment link is valid for 10 minutes. Late transfers may fail.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "2. Make sure the transfer amount matches the repayment amount to avoid failure.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "3. Each order has a unique repayment reference. Do not reuse it to prevent errors.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // BOTTOM ACTION BUTTONS (Matches Image 5)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showConfirmModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_pay_full"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text(
                            text = "Pay in full to unlock ₹ 15,000",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            currentStep = RepaymentStep.GATEWAY
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                    ) {
                        Text(
                            text = "Pay ₹ 3,150 to postpone the payment",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "After completing the payment, you can refresh this link to check the payment status.",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // =========================================================================
        // STEP 2: GATEWAY PAYMENT SCREEN (Matches Image 7)
        // =========================================================================
        if (currentStep == RepaymentStep.GATEWAY) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Checkmark icon in light blue circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Repayment",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Complete your repayment",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Large Blue Amount Card (Matches Image 7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Repayment Amount", fontSize = 13.sp, color = Color(0xFFE0F2FE))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹ ${repayAmount.toInt()}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Payment required", fontSize = 12.sp, color = Color(0xFFE0F2FE))
                    }
                }
            }

            // Order Details Card (Matches Image 7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order Amount", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text("₹ ${repayAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment Type", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text("Full Repayment", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text("Pending", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            // UPI ID CARD WITH BLACK COPY BUTTON (Matches Image 7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("UPI ID", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = centralUpiId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Black Copy Button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UPI ID", centralUpiId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "UPI ID copied: $centralUpiId", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Copy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Procedural QR Canvas
                        UpiQrCanvas(payload = upiPayload, sizeDp = 170.dp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = centralReceiver,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // ACTION TO MOVE TO UTR SUBMISSION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { currentStep = RepaymentStep.UTR_UPLOAD },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Fill in the UTR & Submit Screenshot", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiPayload))
                                context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                            } catch (e: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", centralUpiId))
                                Toast.makeText(context, "UPI ID copied: $centralUpiId", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                    ) {
                        Text("Open in UPI App (GPay / PhonePe / Paytm)", fontSize = 13.sp)
                    }
                }
            }
        }

        // =========================================================================
        // STEP 3: UTR & SCREENSHOT PROOF SUBMISSION (Matches Image 8)
        // =========================================================================
        if (currentStep == RepaymentStep.UTR_UPLOAD) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Enter your 12-digit UTR and upload your Pay Loan ScreenShot Proof",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // UTR Field
                        Text(
                            text = "UTR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = utrInput,
                            onValueChange = { utrInput = it.trim().uppercase() },
                            placeholder = { Text("Enter 12 digit UTR", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("utr_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedBorderColor = Color(0xFF0284C7)
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Your ScreenShot Field
                        Text(
                            text = "Your ScreenShot",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom File Input Box (Matches "Choose File" / "no file selected" in Image 8)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .clickable { screenshotPickerLauncher.launch("image/*") }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text(
                                    text = "Choose File",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Text(
                                text = screenshotFileName,
                                fontSize = 12.sp,
                                color = if (attachedScreenshotUri != null) Color(0xFF0284C7) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button (Vibrant Blue, matches Image 8)
                        Button(
                            onClick = {
                                if (utrInput.length < 10) {
                                    Toast.makeText(context, "Please enter a valid 10-18 character UTR", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val loanId = targetLoan?.id ?: "LN-SE-5000"
                                viewModel.submitRepayment(
                                    loanId = loanId,
                                    installmentId = null,
                                    amount = repayAmount,
                                    utrNumber = utrInput,
                                    receiptUri = attachedScreenshotUri ?: "simulated://screenshot_${System.currentTimeMillis()}"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_submit_utr"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Back Button (Matches Image 8)
            item {
                OutlinedButton(
                    onClick = { currentStep = RepaymentStep.GATEWAY },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                ) {
                    Text("Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // MODAL DIALOG: REPAYMENT AMOUNT CONFIRMATION (Matches Image 6 exactly!)
    if (showConfirmModal) {
        Dialog(onDismissRequest = { showConfirmModal = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close "✕" Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { showConfirmModal = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }

                    Text(
                        text = "Repayment amount",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "₹  ${repayAmount.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "The amount can be raised to ₹ 15,000 after the settlement is completed on time.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // "Repayment now" Button
                    Button(
                        onClick = {
                            showConfirmModal = false
                            currentStep = RepaymentStep.GATEWAY
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_repayment_now"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Repayment now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Demo interface only. No real payment is processed.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Transfer Receipt Check dialog
    if (showReceiptCheckModal) {
        Dialog(onDismissRequest = { showReceiptCheckModal = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Official Transfer Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Lender: $providerName", fontSize = 13.sp)
                    Text("Due Amount: ₹ ${repayAmount.toInt()}", fontSize = 13.sp)
                    Text("Recipient UPI: $centralUpiId", fontSize = 13.sp, color = Color(0xFF0284C7))
                    Text("Settlement Account: $centralReceiver", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showReceiptCheckModal = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
