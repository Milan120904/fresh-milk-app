package com.example.freshmilk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.MainActivity;
import com.example.freshmilk.databinding.ActivityLoginBinding;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(com.example.freshmilk.R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(com.example.freshmilk.R.string.invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(com.example.freshmilk.R.string.field_required));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(com.example.freshmilk.R.string.password_min_length));
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        showLoading(true);

        firebaseHelper.login(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Fetch user role from Firestore
                        String uid = firebaseHelper.getCurrentUserId();
                        if (uid != null) {
                            firebaseHelper.getUserByUid(uid)
                                    .addOnSuccessListener(documentSnapshot -> {
                                        showLoading(false);
                                        User user = documentSnapshot.toObject(User.class);
                                        Intent intent;
                                        if (user != null && User.ROLE_CUSTOMER.equals(user.getRole())) {
                                            intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
                                        } else {
                                            intent = new Intent(LoginActivity.this, MainActivity.class);
                                        }
                                        startActivity(intent);
                                        finishAffinity();
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        // Default to vendor if role fetch fails
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finishAffinity();
                                    });
                        } else {
                            showLoading(false);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
    }

    private void showForgotPasswordDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setPadding(48, 32, 48, 32);

        // Pre-fill with email if already typed
        String existingEmail = binding.etEmail.getText().toString().trim();
        if (!existingEmail.isEmpty()) {
            emailInput.setText(existingEmail);
        }

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your registered email address. We'll send you a password reset link.")
                .setView(emailInput)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);
        firebaseHelper.getCurrentUser(); // just to ensure auth is initialized
        com.google.firebase.auth.FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset email sent! Check your inbox.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to send reset email";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
