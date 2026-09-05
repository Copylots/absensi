package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    viewModel: AttendanceViewModel,
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val nama by viewModel.formNama.collectAsState()
    val shift by viewModel.formShift.collectAsState()
    val status by viewModel.formStatus.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    var showResultDialog by remember { mutableStateOf(false) }
    var resultDialogTitle by remember { mutableStateOf("") }
    var resultDialogMessage by remember { mutableStateOf("") }

    var showAccessDeniedDialog by remember { mutableStateOf(false) }

    // Dropdown States
    var shiftExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val shifts = listOf("Shift 1 (07:00 - 19:00)", "Shift 2 (19:00 - 07:00)")
    val statuses = listOf("Masuk", "Pulang", "Sakit", "Izin")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Form Absensi Karyawan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .testTag("logout_button")
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log Out Icon",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Masukan Data Presensi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Employee Name OutlinedTextField
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { viewModel.formNama.value = it },
                        label = { Text("Nama Lengkap Karyawan") },
                        placeholder = { Text("Tulis nama lengkap Anda") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = "Name Icon"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_nama_input"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    )

                    // 2. Shift Selector (M3 Read-Only OutlinedTextField + DropdownMenu)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = shift,
                            onValueChange = {},
                            label = { Text("Pilih Shift Kerja") },
                            placeholder = { Text("Ketuk untuk memilih shift") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Shift Icon"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown Indicator"
                                )
                            },
                            readOnly = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_shift_dropdown_anchor")
                                .clickable { if (!isSubmitting) shiftExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        // This invisible overlay enables clicks on the entire TextField area
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { if (!isSubmitting) shiftExpanded = true }
                        )

                        DropdownMenu(
                            expanded = shiftExpanded,
                            onDismissRequest = { shiftExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .testTag("form_shift_dropdown_menu")
                        ) {
                            shifts.forEach { shiftOption ->
                                DropdownMenuItem(
                                    text = { Text(shiftOption) },
                                    onClick = {
                                        viewModel.formShift.value = shiftOption
                                        shiftExpanded = false
                                    },
                                    modifier = Modifier.testTag("shift_option_$shiftOption")
                                )
                            }
                        }
                    }

                    // 3. Status Selector (M3 Read-Only OutlinedTextField + DropdownMenu)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            label = { Text("Pilih Status Kehadiran") },
                            placeholder = { Text("Ketuk untuk memilih status") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Status Icon"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown Indicator"
                                )
                            },
                            readOnly = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_status_dropdown_anchor")
                                .clickable { if (!isSubmitting) statusExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        // This invisible overlay enables clicks on the entire TextField area
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { if (!isSubmitting) statusExpanded = true }
                        )

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .testTag("form_status_dropdown_menu")
                        ) {
                            statuses.forEach { statusOption ->
                                DropdownMenuItem(
                                    text = { Text(statusOption) },
                                    onClick = {
                                        viewModel.formStatus.value = statusOption
                                        statusExpanded = false
                                    },
                                    modifier = Modifier.testTag("status_option_$statusOption")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Submit Button
                    Button(
                        onClick = {
                            viewModel.submitAbsensi(
                                onSuccess = { message ->
                                    resultDialogTitle = "Simpan Data"
                                    resultDialogMessage = message
                                    showResultDialog = true
                                },
                                onError = { error ->
                                    resultDialogTitle = "Gagal"
                                    resultDialogMessage = error
                                    showResultDialog = true
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_presence_button"),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "SUBMIT PRESENSI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Navigation Button to History & PDF (Admin only / restriction checked on tap)
            Button(
                onClick = {
                    if (isAdmin) {
                        onNavigateToHistory()
                    } else {
                        showAccessDeniedDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(50.dp)
                    .testTag("pindah_ke_riwayat_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "LIHAT RIWAYAT & CETAK PDF →",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }

    // Success/Error Dialog for Submission
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Text(
                    text = resultDialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = { Text(text = resultDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showResultDialog = false },
                    modifier = Modifier.testTag("result_dialog_ok")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // Access Denied Dialog
    if (showAccessDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDeniedDialog = false },
            title = {
                Text(
                    text = "Akses Ditolak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text(text = "Menu riwayat dan cetak PDF hanya dapat diakses oleh Admin!") },
            confirmButton = {
                TextButton(
                    onClick = { showAccessDeniedDialog = false },
                    modifier = Modifier.testTag("access_denied_ok")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.error
        )
    }
}
