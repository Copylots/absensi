package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.AttendanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AttendanceViewModel"
    private val repository = (application as MainApplication).repository

    // --- Authentication State ---
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // --- Form State ---
    val formNama = MutableStateFlow("")
    val formShift = MutableStateFlow("")
    val formStatus = MutableStateFlow("")

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // --- History / Filters State ---
    val searchQuery = MutableStateFlow("")
    val filterStartDate = MutableStateFlow<String?>(null) // Format: "YYYY-MM-DD"
    val filterEndDate = MutableStateFlow<String?>(null)   // Format: "YYYY-MM-DD"

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // --- Filtered Records Flow ---
    // Reactively combines Room's allRecords Flow with local search & date filters!
    val filteredRecords: StateFlow<List<AttendanceRecord>> = combine(
        repository.allRecords,
        searchQuery,
        filterStartDate,
        filterEndDate
    ) { records, query, start, end ->
        records.filter { record ->
            val matchesQuery = query.isBlank() || record.nama.contains(query, ignoreCase = true)
            val matchesStart = start == null || record.tanggal >= start
            val matchesEnd = end == null || record.tanggal <= end
            matchesQuery && matchesStart && matchesEnd
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Authentication Actions ---
    fun prosesLogin(user: String, pass: String): Boolean {
        val trimmedUser = user.trim()
        val trimmedPass = pass.trim()
        return if (trimmedUser == "admin" && trimmedPass == "admin234") {
            _username.value = "Admin"
            _isAdmin.value = true
            _isLoggedIn.value = true
            true
        } else if (trimmedUser == "karyawan" && trimmedPass == "karyawan234") {
            _username.value = "Karyawan"
            _isAdmin.value = false
            _isLoggedIn.value = true
            true
        } else {
            false
        }
    }

    fun logout() {
        _username.value = ""
        _isAdmin.value = false
        _isLoggedIn.value = false
        // Clear forms
        formNama.value = ""
        formShift.value = ""
        formStatus.value = ""
        // Clear filters
        searchQuery.value = ""
        filterStartDate.value = null
        filterEndDate.value = null
    }

    // --- Form Submit Actions ---
    fun submitAbsensi(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val nama = formNama.value.trim()
        val shift = formShift.value.trim()
        val status = formStatus.value.trim()

        if (nama.isEmpty() || shift.isEmpty() || status.isEmpty()) {
            onError("Semua kolom form wajib diisi!")
            return
        }

        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val now = Date()

                val record = AttendanceRecord(
                    nama = nama,
                    shift = shift,
                    waktu = dateFormat.format(now),
                    tanggal = dateOnlyFormat.format(now),
                    status = status,
                    isSynced = false
                )

                // Save locally to Room (which also attempts instant sync)
                val isSynced = repository.insertRecord(record)

                // Clear input fields
                formNama.value = ""
                formShift.value = ""
                formStatus.value = ""

                if (isSynced) {
                    onSuccess("Data presensi ($status) berhasil dikirim ke Cloud Firebase.")
                } else {
                    onSuccess("Data presensi ($status) disimpan secara lokal (Offline mode).")
                }
            } catch (e: Exception) {
                onError("Gagal menyimpan data: ${e.message}")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // --- History Actions ---
    fun refreshHistory(onComplete: (Boolean) -> Unit) {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val success = repository.refreshRecords()
                onComplete(success)
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- PDF Export Action (High Fidelity Custom Drawing) ---
    fun exportToPdf(
        context: Context,
        records: List<AttendanceRecord>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (records.isEmpty()) {
            onError("Tidak ada data untuk dicetak!")
            return
        }

        viewModelScope.launch {
            try {
                val fileName = "Laporan_Absensi_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
                val document = PdfDocument()

                // Create a page info (A4 size is typically 595 x 842 points)
                val pageWidth = 595
                val pageHeight = 842
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Set up Paint objects for text & table borders
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val subtitlePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val headerTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val headerBgPaint = Paint().apply {
                    color = Color.rgb(0, 150, 136) // Standard Teal
                    style = Paint.Style.FILL
                }

                val cellPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val borderPaint = Paint().apply {
                    color = Color.LTGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                val linePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 1.5f
                }

                // 1. Draw Page Header
                canvas.drawText("LAPORAN DATA ABSENSI KARYAWAN", 40f, 50f, titlePaint)

                val startText = filterStartDate.value ?: "Semua"
                val endText = filterEndDate.value ?: "Semua"
                canvas.drawText("Periode: $startText s/d $endText", 40f, 75f, subtitlePaint)

                val printTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                canvas.drawText("Dicetak pada tanggal: $printTime", 40f, 90f, subtitlePaint)

                // Divider line
                canvas.drawLine(40f, 105f, (pageWidth - 40).toFloat(), 105f, linePaint)

                // 2. Setup Table Layout Parameters
                val startX = 40f
                var currentY = 130f
                val colWidths = floatArrayOf(30f, 140f, 110f, 120f, 115f) // Total = 515 (fits 595 page width with 40 margins)
                val headers = arrayOf("No", "Nama Karyawan", "Shift Kerja", "Waktu Absen", "Status")

                // Draw Table Headers
                var colX = startX
                for (i in headers.indices) {
                    val rectRight = colX + colWidths[i]
                    // Fill background
                    canvas.drawRect(colX, currentY, rectRight, currentY + 25f, headerBgPaint)
                    // Draw border
                    canvas.drawRect(colX, currentY, rectRight, currentY + 25f, borderPaint)
                    // Text align center or left
                    val textX = if (i == 0 || i == 4) colX + (colWidths[i] / 2) - (headerTextPaint.measureText(headers[i]) / 2) else colX + 8f
                    canvas.drawText(headers[i], textX, currentY + 17f, headerTextPaint)
                    colX += colWidths[i]
                }

                currentY += 25f

                // Draw Rows
                val sortedRecords = records.sortedBy { it.waktu }
                for (index in sortedRecords.indices) {
                    val record = sortedRecords[index]
                    val rowHeight = 25f

                    // Check if table overflows page, simple page overflow handling
                    if (currentY + rowHeight > pageHeight - 50f) {
                        break // For this simple implementation we clip at page bounds, fits ~25 records nicely!
                    }

                    colX = startX
                    val rowData = arrayOf(
                        (index + 1).toString(),
                        record.nama,
                        record.shift,
                        record.waktu,
                        record.status
                    )

                    for (i in rowData.indices) {
                        val rectRight = colX + colWidths[i]
                        // Draw cell border
                        canvas.drawRect(colX, currentY, rectRight, currentY + rowHeight, borderPaint)
                        // Cell Text
                        val text = rowData[i]
                        val textX = if (i == 0 || i == 4) colX + (colWidths[i] / 2) - (cellPaint.measureText(text) / 2) else colX + 8f
                        canvas.drawText(text, textX, currentY + 16f, cellPaint)
                        colX += colWidths[i]
                    }
                    currentY += rowHeight
                }

                document.finishPage(page)

                // 3. Save PDF to external Documents storage via MediaStore (supports Android 10+ safely)
                saveDocumentToMediaStore(context, document, fileName, "application/pdf")

                document.close()
                onSuccess("PDF Berhasil Dicetak!\n\nNama File:\n$fileName\n\nLokasi: Folder Documents/Laporan_Absensi")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export PDF: ${e.message}", e)
                onError("Gagal mengekspor PDF: ${e.message}")
            }
        }
    }

    // --- CSV Export Action ---
    fun exportToCsv(
        context: Context,
        records: List<AttendanceRecord>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (records.isEmpty()) {
            onError("Tidak ada data untuk dicetak!")
            return
        }

        viewModelScope.launch {
            try {
                val fileName = "Laporan_Absensi_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

                // Create CSV Data Content (with BOM prefix for Microsoft Excel compatibility)
                val sb = StringBuilder()
                sb.append('\ufeff') // Excel UTF-8 BOM
                sb.append("No;Nama Karyawan;Shift Kerja;Waktu Absen;Status Kehadiran\n")

                val sortedRecords = records.sortedBy { it.waktu }
                for (index in sortedRecords.indices) {
                    val record = sortedRecords[index]
                    sb.append("${index + 1};")
                    sb.append("${record.nama};")
                    sb.append("${record.shift};")
                    sb.append("${record.waktu};")
                    sb.append("${record.status}\n")
                }

                val csvContent = sb.toString()

                // Save to MediaStore (Documents/Laporan_Absensi)
                saveStringToMediaStore(context, csvContent, fileName, "text/csv")

                onSuccess("Excel (CSV) Berhasil Dicetak!\n\nNama File:\n$fileName\n\nLokasi: Folder Documents/Laporan_Absensi")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export CSV: ${e.message}", e)
                onError("Gagal mengekspor Excel: ${e.message}")
            }
        }
    }

    // --- MediaStore Document Saving Helpers (Safely handles Scoped Storage on Android 10+) ---
    private fun saveDocumentToPdfFileLegacy(document: PdfDocument, fileName: String): File {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(path, "Laporan_Absensi")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        document.writeTo(FileOutputStream(file))
        return file
    }

    private fun saveDocumentToMediaStore(context: Context, document: PdfDocument, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Laporan_Absensi")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outStream ->
                    if (outStream != null) {
                        document.writeTo(outStream)
                    } else {
                        throw Exception("Gagal membuka output stream.")
                    }
                }
            } else {
                throw Exception("Gagal membuat entri dokumen di Android MediaStore.")
            }
        } else {
            // Legacy saving for older Android versions
            saveDocumentToPdfFileLegacy(document, fileName)
        }
    }

    private fun saveStringToMediaStore(context: Context, content: String, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Laporan_Absensi")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outStream ->
                    if (outStream != null) {
                        outStream.write(content.toByteArray(Charsets.UTF_8))
                    } else {
                        throw Exception("Gagal membuka output stream.")
                    }
                }
            } else {
                throw Exception("Gagal membuat entri dokumen di Android MediaStore.")
            }
        } else {
            // Legacy saving for older Android versions
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(path, "Laporan_Absensi")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)
            FileOutputStream(file).use { outStream ->
                outStream.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
