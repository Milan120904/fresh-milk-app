package com.example.freshmilk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.MainActivity;
import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityRegisterBinding;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        // Default to vendor
        binding.rbVendor.setChecked(true);

        // Toggle vendor phone & address fields based on role selection
        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isCustomer = (checkedId == R.id.rbCustomer);
            binding.tilVendorPhone.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
            binding.tilAddress.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        });

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvSignIn.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Get selected role
        String role = binding.rbCustomer.isChecked() ? User.ROLE_CUSTOMER : User.ROLE_VENDOR;
        boolean isCustomer = User.ROLE_CUSTOMER.equals(role);

        String vendorPhone = "";
        String address = "";
        if (isCustomer) {
            vendorPhone = binding.etVendorPhone.getText().toString().trim();
            address = binding.etAddress.getText().toString().trim();
        }

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.invalid_email));
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.setError(getString(R.string.field_required));
            return;
        }
        if (phone.length() < 10) {
            binding.tilPhone.setError(getString(R.string.invalid_phone));
            return;
        }
        // Customer-specific validation
        if (isCustomer) {
            if (TextUtils.isEmpty(vendorPhone)) {
                binding.tilVendorPhone.setError(getString(R.string.field_required));
                return;
            }
            if (vendorPhone.length() < 10) {
                binding.tilVendorPhone.setError(getString(R.string.invalid_phone));
                return;
            }
            if (TextUtils.isEmpty(address)) {
                binding.tilAddress.setError(getString(R.string.field_required));
                return;
            }
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.field_required));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.password_min_length));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.passwords_dont_match));
            return;
        }

        // Clear errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        if (isCustomer) {
            binding.tilVendorPhone.setError(null);
            binding.tilAddress.setError(null);
        }
        showLoading(true);

        if (isCustomer) {
            // Customer flow: first find the vendor by phone, then register
            String finalVendorPhone = vendorPhone;
            String finalAddress = address;
            firebaseHelper.findVendorByPhone(vendorPhone)
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            showLoading(false);
                            binding.tilVendorPhone.setError(getString(R.string.vendor_not_found));
                            Toast.makeText(this, R.string.vendor_not_found, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Vendor found — get vendorId
                        DocumentSnapshot vendorDoc = querySnapshot.getDocuments().get(0);
                        String vendorId = vendorDoc.getString("uid");
                        if (vendorId == null) {
                            vendorId = vendorDoc.getId();
                        }

                        // Proceed with Firebase Auth registration
                        String finalVendorId = vendorId;
                        firebaseHelper.register(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                                        String uid = task.getResult().getUser().getUid();
                                        Log.d(TAG, "Customer auth registration successful, UID: " + uid);

                                        // Save user data
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("uid", uid);
                                        userData.put("name", name);
                                        userData.put("email", email);
                                        userData.put("phone", phone);
                                        userData.put("role", role);
                                        userData.put("vendorId", finalVendorId);
                                        userData.put("createdAt", FieldValue.serverTimestamp());

                                        firebaseHelper.saveUserData(uid, userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Also create a customer document linked to this vendor
                                                    createCustomerDocument(uid, finalVendorId, name, phone, finalAddress);
                                                })
                                                .addOnFailureListener(e -> {
                                                    showLoading(false);
                                                    Log.e(TAG, "Failed to save user data", e);
                                                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                                                    Toast.makeText(this, "Failed to save: " + errorMsg, Toast.LENGTH_LONG).show();
                                                });
                                    } else {
                                        showLoading(false);
                                        String error = task.getException() != null ? task.getException().getMessage()
                                                : "Registration failed";
                                        Log.e(TAG, "Auth registration failed: " + error, task.getException());
                                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Failed to search for vendor", e);
                        Toast.makeText(this, "Error searching for vendor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            // Vendor flow: standard registration
            registerVendor(name, email, phone, password, role);
        }
    }

    private void registerVendor(String name, String email, String phone, String password, String role) {
        firebaseHelper.register(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        Log.d(TAG, "Vendor auth registration successful, UID: " + uid);

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", uid);
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("phone", phone);
                        userData.put("role", role);
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        firebaseHelper.saveUserData(uid, userData)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    Log.d(TAG, "Vendor data saved successfully");
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finishAffinity();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Log.e(TAG, "Failed to save vendor data", e);
                                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                                    Toast.makeText(this, "Failed to save: " + errorMsg, Toast.LENGTH_LONG).show();
                                });
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Registration failed";
                        Log.e(TAG, "Auth registration failed: " + error, task.getException());
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createCustomerDocument(String userId, String vendorId, String name, String phone, String address) {
        // Use Auth UID as customerId so vendor entries match customer queries
        // customerId == userId == Firebase Auth UID
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerId", userId);
        customerData.put("userId", userId);
        customerData.put("vendorId", vendorId);
        customerData.put("name", name);
        customerData.put("mobile", phone);
        customerData.put("address", address);
        customerData.put("milkType", "Cow Milk");
        customerData.put("defaultQuantity", 1.0);
        customerData.put("ratePerLiter", 0.0);
        customerData.put("active", true);
        customerData.put("createdAt", FieldValue.serverTimestamp());

        // Document ID = userId = customerId (all the same)
        firebaseHelper.getCustomersCollection().document(userId)
                .set(customerData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "Customer document created with ID=UID: " + userId + ", linked to vendor: " + vendorId);
                    Toast.makeText(this, R.string.customer_linked_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, CustomerMainActivity.class);
                    startActivity(intent);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to create customer document", e);
                    Toast.makeText(this, "Account created but linking failed. Contact vendor.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, CustomerMainActivity.class);
                    startActivity(intent);
                    finishAffinity();
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
    }
}
