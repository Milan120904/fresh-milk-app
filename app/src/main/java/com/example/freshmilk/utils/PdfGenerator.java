package com.example.freshmilk.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.ExtraCharge;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generates monthly bill PDFs using Android's native PdfDocument API.
 * No external library dependencies required.
 */
public class PdfGenerator {

    private static final String TAG = "PdfGenerator";

    // Page dimensions (A4 in points: 595 x 842)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;

    // Colors
    private static final int COLOR_PRIMARY = Color.rgb(25, 118, 210);
    private static final int COLOR_GRAY = Color.rgb(97, 97, 97);
    private static final int COLOR_DARK = Color.rgb(33, 33, 33);
    private static final int COLOR_PAID = Color.rgb(46, 125, 50);
    private static final int COLOR_UNPAID = Color.rgb(198, 40, 40);
    private static final int COLOR_HEADER_BG = Color.rgb(25, 118, 210);
    private static final int COLOR_ALT_ROW = Color.rgb(237, 246, 255);

    public static Uri generateMonthlyBill(Context context, String vendorName, Customer customer,
            List<DailyEntry> entries, List<ExtraCharge> extraCharges, int month, int year,
            double totalAmount, String paymentStatus) {

        if (customer == null) {
            Toast.makeText(context, "Customer data not available", Toast.LENGTH_SHORT).show();
            return null;
        }

        String[] monthNames = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        String monthName = monthNames[month - 1];

        String custName = customer.getName() != null ? customer.getName() : "Customer";
        String fileName = "FreshMilk_Bill_" + custName.replace(" ", "_")
                + "_" + monthName + "_" + year + ".pdf";

        PdfDocument pdfDocument = new PdfDocument();

        try {
            // Calculate pages needed
            int entriesPerPage = 20;
            int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / entriesPerPage));

            float y; // Current y position on page

            for (int page = 0; page < totalPages; page++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, page + 1).create();
                PdfDocument.Page pdfPage = pdfDocument.startPage(pageInfo);
                Canvas canvas = pdfPage.getCanvas();

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                y = MARGIN;

                if (page == 0) {
                    // ===== HEADER (First page only) =====

                    // App Title
                    paint.setColor(COLOR_PRIMARY);
                    paint.setTextSize(24);
                    paint.setFakeBoldText(true);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("Fresh Milk Delivery", PAGE_WIDTH / 2f, y + 24, paint);
                    y += 35;

                    // Subtitle
                    paint.setColor(COLOR_GRAY);
                    paint.setTextSize(13);
                    paint.setFakeBoldText(false);
                    canvas.drawText("Monthly Milk Delivery Bill", PAGE_WIDTH / 2f, y + 13, paint);
                    y += 25;

                    // Divider line
                    paint.setColor(COLOR_PRIMARY);
                    paint.setStrokeWidth(2);
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
                    y += 15;

                    // Bill period & generated date
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setColor(COLOR_DARK);
                    paint.setTextSize(11);
                    canvas.drawText("Bill Period: " + monthName + " " + year, MARGIN, y + 11, paint);
                    y += 18;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    canvas.drawText("Generated: " + sdf.format(new Date()), MARGIN, y + 11, paint);
                    y += 25;

                    // Vendor Details
                    if (vendorName != null && !vendorName.isEmpty()) {
                        paint.setColor(COLOR_PRIMARY);
                        paint.setTextSize(13);
                        paint.setFakeBoldText(true);
                        canvas.drawText("Vendor Details", MARGIN, y + 13, paint);
                        y += 20;

                        paint.setColor(COLOR_DARK);
                        paint.setTextSize(11);
                        paint.setFakeBoldText(false);
                        canvas.drawText("Name: " + vendorName, MARGIN, y + 11, paint);
                        y += 22;
                    }

                    // Customer Details
                    paint.setColor(COLOR_PRIMARY);
                    paint.setTextSize(13);
                    paint.setFakeBoldText(true);
                    canvas.drawText("Customer Details", MARGIN, y + 13, paint);
                    y += 20;

                    paint.setColor(COLOR_DARK);
                    paint.setTextSize(11);
                    paint.setFakeBoldText(false);
                    canvas.drawText("Name: " + safeStr(customer.getName()), MARGIN, y + 11, paint);
                    y += 16;
                    canvas.drawText("Mobile: " + safeStr(customer.getMobile()), MARGIN, y + 11, paint);
                    y += 16;
                    canvas.drawText("Address: " + safeStr(customer.getAddress()), MARGIN, y + 11, paint);
                    y += 16;
                    canvas.drawText("Milk Type: " + safeStr(customer.getMilkType()), MARGIN, y + 11, paint);
                    y += 16;
                    canvas.drawText("Rate: Rs." + String.format(Locale.getDefault(), "%.2f", customer.getRatePerLiter()) + " per Liter", MARGIN, y + 11, paint);
                    y += 25;

                    // Thin divider
                    paint.setColor(Color.LTGRAY);
                    paint.setStrokeWidth(1);
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
                    y += 10;
                }

