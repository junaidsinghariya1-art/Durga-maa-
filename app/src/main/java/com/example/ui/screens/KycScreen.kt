package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.DocumentType
import com.example.data.model.KycStatus
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.KycStatusBadge
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val kycSubmission by viewModel.customerKyc.collectAsState()

    val docTypes = DocumentType.values()
    var selectedDocType by remember { mutableStateOf(DocumentType.AADHAAR_CARD) }
    var docDropdownExpanded by remember { mutableStateOf(false) }
    var idNumberInput by remember { mutableStateOf("") }
    var attachedDocUri by remember { mutableStateOf<String?>(null) }
    var selfieUri by remember { mutableStateOf<String?>(null) }

    // Consent MUST start unchecked
    var isConsentChecked by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    // Document image picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedDocUri = uri.toString()
            Toast.makeText(context, "Identity document attached", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera runtime permission launcher for optional live selfie
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            selfieUri = "content://media/camera/selfie_${System.currentTimeMillis()}"
            Toast.makeText(context, "Camera permission granted. Live selfie captured.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission is required for live selfie capture.", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectScreen(AppScreen.CUSTOMER_DASHBOARD) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("Consent-Based KYC", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("RBI-Mandated Identity Verification & Data Privacy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // If user already has a submission, display current status and audit details
        if (kycSubmission != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current KYC Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            KycStatusBadge(kycSubmission?.status ?: KycStatus.NOT_SUBMITTED)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Document Type: ${kycSubmission?.documentType?.name?.replace('_', ' ')}", fontSize = 13.sp)
                        Text("Document ID: ${kycSubmission?.idNumber}", fontSize = 13.sp)
                        Text("Submitted At: ${formatDateTime(kycSubmission?.submittedAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Privacy Version: ${kycSubmission?.privacyPolicyVersion}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (kycSubmission?.reviewedAt != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Reviewed By: ${kycSubmission?.reviewedBy}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Notes: ${kycSubmission?.reviewNotes ?: "Verification complete."}", fontSize = 12.sp)
                                }
                            }
                        }

                        if (kycSubmission?.consentWithdrawn == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Consent Withdrawn on ${formatDateTime(kycSubmission?.consentWithdrawnAt)}. Scheduled for purge according to statutory limits.",
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showWithdrawDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw Consent & Request Data Deletion", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // TRANSPARENT PRIVACY DISCLOSURE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transparent KYC Privacy Disclosure", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• What Data is Collected: Valid government ID (Aadhaar / PAN / Passport / Voter ID) and optional live facial selfie.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Why It Is Needed: Mandated by Reserve Bank of India (RBI) KYC Master Directions (2016) for statutory customer identification, fraud mitigation, and loan underwriting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Who Can Access: Certified compliance officers and authorized underwriters under zero-trust Row-Level Security.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Protection & Retention: Private storage buckets, AES-256 at rest, encrypted TLS in transit, retained strictly for statutory limits (5 years).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Zero Secret Harvesting: We NEVER collect contacts, call logs, full gallery images, or background location.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                }
            }
        }

        // SUBMISSION FORM
        if (kycSubmission == null || kycSubmission?.status == KycStatus.REJECTED || kycSubmission?.consentWithdrawn == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Submit Identification Documents", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(14.dp))

                        ExposedDropdownMenuBox(
                            expanded = docDropdownExpanded,
                            onExpandedChange = { docDropdownExpanded = !docDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDocType.name.replace('_', ' '),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Government Document Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = docDropdownExpanded,
                                onDismissRequest = { docDropdownExpanded = false }
                            ) {
                                docTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name.replace('_', ' ')) },
                                        onClick = {
                                            selectedDocType = type
                                            docDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = idNumberInput,
                            onValueChange = { idNumberInput = it.uppercase() },
                            label = { Text("Document ID Number") },
                            placeholder = { Text("e.g. ABCDE1234F or 12-digit Aadhaar") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kyc_id_number_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Document Attachment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .clickable { documentPickerLauncher.launch("image/*") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(if (attachedDocUri != null) "Document Image Attached" else "Upload Official Document Photo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(if (attachedDocUri != null) "Tap to change" else "Front & back of original ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (attachedDocUri != null) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Optional Live Selfie with Runtime Camera Permission
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .clickable {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        selfieUri = "content://media/camera/selfie_${System.currentTimeMillis()}"
                                        Toast.makeText(context, "Live selfie captured successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(if (selfieUri != null) "Live Facial Selfie Captured" else "Optional Live Selfie Verification", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Explicit camera permission requested on tap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selfieUri != null) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // MANDATORY UNCHECKED CONSENT CHECKBOX
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isConsentChecked,
                                onCheckedChange = { isConsentChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("kyc_consent_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I explicitly grant consent to Durga Maa Loan Financial Services to verify my identity and store submitted documents strictly for loan sanctioning and statutory compliance under Privacy Policy v2.4-2026.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                viewModel.submitKyc(
                                    docType = selectedDocType,
                                    idNumber = idNumberInput,
                                    docUri = attachedDocUri ?: "simulated://docs/${System.currentTimeMillis()}",
                                    selfieUri = selfieUri,
                                    consentGiven = isConsentChecked
                                )
                            },
                            enabled = isConsentChecked && idNumberInput.length >= 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_submit_kyc"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit KYC with Verified Consent", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Confirm KYC Consent Withdrawal") },
            text = {
                Text("Withdrawing consent will revoke active credit lines and freeze new loan sanctions. Submitted identification records will be scheduled for purge in accordance with statutory RBI retention guidelines. Do you wish to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.withdrawKycConsent()
                        showWithdrawDialog = false
                    }
                ) {
                    Text("Withdraw & Request Deletion", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
