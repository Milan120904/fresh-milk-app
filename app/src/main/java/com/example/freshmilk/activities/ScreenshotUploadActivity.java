package com.example.freshmilk.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.freshmilk.databinding.ActivityScreenshotUploadBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScreenshotUploadActivity extends AppCompatActivity {

    private static final String TAG = "ScreenshotUpload";
    private static final String[] MONTH_NAMES = {"January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December"};

    private ActivityScreenshotUploadBinding binding;
    private FirebaseHelper firebaseHelper;

    private String customerId;
    private String vendorId;
    private String customerName;
    private double amount;
    private int month, year;
    private String existingPaymentId;

    private Uri selectedImageUri;
    private Uri cameraImageUri;

    // Gallery picker
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    showImagePreview();
                }
            }
    );

    // Camera capture
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    showImagePreview();
                }
            }
    );

    // Permission request
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScreenshotUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        // Get intent data
        customerId = getIntent().getStringExtra("customerId");
        vendorId = getIntent().getStringExtra("vendorId");
        customerName = getIntent().getStringExtra("customerName");
        amount = getIntent().getDoubleExtra("amount", 0);
        month = getIntent().getIntExtra("month", 1);
        year = getIntent().getIntExtra("year", 2026);
        existingPaymentId = getIntent().getStringExtra("paymentId");

        setupUI();
        setupClickListeners();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        binding.tvMonthYear.setText(MONTH_NAMES[month - 1] + " " + year);
    }

    private void setupClickListeners() {
        binding.btnCamera.setOnClickListener(v -> checkCameraPermission());
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnUpload.setOnClickListener(v -> uploadScreenshot());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoFile = new File(getCacheDir(), "payment_screenshot_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera", e);
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void showImagePreview() {
        if (selectedImageUri != null) {
            binding.ivPreview.setPadding(0, 0, 0, 0);
            binding.ivPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            binding.ivPreview.setImageURI(selectedImageUri);
            binding.tvImageHint.setText("Image selected ✓ — Tap Upload to submit");
            binding.tvImageHint.setTextColor(getColor(com.example.freshmilk.R.color.status_paid));
            binding.btnUpload.setEnabled(true);
        }
    }

    private void uploadScreenshot() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        binding.layoutProgress.setVisibility(View.VISIBLE);
        binding.btnUpload.setEnabled(false);
        binding.btnCamera.setEnabled(false);
        binding.btnGallery.setEnabled(false);
        binding.tvProgressText.setText("Uploading... 0%");

        // Upload to Cloudinary
        com.example.freshmilk.utils.CloudinaryHelper.uploadPaymentScreenshot(
                this, selectedImageUri, customerId,
                new com.example.freshmilk.utils.CloudinaryHelper.UploadCallback() {
                    @Override
                    public void onProgress(int percentage) {
                        binding.progressBar.setProgress(percentage);
                        binding.tvProgressText.setText(String.format(Locale.getDefault(),
                                "Uploading... %d%%", percentage));
                    }

                    @Override
                    public void onSuccess(String secureUrl) {
                        savePaymentWithScreenshot(secureUrl);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                }
        );
    }

    private void savePaymentWithScreenshot(String screenshotUrl) {
        if (existingPaymentId != null) {
            // Update existing payment
            Map<String, Object> updates = new HashMap<>();
            updates.put("screenshotUrl", screenshotUrl);
            updates.put("status", Payment.STATUS_PROCESSING);
            updates.put("vendorId", vendorId);

            firebaseHelper.updatePaymentFields(existingPaymentId, updates)
                    .addOnSuccessListener(v -> showSuccess())
                    .addOnFailureListener(e -> showError("Failed to save: " + e.getMessage()));
        } else {
            // Check for existing payment first
            firebaseHelper.getPaymentByCustomerAndMonth(customerId, month, year)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Update existing
                            String docId = querySnapshot.getDocuments().get(0).getId();
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("screenshotUrl", screenshotUrl);
                            updates.put("status", Payment.STATUS_PROCESSING);
                            updates.put("vendorId", vendorId);

                            firebaseHelper.updatePaymentFields(docId, updates)
                                    .addOnSuccessListener(v -> showSuccess())
                                    .addOnFailureListener(e -> showError("Failed to save: " + e.getMessage()));
                        } else {
                            // Create new payment
                            Payment payment = new Payment(customerId, customerName != null ? customerName : "",
                                    firebaseHelper.getCurrentUserId(), month, year, amount);
                            payment.setVendorId(vendorId);
                            payment.setStatus(Payment.STATUS_PROCESSING);
                            payment.setScreenshotUrl(screenshotUrl);

                            firebaseHelper.addPayment(payment)
                                    .addOnSuccessListener(docRef -> showSuccess())
                                    .addOnFailureListener(e -> showError("Failed to save: " + e.getMessage()));
                        }
                    });
        }
    }

    private void showSuccess() {
        binding.layoutProgress.setVisibility(View.GONE);
        binding.cardStatus.setVisibility(View.VISIBLE);
        binding.tvStatusTitle.setText("✅ Screenshot Uploaded");
        binding.tvStatusMessage.setText("Your vendor will verify the payment shortly");
        binding.btnUpload.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Log.e(TAG, message);
        binding.layoutProgress.setVisibility(View.GONE);
        binding.btnUpload.setEnabled(true);
        binding.btnCamera.setEnabled(true);
        binding.btnGallery.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
