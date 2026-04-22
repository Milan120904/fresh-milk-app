package com.example.freshmilk.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.databinding.ActivityAddExtraChargeBinding;
import com.example.freshmilk.models.ExtraCharge;
import com.example.freshmilk.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddExtraChargeActivity extends AppCompatActivity {

    private ActivityAddExtraChargeBinding binding;
    private FirebaseHelper firebaseHelper;
    private Calendar calendar;

    private String customerId;
    private String vendorId;
    private String customerName;
    private String chargeId; // if editing

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExtraChargeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        calendar = Calendar.getInstance();

        customerId = getIntent().getStringExtra("customerId");
        vendorId = getIntent().getStringExtra("vendorId");
        customerName = getIntent().getStringExtra("customerName");

        // Edit support
        chargeId = getIntent().getStringExtra("chargeId");
        if (chargeId != null && !chargeId.isEmpty()) {
            isEditMode = true;
            binding.btnSave.setText("Update Extra Charge");
            binding.etAmount.setText(String.valueOf(getIntent().getDoubleExtra("amount", 0)));
            binding.etDescription.setText(getIntent().getStringExtra("description"));
            binding.etDate.setText(getIntent().getStringExtra("date"));
        } else {
            updateDateLabel();
        }

        if (customerName != null) {
            binding.tvCustomerName.setText("Customer: " + customerName);
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.etDate.setOnClickListener(v -> showDatePicker());

        binding.btnSave.setOnClickListener(v -> saveCharge());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        binding.etDate.setText(sdf.format(calendar.getTime()));
    }

    private void saveCharge() {
        String amountStr = binding.etAmount.getText() != null ? binding.etAmount.getText().toString().trim() : "";
        String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";
        String date = binding.etDate.getText() != null ? binding.etDate.getText().toString().trim() : "";

        if (amountStr.isEmpty()) {
            binding.etAmount.setError("Required");
            return;
        }
        if (description.isEmpty()) {
            binding.etDescription.setError("Required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.etAmount.setError("Invalid amount");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        // Extract month format yyyy-MM
        String month = date.substring(0, 7);

        ExtraCharge charge = new ExtraCharge(customerId, vendorId, amount, description, date, month);

        if (isEditMode) {
            charge.setChargeId(chargeId);
            firebaseHelper.updateExtraCharge(chargeId, charge)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            firebaseHelper.addExtraCharge(charge)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(this, "Failed to add: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
