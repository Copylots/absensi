package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecord
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStart by viewModel.filterStartDate.collectAsState()
    val filterEnd by viewModel.filterEndDate.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filteredRecords by viewModel.filteredRecords.collectAsState()

    // DatePicker Dialog state
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "start" or "end"

    var showExportDialog by remember { mutableStateOf(false) }
    var exportDialogTitle by remember { mutableStateOf("") }
    var exportDialogMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Riwayat Absensi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Input Form"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshHistory { success ->
                                val msg = if (success) "Data berhasil diperbarui dari Firebase!" else "Gagal memuat data baru. Menampilkan cache lokal."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_history_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Perbarui Data")
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Search Bar OutlinedTextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Cari Nama Karyawan...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // 2. Date Filter Buttons (Mulai & Akhir)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDatePickerFor = "start" },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("start_date_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Start Date Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (filterStart != null) "Mulai: $filterStart" else "Mulai: Semua",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = { showDatePickerFor = "end" },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("end_date_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "End Date Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (filterEnd != null) "Akhir: $filterEnd" else "Akhir: Semua",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // If date filters are active, show a clear button
                if (filterStart != null || filterEnd != null) {
                    IconButton(
                        onClick = {
                            viewModel.filterStartDate.value = null
                            viewModel.filterEndDate.value = null
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("clear_date_filters_button")
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Dates")
                    }
                }
            }

            // 3. Compact Tabular Layout Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Karyawan / Shift", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text(text = "Waktu Absen", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), textAlign = TextAlign.Center)
                Text(text = "Status", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            // 4. Scrollable List of Records (Table Rows)
            Box(modifier = Modifier.weight(1f)) {
                if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No Data Icon",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada data yang cocok dengan filter!",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("attendance_table_list")
                    ) {
                        itemsIndexed(filteredRecords) { index, item ->
                            TableRowItem(item, index)
                        }
                    }
                }
            }

            // 5. EXPORT ACTIONS (PDF & EXCEL CSV)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDF Export Button
                Button(
                    onClick = {
                        viewModel.exportToPdf(
                            context = context,
                            records = filteredRecords,
                            onSuccess = { path ->
                                exportDialogTitle = "Berhasil"
                                exportDialogMessage = path
                                showExportDialog = true
                            },
                            onError = { error ->
                                exportDialogTitle = "Error"
                                exportDialogMessage = error
                                showExportDialog = true
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)), // Rich Dark Red for PDF
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "PDF Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CETAK PERIODE KE PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Excel Export Button
                Button(
                    onClick = {
                        viewModel.exportToCsv(
                            context = context,
                            records = filteredRecords,
                            onSuccess = { path ->
                                exportDialogTitle = "Berhasil"
                                exportDialogMessage = path
                                showExportDialog = true
                            },
                            onError = { error ->
                                exportDialogTitle = "Error"
                                exportDialogMessage = error
                                showExportDialog = true
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_excel_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)), // Rich Emerald Green for Excel
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Excel Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CETAK PERIODE KE EXCEL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Back Button Link
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("history_back_button")
                ) {
                    Text(
                        text = "← KEMBALI KE INPUT FORM",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Modern Material 3 Date Picker Dialog Integration
    if (showDatePickerFor != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            // Convert UTC millis to "yyyy-MM-dd"
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val dateStr = format.format(Date(selectedMillis))
                            if (showDatePickerFor == "start") {
                                viewModel.filterStartDate.value = dateStr
                            } else {
                                viewModel.filterEndDate.value = dateStr
                            }
                        }
                        showDatePickerFor = null
                    },
                    modifier = Modifier.testTag("datepicker_confirm_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Success/Error Alert Dialog for Exports
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = exportDialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = { Text(text = exportDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    modifier = Modifier.testTag("export_dialog_ok")
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
}

@Composable
fun TableRowItem(item: AttendanceRecord, index: Int) {
    val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val statusColor = when (item.status) {
        "Masuk" -> Color(0xFF2E7D32)
        "Pulang" -> Color(0xFF1565C0)
        "Sakit" -> Color(0xFFEF6C00)
        "Izin" -> Color(0xFFAD1457)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name & Shift Info
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = item.nama,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.shift,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Timestamp / Time Taken
        Text(
            text = item.waktu,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.8f),
            textAlign = TextAlign.Center
        )

        // Status Badge with Rounded Background
        Box(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End)
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
