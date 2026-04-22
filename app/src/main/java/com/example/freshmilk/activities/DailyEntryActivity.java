package com.example.freshmilk.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityDailyEntryBinding;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DailyEntryActivity extends AppCompatActivity {

    private ActivityDailyEntryBinding binding;
    private FirebaseHelper firebaseHelper;
    private String customerId;
    private String vendorId;
    private String entryId; // null for new, non-null for edit
    private double ratePerLiter = 0;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDailyEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayFormat = new SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        customerId = getIntent().getStringExtra("customerId");
        vendorId = getIntent().getStringExtra("vendorId");
        entryId = getIntent().getStringExtra("entryId");
        String customerName = getIntent().getStringExtra("customerName");
        ratePerLiter = getIntent().getDoubleExtra("ratePerLiter", 0);
        double defaultQty = getIntent().getDoubleExtra("defaultQuantity", 0);

        binding.tvCustomerName.setText(customerName != null ? customerName : "");

        // If editing an existing entry, pre-fill the fields
        if (entryId != null) {
            String date = getIntent().getStringExtra("date");
            double morningQty = getIntent().getDoubleExtra("morningQty", 0);
            double eveningQty = getIntent().getDoubleExtra("eveningQty", 0);

            if (date != null) {
                try {
                    selectedDate.setTime(dateFormat.parse(date));
                } catch (Exception ignored) {}
            }

            binding.etMorning.setText(String.valueOf(morningQty));
            binding.etEvening.setText(String.valueOf(eveningQty));
            binding.btnSave.setText("Update Entry");
        } else if (defaultQty > 0) {
            binding.etMorning.setText(String.valueOf(defaultQty / 2));
            binding.etEvening.setText(String.valueOf(defaultQty / 2));
        }

        binding.tvDate.setText(displayFormat.format(selectedDate.getTime()));
        setupListeners();
        calculateTotal();
    }

    private void setupListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnPickDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        binding.tvDate.setText(displayFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etMorning.addTextChangedListener(watcher);
        binding.etEvening.addTextChangedListener(watcher);

        binding.btnSave.setOnClickListener(v -> saveEntry());
    }

    private void calculateTotal() {
        double morning = parseDouble(binding.etMorning.getText().toString());
        double evening = parseDouble(binding.etEvening.getText().toString());
        double total = morning + evening;
        double amount = total * ratePerLiter;

        binding.tvTotalQty.setText(String.format(Locale.getDefault(), "%.1f L", total));
        binding.tvDailyAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
    }

    private double parseDouble(String value) {
        if (TextUtils.isEmpty(value))
            return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void saveEntry() {
        double morning = parseDouble(binding.etMorning.getText().toString());
        double evening = parseDouble(binding.etEvening.getText().toString());

        if (morning == 0 && evening == 0) {
            Toast.makeText(this, "Please enter at least one quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null || customerId == null) {
            Toast.makeText(this, "Error: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        String dateStr = dateFormat.format(selectedDate.getTime());
        DailyEntry entry = new DailyEntry(customerId, userId, vendorId, dateStr, morning, evening, ratePerLiter);

        if (entryId != null) {
            // Update existing entry
            firebaseHelper.updateDailyEntry(entryId, entry)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Add new entry
            firebaseHelper.addDailyEntry(entry)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, R.string.entry_saved, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }
}
