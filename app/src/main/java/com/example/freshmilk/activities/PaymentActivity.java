package com.example.freshmilk.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityPaymentBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "PaymentActivity";
    private static final String RAZORPAY_KEY = "rzp_test_SbH7xsDWW38hOz"; // Replace with your Razorpay Key

    private ActivityPaymentBinding binding;
    private FirebaseHelper firebaseHelper;

    private String customerId;
    private String vendorId;
    private double amount;
    private int month, year;
    private String vendorUpiId;
    private String vendorName = "";
    private String customerName = "";
    private String existingPaymentId = null;

    private static final String[] MONTH_NAMES = {"January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December"};

    private final ActivityResultLauncher<Intent> upiLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == Activity.RESULT_CANCELED) {
                    handleUpiResponse(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize Razorpay
        Checkout.preload(getApplicationContext());

        // Get intent data
        customerId = getIntent().getStringExtra("customerId");
        vendorId = getIntent().getStringExtra("vendorId");
        amount = getIntent().getDoubleExtra("amount", 0);
        month = getIntent().getIntExtra("month", 1);
        year = getIntent().getIntExtra("year", 2026);
        customerName = getIntent().getStringExtra("customerName");
        if (customerName == null) customerName = "";

        setupUI();
        setupClickListeners();
        loadVendorData();
        checkExistingPayment();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        binding.tvMonthYear.setText(MONTH_NAMES[month - 1] + " " + year);
    }

    private void setupClickListeners() {
        binding.btnPayUpi.setOnClickListener(v -> initiateUpiPayment());
        binding.btnPayQr.setOnClickListener(v -> confirmQrPayment());
        binding.btnPayCard.setOnClickListener(v -> initiateRazorpayPayment());
        binding.btnUploadScreenshot.setOnClickListener(v -> openScreenshotUpload());
    }

    private void loadVendorData() {
        if (vendorId == null) {
            binding.tvVendorUpi.setText("Vendor not linked");
            binding.tvQrError.setVisibility(View.VISIBLE);
            binding.tvQrError.setText("Cannot generate QR: No vendor linked");
            disablePaymentButtons();
            return;
        }

        firebaseHelper.getVendorById(vendorId)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        vendorName = doc.getString("name");
                        vendorUpiId = doc.getString("upiId");
                        if (vendorName == null) vendorName = "";

                        if (vendorUpiId != null && !vendorUpiId.isEmpty()) {
                            binding.tvVendorUpi.setText(vendorUpiId);
                            generateQrCode();
                        } else {
                            binding.tvVendorUpi.setText("UPI ID not set by vendor");
                            binding.tvQrError.setVisibility(View.VISIBLE);
                            binding.tvQrError.setText("Vendor has not set UPI ID");
                            binding.btnPayUpi.setEnabled(false);
                            binding.btnPayQr.setEnabled(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load vendor data", e);
                    binding.tvVendorUpi.setText("Failed to load vendor data");
                });
    }

    private void checkExistingPayment() {
        firebaseHelper.getPaymentByCustomerAndMonth(customerId, month, year)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        var doc = querySnapshot.getDocuments().get(0);
                        Payment payment = doc.toObject(Payment.class);
                        existingPaymentId = doc.getId();

                        if (payment != null) {
                            String status = payment.getStatus();
                            if (Payment.STATUS_PAID.equals(status) || Payment.STATUS_PROCESSING.equals(status)) {
                                // Already paid or processing — show status and disable buttons
                                showPaymentSuccess(payment.getTransactionId(), status);
                                disablePaymentButtons();
                            } else if (Payment.STATUS_PENDING.equals(status) && payment.getTransactionId() != null) {
                                // Payment done but no screenshot yet
                                showPaymentSuccess(payment.getTransactionId(), status);
                            }
                        }
                    }
                });
    }

    private void disablePaymentButtons() {
        binding.btnPayUpi.setEnabled(false);
        binding.btnPayQr.setEnabled(false);
        binding.btnPayCard.setEnabled(false);
    }

    // ==================== QR CODE ====================

    private void generateQrCode() {
        binding.progressQr.setVisibility(View.VISIBLE);
        binding.tvQrError.setVisibility(View.GONE);

        try {
            String upiString = String.format(Locale.US,
                    "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=FreshMilk_%s_%d",
                    vendorUpiId, vendorName, amount,
                    MONTH_NAMES[month - 1], year);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    upiString, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ?
                            getColor(R.color.on_surface) : getColor(R.color.surface));
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap);
            binding.progressQr.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "QR generation failed", e);
            binding.progressQr.setVisibility(View.GONE);
            binding.tvQrError.setVisibility(View.VISIBLE);
            binding.tvQrError.setText("Failed to generate QR code");
        }
    }

    // ==================== UPI INTENT ====================

    private void initiateUpiPayment() {
        if (vendorUpiId == null || vendorUpiId.isEmpty()) {
            Toast.makeText(this, "Vendor UPI ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String upiUri = String.format(Locale.US,
                "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=FreshMilk_%s_%d",
                vendorUpiId, Uri.encode(vendorName), amount,
                MONTH_NAMES[month - 1], year);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(upiUri));

        Intent chooser = Intent.createChooser(intent, "Pay with UPI");
        if (intent.resolveActivity(getPackageManager()) != null) {
            upiLauncher.launch(chooser);
        } else {
            Toast.makeText(this, "No UPI apps installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUpiResponse(Intent data) {
        if (data != null) {
            String response = data.getStringExtra("response");
            Log.d(TAG, "UPI Response: " + response);

            if (response != null) {
                String status = getUpiStatus(response);
                String txnId = getUpiTransactionId(response);

                if ("SUCCESS".equalsIgnoreCase(status) || "success".equals(status)) {
                    savePayment(Payment.METHOD_UPI, txnId != null ? txnId : generateLocalTxnId());
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
                } else if ("SUBMITTED".equalsIgnoreCase(status)) {
                    Toast.makeText(this, "Payment is pending. Please check your UPI app.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Payment failed or cancelled", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Some UPI apps don't return response — ask user to confirm
                showManualConfirmDialog(Payment.METHOD_UPI);
            }
        } else {
            showManualConfirmDialog(Payment.METHOD_UPI);
        }
    }

    private String getUpiStatus(String response) {
        for (String param : response.split("&")) {
            if (param.startsWith("Status=") || param.startsWith("status=")) {
                return param.split("=")[1];
            }
        }
        return null;
    }

    private String getUpiTransactionId(String response) {
        for (String param : response.split("&")) {
            if (param.startsWith("txnId=")) {
                return param.split("=")[1];
            }
        }
        return null;
    }

    // ==================== QR CONFIRMATION ====================

    private void confirmQrPayment() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm QR Payment")
                .setMessage("Have you completed the payment by scanning the QR code?")
                .setPositiveButton("Yes, I paid", (dialog, which) -> {
                    savePayment(Payment.METHOD_QR, generateLocalTxnId());
                    Toast.makeText(this, "Payment recorded!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Not yet", null)
                .show();
    }

    // ==================== RAZORPAY ====================

    private void initiateRazorpayPayment() {
        if (RAZORPAY_KEY.equals("YOUR_RAZORPAY_KEY_ID")) {
            Toast.makeText(this, "Razorpay key not configured. Please contact vendor.", Toast.LENGTH_LONG).show();
            return;
        }

        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_KEY);

        try {
            JSONObject options = new JSONObject();
            options.put("name", "FreshMilk");
            options.put("description", "Milk Payment - " + MONTH_NAMES[month - 1] + " " + year);
            options.put("currency", "INR");
            options.put("amount", (int) (amount * 100)); // Amount in paise

            JSONObject prefill = new JSONObject();
            prefill.put("contact", "");
            prefill.put("email", "");
            options.put("prefill", prefill);

            JSONObject theme = new JSONObject();
            theme.put("color", "#1976D2");
            options.put("theme", theme);

            checkout.open(this, options);

        } catch (Exception e) {
            Log.e(TAG, "Razorpay open failed", e);
            Toast.makeText(this, "Payment initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Log.d(TAG, "Razorpay Success: " + razorpayPaymentID);
        savePayment(Payment.METHOD_CARD, razorpayPaymentID);
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.e(TAG, "Razorpay Error: " + code + " — " + response);
        Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show();
    }

    // ==================== SAVE PAYMENT ====================

    private void savePayment(String method, String transactionId) {
        // Check for duplicate
        firebaseHelper.getPaymentByCustomerAndMonth(customerId, month, year)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Update existing payment
                        var doc = querySnapshot.getDocuments().get(0);
                        Payment existingPayment = doc.toObject(Payment.class);

                        if (existingPayment != null) {
                            String exStatus = existingPayment.getStatus();
                            if (Payment.STATUS_PAID.equals(exStatus) || Payment.STATUS_PROCESSING.equals(exStatus)) {
                                Toast.makeText(this, "Payment already completed for this month!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", Payment.STATUS_PENDING);
                        updates.put("paidAmount", amount);
                        updates.put("paymentMethod", method);
                        updates.put("transactionId", transactionId);
                        updates.put("paidDate", new Timestamp(new Date()));
                        updates.put("vendorId", vendorId);

                        firebaseHelper.updatePaymentFields(doc.getId(), updates)
                                .addOnSuccessListener(v -> showPaymentSuccess(transactionId, Payment.STATUS_PENDING))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update payment", e);
                                    Toast.makeText(this, "Failed to save payment", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Create new payment
                        Payment payment = new Payment(customerId, customerName,
                                firebaseHelper.getCurrentUserId(), month, year, amount);
                        payment.setVendorId(vendorId);
                        payment.setStatus(Payment.STATUS_PENDING);
                        payment.setPaidAmount(amount);
                        payment.setPaymentMethod(method);
                        payment.setTransactionId(transactionId);
                        payment.setPaidDate(new Timestamp(new Date()));

                        firebaseHelper.addPayment(payment)
                                .addOnSuccessListener(docRef -> {
                                    existingPaymentId = docRef.getId();
                                    showPaymentSuccess(transactionId, Payment.STATUS_PENDING);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save payment", e);
                                    Toast.makeText(this, "Failed to save payment", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void showPaymentSuccess(String transactionId, String status) {
        binding.cardPaymentStatus.setVisibility(View.VISIBLE);
        
        if (Payment.STATUS_PROCESSING.equals(status) || Payment.STATUS_PAID.equals(status)) {
            binding.tvStatusTitle.setText("✅ Payment Processing/Paid");
            binding.btnUploadScreenshot.setVisibility(View.GONE);
        } else {
            binding.btnUploadScreenshot.setVisibility(View.VISIBLE);
        }

        if (transactionId != null && !transactionId.isEmpty()) {
            binding.tvTransactionId.setText("Txn ID: " + transactionId);
        } else {
            binding.tvTransactionId.setText("Payment recorded");
        }
        disablePaymentButtons();
    }

    private void openScreenshotUpload() {
        Intent intent = new Intent(this, ScreenshotUploadActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("vendorId", vendorId);
        intent.putExtra("amount", amount);
        intent.putExtra("month", month);
        intent.putExtra("year", year);
        intent.putExtra("customerName", customerName);
        if (existingPaymentId != null) {
            intent.putExtra("paymentId", existingPaymentId);
        }
        startActivity(intent);
    }

    private void showManualConfirmDialog(String method) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage("Did the payment complete successfully?")
                .setPositiveButton("Yes", (d, w) -> {
                    savePayment(method, generateLocalTxnId());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private String generateLocalTxnId() {
        return "FM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
