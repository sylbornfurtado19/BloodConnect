package com.bloodconnect.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top-level certificate generator function and object utility.
 * Generates an official Certificate of Appreciation as an A4 PDF document (595x842 pt).
 */
fun generate(
    context: Context,
    donorName: String,
    bloodGroup: String,
    hospital: String,
    dateMillis: Long,
    serial: String
): File {
    val cleanSerial = serial.take(8).uppercase().ifBlank { "DONATION" }
    val certDir = File(context.cacheDir, "certificates")
    if (!certDir.exists()) {
        certDir.mkdirs()
    }
    val outputFile = File(certDir, "BloodConnect_$cleanSerial.pdf")

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    // Format date
    val effectiveDate = if (dateMillis > 0L) dateMillis else System.currentTimeMillis()
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(effectiveDate))

    // Background
    canvas.drawColor(Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer Decorative Crimson Border
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(180, 25, 25)
    paint.strokeWidth = 4f
    canvas.drawRect(28f, 28f, 567f, 814f, paint)

    // Inner Elegant Border
    paint.strokeWidth = 1f
    paint.color = Color.rgb(200, 160, 80) // Gold accent
    canvas.drawRect(36f, 36f, 559f, 806f, paint)

    // Corner decorative accents
    val cornerSize = 18f
    canvas.drawRect(36f, 36f, 36f + cornerSize, 36f + cornerSize, paint)
    canvas.drawRect(559f - cornerSize, 36f, 559f, 36f + cornerSize, paint)
    canvas.drawRect(36f, 806f - cornerSize, 36f + cornerSize, 806f, paint)
    canvas.drawRect(559f - cornerSize, 806f - cornerSize, 559f, 806f, paint)

    // Header Emblem
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(210, 35, 35)
    canvas.drawCircle(595f / 2f, 100f, 28f, paint)

    paint.color = Color.WHITE
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("♥", 595f / 2f, 108f, paint)

    // Header: BloodConnect Organization
    paint.color = Color.rgb(180, 25, 25)
    paint.textSize = 14f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    canvas.drawText("BLOODCONNECT LIFESAVER NETWORK", 595f / 2f, 150f, paint)

    // Title: Certificate of Appreciation
    paint.color = Color.rgb(33, 33, 33)
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    canvas.drawText("Certificate of Appreciation", 595f / 2f, 195f, paint)

    // Decorative divider line
    paint.color = Color.rgb(200, 160, 80)
    paint.strokeWidth = 2f
    canvas.drawLine(160f, 220f, 435f, 220f, paint)

    // "PROUDLY PRESENTED TO"
    paint.color = Color.rgb(110, 110, 110)
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    canvas.drawText("THIS CERTIFICATE IS PROUDLY PRESENTED TO", 595f / 2f, 260f, paint)

    // Recipient Donor Name
    paint.color = Color.rgb(25, 25, 25)
    paint.textSize = 26f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    canvas.drawText(donorName, 595f / 2f, 310f, paint)

    // Name underline
    paint.color = Color.rgb(180, 25, 25)
    paint.strokeWidth = 1.5f
    val nameWidth = paint.measureText(donorName).coerceAtLeast(180f)
    canvas.drawLine((595f - nameWidth) / 2f, 325f, (595f + nameWidth) / 2f, 325f, paint)

    // Certification Line:
    // "This certifies that <name> donated blood (<group>) on <date> at <hospital>"
    val certLine = "This certifies that $donorName donated blood ($bloodGroup) on $dateStr at $hospital"
    paint.color = Color.rgb(55, 55, 55)
    paint.textSize = 15f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    drawCenteredMultilineText(canvas, certLine, 595f / 2f, 380f, paint, maxWidth = 480f)

    // Appreciation Text: "Thank you for saving lives. BloodConnect"
    paint.color = Color.rgb(180, 25, 25)
    paint.textSize = 17f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    canvas.drawText("Thank you for saving lives. BloodConnect", 595f / 2f, 520f, paint)

    // Gold Seal
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(200, 160, 80)
    paint.strokeWidth = 2f
    canvas.drawCircle(595f / 2f, 620f, 35f, paint)
    paint.strokeWidth = 1f
    canvas.drawCircle(595f / 2f, 620f, 30f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(200, 160, 80)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    canvas.drawText("OFFICIAL", 595f / 2f, 616f, paint)
    canvas.drawText("VERIFIED", 595f / 2f, 628f, paint)

    // Serial text (first 8 characters of requestId, upper case)
    paint.color = Color.rgb(100, 100, 100)
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    canvas.drawText("SERIAL: $cleanSerial", 595f / 2f, 700f, paint)

    // Date Issued Footer
    paint.color = Color.rgb(130, 130, 130)
    paint.textSize = 11f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    canvas.drawText("Issued on $dateStr • Emergency Blood Donation Network", 595f / 2f, 760f, paint)

    document.finishPage(page)

    FileOutputStream(outputFile).use { out ->
        document.writeTo(out)
    }
    document.close()

    return outputFile
}

/**
 * Utility object wrapping [generate] for call convenience.
 */
object CertificateGenerator {
    fun generate(
        context: Context,
        donorName: String,
        bloodGroup: String,
        hospital: String,
        dateMillis: Long,
        serial: String
    ): File = com.bloodconnect.utils.generate(context, donorName, bloodGroup, hospital, dateMillis, serial)
}

/**
 * Helper to draw centered text that wraps smoothly across multiple lines within [maxWidth].
 */
private fun drawCenteredMultilineText(
    canvas: Canvas,
    text: String,
    centerX: Float,
    startY: Float,
    paint: Paint,
    maxWidth: Float
) {
    if (paint.measureText(text) <= maxWidth) {
        canvas.drawText(text, centerX, startY, paint)
        return
    }

    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val test = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(test) <= maxWidth) {
            current = test
        } else {
            if (current.isNotEmpty()) lines.add(current)
            current = word
        }
    }
    if (current.isNotEmpty()) lines.add(current)

    var y = startY
    val lineHeight = paint.textSize * 1.5f
    for (line in lines) {
        canvas.drawText(line, centerX, y, paint)
        y += lineHeight
    }
}
