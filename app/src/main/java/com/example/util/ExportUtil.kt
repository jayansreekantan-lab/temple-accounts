package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entities.TransactionEntity
import com.example.model.CashBookEntry
import com.example.model.BankBookEntry
import com.example.model.TempleConstants
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtil {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())

    // Share Text or CSV
    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    // Export CSV / Excel for Reports or Cash Book
    fun exportToExcelCsv(context: Context, fileName: String, csvContent: String) {
        try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "$fileName.csv")
            
            // Write UTF-8 BOM (\uFEFF) so Excel opens Unicode/Malayalam cleanly
            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Excel / CSV"))
        } catch (e: Exception) {
            // Fallback to text share
            shareText(context, fileName, csvContent)
        }
    }

    // Generate and share PDF Receipt
    fun generateAndShareReceiptPdf(context: Context, tx: TransactionEntity) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(400, 560, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                color = Color.rgb(122, 28, 36)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valuePaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
            }

            // Header Border
            paint.color = Color.rgb(201, 139, 20)
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(12f, 12f, 388f, 548f, paint)

            canvas.drawText(TempleConstants.TEMPLE_NAME, 200f, 40f, titlePaint)
            canvas.drawText(TempleConstants.TEMPLE_LOCATION, 200f, 58f, subPaint)

            paint.color = Color.rgb(122, 28, 36)
            paint.style = Paint.Style.FILL
            canvas.drawRect(100f, 72f, 300f, 96f, paint)
            
            val badgePaint = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val badgeText = if (tx.type == "INCOME") "രസീത് (RECEIPT)" else "പെയ്‌മെന്റ് വൗച്ചർ (VOUCHER)"
            canvas.drawText(badgeText, 200f, 88f, badgePaint)

            var y = 130f
            fun drawRow(label: String, value: String) {
                canvas.drawText(label, 30f, y, labelPaint)
                canvas.drawText(value, 180f, y, valuePaint)
                y += 28f
            }

            drawRow("Number:", tx.voucherOrReceiptNo)
            drawRow("Date:", tx.dateFormatted)
            drawRow(if (tx.type == "INCOME") "Received From:" else "Paid To:", tx.partyName)
            drawRow("Category:", tx.category)
            if (!tx.festivalName.isNullOrBlank()) {
                drawRow("Festival:", tx.festivalName)
            }
            drawRow("Payment Mode:", tx.paymentMode)
            if (tx.notes.isNotBlank()) {
                drawRow("Notes:", tx.notes)
            }

            // Amount Box
            y += 10f
            paint.color = Color.rgb(245, 235, 220)
            paint.style = Paint.Style.FILL
            canvas.drawRect(30f, y, 370f, y + 45f, paint)
            
            val amtLabelPaint = Paint().apply {
                color = Color.rgb(122, 28, 36)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Amount: ₹${String.format(Locale.US, "%,.2f", tx.amount)}", 45f, y + 28f, amtLabelPaint)

            // Signatures
            y += 90f
            canvas.drawText("Prepared By: ${tx.createdBy.ifEmpty { "Treasurer" }}", 30f, y, subPaint)
            canvas.drawText("Authorized Signatory", 280f, y, subPaint)

            pdfDocument.finishPage(page)

            val cacheDir = File(context.cacheDir, "receipts")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${tx.voucherOrReceiptNo}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "Receipt ${tx.voucherOrReceiptNo}")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Receipt PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Receipt PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