                // ===== ENTRIES TABLE =====
                float[] colWidths = { 110, 80, 80, 80, 100 };
                String[] headers = { "Date", "Morning(L)", "Evening(L)", "Total(L)", "Amount(Rs)" };
                float tableX = MARGIN;

                // Draw table header (on every page)
                paint.setColor(COLOR_HEADER_BG);
                float headerHeight = 28;
                float totalWidth = 0;
                for (float w : colWidths) totalWidth += w;
                canvas.drawRect(tableX, y, tableX + totalWidth, y + headerHeight, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(10);
                paint.setFakeBoldText(true);
                paint.setTextAlign(Paint.Align.CENTER);
                float cx = tableX;
                for (int i = 0; i < headers.length; i++) {
                    canvas.drawText(headers[i], cx + colWidths[i] / 2f, y + 18, paint);
                    cx += colWidths[i];
                }
                y += headerHeight;

                // Draw entry rows for this page
                int startIdx = page * entriesPerPage;
                int endIdx = Math.min(startIdx + entriesPerPage, entries.size());
                float rowHeight = 24;

                paint.setFakeBoldText(false);
                paint.setTextSize(10);

                for (int i = startIdx; i < endIdx; i++) {
                    DailyEntry entry = entries.get(i);

                    // Alternate row background
                    if ((i - startIdx) % 2 == 1) {
                        paint.setColor(COLOR_ALT_ROW);
                        canvas.drawRect(tableX, y, tableX + totalWidth, y + rowHeight, paint);
                    }

                    // Row border
                    paint.setColor(Color.LTGRAY);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(0.5f);
                    canvas.drawRect(tableX, y, tableX + totalWidth, y + rowHeight, paint);
                    paint.setStyle(Paint.Style.FILL);

                    // Row data
                    paint.setColor(COLOR_DARK);
                    paint.setTextAlign(Paint.Align.CENTER);
                    cx = tableX;
                    canvas.drawText(safeStr(entry.getDate()), cx + colWidths[0] / 2f, y + 16, paint);
                    cx += colWidths[0];
                    canvas.drawText(String.format(Locale.getDefault(), "%.1f", entry.getMorningQty()), cx + colWidths[1] / 2f, y + 16, paint);
                    cx += colWidths[1];
                    canvas.drawText(String.format(Locale.getDefault(), "%.1f", entry.getEveningQty()), cx + colWidths[2] / 2f, y + 16, paint);
                    cx += colWidths[2];
                    canvas.drawText(String.format(Locale.getDefault(), "%.1f", entry.getTotalQty()), cx + colWidths[3] / 2f, y + 16, paint);
                    cx += colWidths[3];
                    canvas.drawText(String.format(Locale.getDefault(), "%.2f", entry.getDailyAmount()), cx + colWidths[4] / 2f, y + 16, paint);

                    y += rowHeight;
                }

                // On last page: draw extra charges and totals
                if (page == totalPages - 1) {
                    y += 20;

                    // Extra Charges
                    if (extraCharges != null && !extraCharges.isEmpty()) {
                        paint.setColor(COLOR_PRIMARY);
                        paint.setTextSize(12);
                        paint.setFakeBoldText(true);
                        paint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText("Extra Charges:", MARGIN, y + 12, paint);
                        y += 20;

                        paint.setColor(COLOR_DARK);
                        paint.setTextSize(10);
                        paint.setFakeBoldText(false);

                        double sumExtra = 0;
                        for (ExtraCharge ec : extraCharges) {
                            String desc = safeStr(ec.getDescription());
                            String amt = String.format(Locale.getDefault(), "Rs. %.2f", ec.getAmount());
                            canvas.drawText(desc, MARGIN + 20, y + 10, paint);
                            
                            paint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText(amt, PAGE_WIDTH - MARGIN, y + 10, paint);
                            paint.setTextAlign(Paint.Align.LEFT);
                            
                            sumExtra += ec.getAmount();
                            y += 16;
                        }

                        // Minor divider
                        y += 4;
                        paint.setColor(Color.LTGRAY);
                        paint.setStrokeWidth(0.5f);
                        canvas.drawLine(PAGE_WIDTH - MARGIN - 100, y, PAGE_WIDTH - MARGIN, y, paint);
                        y += 12;

                        paint.setColor(COLOR_DARK);
                        paint.setFakeBoldText(true);
                        paint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText("Extra Total: Rs. " + String.format(Locale.getDefault(), "%.2f", sumExtra), PAGE_WIDTH - MARGIN, y + 10, paint);
                        y += 25;
                    }

                    // Total Amount
                    paint.setColor(COLOR_PRIMARY);
                    paint.setTextSize(14);
                    paint.setFakeBoldText(true);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("Final Amount: Rs. " + String.format(Locale.getDefault(), "%.2f", totalAmount),
                            PAGE_WIDTH - MARGIN, y + 14, paint);
                    y += 25;

                    // Total Quantity
                    double totalQty = 0;
                    for (DailyEntry e : entries) totalQty += e.getTotalQty();
                    paint.setColor(COLOR_GRAY);
                    paint.setTextSize(11);
                    paint.setFakeBoldText(false);
                    canvas.drawText("Total Quantity: " + String.format(Locale.getDefault(), "%.1f", totalQty) + " Liters",
                            PAGE_WIDTH - MARGIN, y + 11, paint);
                    y += 22;

                    // Payment Status
                    boolean isPaid = "PAID".equals(paymentStatus);
                    paint.setColor(isPaid ? COLOR_PAID : COLOR_UNPAID);
                    paint.setTextSize(13);
                    paint.setFakeBoldText(true);
                    canvas.drawText("Payment Status: " + safeStr(paymentStatus),
                            PAGE_WIDTH - MARGIN, y + 13, paint);
                    y += 35;

                    // Footer
                    paint.setColor(Color.LTGRAY);
                    paint.setStrokeWidth(1);
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
                    y += 15;
                    paint.setColor(COLOR_GRAY);
                    paint.setTextSize(9);
                    paint.setFakeBoldText(false);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("Generated by Fresh Milk Delivery App", PAGE_WIDTH / 2f, y + 9, paint);
                }

                pdfDocument.finishPage(pdfPage);
            }

            // Save to Downloads/FreshMilk
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FreshMilk");

            Uri pdfUri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (pdfUri == null) {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show();
                pdfDocument.close();
                return null;
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(pdfUri);
            if (outputStream == null) {
                Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                pdfDocument.close();
                return null;
            }

            pdfDocument.writeTo(outputStream);
            outputStream.close();
            pdfDocument.close();

            Toast.makeText(context, "Bill saved to Downloads/FreshMilk/" + fileName, Toast.LENGTH_LONG).show();
            Log.d(TAG, "PDF generated successfully: " + fileName);
            return pdfUri;

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            pdfDocument.close();
            return null;
        }
    }

    private static String safeStr(String value) {
        return value != null ? value : "N/A";
    }
}
