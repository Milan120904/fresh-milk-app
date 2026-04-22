package com.example.freshmilk.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.freshmilk.R;
import com.example.freshmilk.activities.LoginActivity;
import com.example.freshmilk.databinding.FragmentProfileBinding;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseHelper firebaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();

        loadUserProfile();
        setupDarkMode();
        setupLogout();
    }

    private void loadUserProfile() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUserRef(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && binding != null) {
                            String name = user.getName();
                            if (name != null && !name.isEmpty()) {
                                binding.tvProfileInitial.setText(
                                        String.valueOf(name.charAt(0)).toUpperCase());
                            } else {
                                binding.tvProfileInitial.setText("?");
                            }

                            binding.tvUserName.setText(name != null ? name : "N/A");
                            binding.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                            binding.tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");

                            String role = user.getRole();
                            if (role != null) {
                                binding.tvUserRole.setText(role);
                                binding.tvUserRole.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvUserRole.setVisibility(View.GONE);
                            }

                            // Show UPI card for vendors
                            if (user.isVendor()) {
                                binding.cardUpiId.setVisibility(View.VISIBLE);
                                binding.llVendorSettings.setVisibility(View.VISIBLE);
                                binding.spaceVendorSettings.setVisibility(View.VISIBLE);
                                if (user.getUpiId() != null && !user.getUpiId().isEmpty()) {
                                    binding.etUpiId.setText(user.getUpiId());
                                }
                                setupUpiSave(uid);
                                setupVendorSettings();
                            } else {
                                binding.cardUpiId.setVisibility(View.GONE);
                                binding.llVendorSettings.setVisibility(View.GONE);
                                binding.spaceVendorSettings.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    private void setupUpiSave(String uid) {
        binding.btnSaveUpi.setOnClickListener(v -> {
            String upiId = binding.etUpiId.getText().toString().trim();
            if (upiId.isEmpty()) {
                binding.etUpiId.setError("Enter UPI ID");
                return;
            }
            if (!upiId.contains("@")) {
                binding.etUpiId.setError("Invalid UPI ID format");
                return;
            }

            binding.btnSaveUpi.setEnabled(false);
            Map<String, Object> fields = new HashMap<>();
            fields.put("upiId", upiId);

            firebaseHelper.updateUserFields(uid, fields)
                    .addOnSuccessListener(aVoid -> {
                        if (binding != null) {
                            binding.btnSaveUpi.setEnabled(true);
                            Toast.makeText(requireContext(), "UPI ID saved successfully!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (binding != null) {
                            binding.btnSaveUpi.setEnabled(true);
                            Toast.makeText(requireContext(), "Failed to save UPI ID", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setupVendorSettings() {
        binding.llVendorSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.freshmilk.activities.VendorSettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupDarkMode() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        int mode = prefs.getInt("dark_mode_state", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        updateDarkModeText(mode);

        binding.llDarkMode.setOnClickListener(v -> {
            String[] options = {"System Default", "Light", "Dark"};
            int checkedItem = 0;
            switch(mode) {
                case AppCompatDelegate.MODE_NIGHT_NO: checkedItem = 1; break;
                case AppCompatDelegate.MODE_NIGHT_YES: checkedItem = 2; break;
                default: checkedItem = 0; break;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Dark Mode")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        int selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        if (which == 1) selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
                        else if (which == 2) selectedMode = AppCompatDelegate.MODE_NIGHT_YES;

                        prefs.edit().putInt("dark_mode_state", selectedMode).apply();
                        AppCompatDelegate.setDefaultNightMode(selectedMode);
                        updateDarkModeText(selectedMode);
                        dialog.dismiss();
                    })
                    .show();
        });
    }

    private void updateDarkModeText(int mode) {
        if (binding == null) return;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) binding.tvDarkModeState.setText("Light");
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) binding.tvDarkModeState.setText("Dark");
        else binding.tvDarkModeState.setText("System Default");
    }

    private void setupLogout() {
        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.logout))
                    .setMessage(getString(R.string.logout_confirm))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        firebaseHelper.logout();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
