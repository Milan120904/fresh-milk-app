package com.example.freshmilk.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityAddCustomerBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddCustomerActivity extends AppCompatActivity {

    private static final String TAG = "AddCustomerActivity";
    private ActivityAddCustomerBinding binding;
    private FirebaseHelper firebaseHelper;
    private String editCustomerId = null;

    private double selectedLatitude = 0;
    private double selectedLongitude = 0;

    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLatitude = data.getDoubleExtra("latitude", 0);
                    selectedLongitude = data.getDoubleExtra("longitude", 0);
                    String address = data.getStringExtra("address");

                    // Update address field if we got one from map
                    if (address != null && !address.isEmpty()) {
                        binding.etAddress.setText(address);
                    }

                    // Show selected coordinates
                    updateLocationDisplay();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCustomerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        setupMilkTypeDropdown();
        setupClickListeners();

        // Check if editing existing customer
        if (getIntent().hasExtra("customerId")) {
            editCustomerId = getIntent().getStringExtra("customerId");
            loadCustomerData();
        }
    }

    private void setupMilkTypeDropdown() {
        String[] milkTypes = {
                getString(R.string.cow_milk),
                getString(R.string.buffalo_milk),
                getString(R.string.mixed_milk)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, milkTypes);
        binding.actvMilkType.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveCustomer());

        // Pick Location on Map button
        binding.btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            if (selectedLatitude != 0 && selectedLongitude != 0) {
                intent.putExtra("latitude", selectedLatitude);
                intent.putExtra("longitude", selectedLongitude);
            }
            mapPickerLauncher.launch(intent);
        });
    }

    private void updateLocationDisplay() {
        if (selectedLatitude != 0 && selectedLongitude != 0) {
            binding.tvSelectedLocation.setVisibility(View.VISIBLE);
            binding.tvSelectedLocation.setText(
                    String.format(Locale.getDefault(), getString(R.string.location_selected),
                            selectedLatitude, selectedLongitude));
        } else {
            binding.tvSelectedLocation.setVisibility(View.GONE);
        }
    }

    private void loadCustomerData() {
        firebaseHelper.getCustomersCollection().document(editCustomerId)
                .get()
                .addOnSuccessListener(doc -> {
                    Customer customer = doc.toObject(Customer.class);
                    if (customer != null) {
                        binding.etName.setText(customer.getName());
                        binding.etMobile.setText(customer.getMobile());
                        binding.etAddress.setText(customer.getAddress());
                        binding.actvMilkType.setText(customer.getMilkType(), false);
                        binding.etQuantity.setText(String.valueOf(customer.getDefaultQuantity()));
                        binding.etRate.setText(String.valueOf(customer.getRatePerLiter()));
                        binding.btnSave.setText(R.string.update);

                        // Load existing location
                        selectedLatitude = customer.getLatitude();
                        selectedLongitude = customer.getLongitude();
                        updateLocationDisplay();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load customer data", e);
                    Toast.makeText(this, "Failed to load customer data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCustomer() {
        String name = binding.etName.getText().toString().trim();
        String mobile = binding.etMobile.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String milkType = binding.actvMilkType.getText().toString().trim();
        String qtyStr = binding.etQuantity.getText().toString().trim();
        String rateStr = binding.etRate.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(mobile) || mobile.length() < 10) {
            binding.tilMobile.setError(getString(R.string.invalid_phone));
            return;
        }
        if (TextUtils.isEmpty(address)) {
            binding.tilAddress.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(milkType)) {
            binding.tilMilkType.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(qtyStr)) {
            binding.tilQuantity.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(rateStr)) {
            binding.tilRate.setError(getString(R.string.field_required));
            return;
        }

        double quantity;
        double rate;
        try {
            quantity = Double.parseDouble(qtyStr);
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        // If user typed address manually but didn't pick on map, try geocoding
        if (selectedLatitude == 0 && selectedLongitude == 0 && !TextUtils.isEmpty(address)) {
            geocodeAddress(address);
        }

        String userId = firebaseHelper.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (editCustomerId != null) {
            // Update existing customer using HashMap + merge to preserve vendorId
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", name);
            updateData.put("mobile", mobile);
            updateData.put("address", address);
            updateData.put("milkType", milkType);
            updateData.put("defaultQuantity", quantity);
            updateData.put("ratePerLiter", rate);
            updateData.put("latitude", selectedLatitude);
            updateData.put("longitude", selectedLongitude);
            updateData.put("active", true);

            firebaseHelper.updateCustomer(editCustomerId, updateData)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Log.d(TAG, "Customer updated successfully");
                        Toast.makeText(this, R.string.customer_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Failed to update customer", e);
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to update: " + errorMsg, Toast.LENGTH_LONG).show();
                    });
        } else {
            // Check for duplicate customer by phone number
            firebaseHelper.checkDuplicateCustomerByPhone(userId, mobile)
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            showLoading(false);
                            binding.tilMobile.setError(getString(R.string.duplicate_customer));
                            Toast.makeText(this, R.string.duplicate_customer, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Add new customer using HashMap to avoid POJO serialization issues
                        DocumentReference docRef = firebaseHelper.getCustomersCollection().document();
                        String customerId = docRef.getId();

                        Map<String, Object> customerData = new HashMap<>();
                        customerData.put("customerId", customerId);
                        customerData.put("userId", userId);
                        customerData.put("vendorId", userId);
                        customerData.put("name", name);
                        customerData.put("mobile", mobile);
                        customerData.put("address", address);
                        customerData.put("milkType", milkType);
                        customerData.put("defaultQuantity", quantity);
                        customerData.put("ratePerLiter", rate);
                        customerData.put("latitude", selectedLatitude);
                        customerData.put("longitude", selectedLongitude);
                        customerData.put("active", true);
                        customerData.put("createdAt", FieldValue.serverTimestamp());

                        docRef.set(customerData)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    Log.d(TAG, "Customer added successfully with ID: " + customerId);
                                    Toast.makeText(this, R.string.customer_added, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Log.e(TAG, "Failed to add customer", e);
                                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                                    Toast.makeText(this, "Failed to save: " + errorMsg, Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Failed to check duplicate", e);
                        Toast.makeText(this, "Error checking duplicates: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    /**
     * Attempt to geocode a text address into lat/lng coordinates.
     * This is a best-effort fallback when the user types an address manually
     * instead of using the map picker.
     */
    private void geocodeAddress(String address) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(address, 1);
            if (results != null && !results.isEmpty()) {
                Address location = results.get(0);
                selectedLatitude = location.getLatitude();
                selectedLongitude = location.getLongitude();
                Log.d(TAG, "Geocoded address to: " + selectedLatitude + ", " + selectedLongitude);
            }
        } catch (IOException e) {
            Log.w(TAG, "Geocoding failed for address: " + address, e);
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }
}
