package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.databinding.ActivityVendorSettingsBinding;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class VendorSettingsActivity extends AppCompatActivity {
    
    private ActivityVendorSettingsBinding binding;
    private FirebaseHelper firebaseHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVendorSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // Setup pickers defaults (1 to 31)
        binding.npStart.setMinValue(1);
        binding.npStart.setMaxValue(31);
        binding.npEnd.setMinValue(1);
        binding.npEnd.setMaxValue(31);
        
        loadCurrentSettings();
        
        binding.btnSave.setOnClickListener(v -> saveSettings());
    }
    
    private void loadCurrentSettings() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid != null) {
            firebaseHelper.getUserRef(uid).get().addOnSuccessListener(snapshot -> {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    int start = user.getReminderStartDate();
                    int end = user.getReminderEndDate();
                    binding.npStart.setValue(start == 0 ? 1 : start);
                    binding.npEnd.setValue(end == 0 ? 10 : end);
                }
            });
        }
    }
    
    private void saveSettings() {
        int start = binding.npStart.getValue();
        int end = binding.npEnd.getValue();
        
        if (start > end) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String uid = firebaseHelper.getCurrentUserId();
        if (uid != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("reminderStartDate", start);
            updates.put("reminderEndDate", end);
            
            firebaseHelper.updateUserFields(uid, updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show());
        }
    }
}
