package com.example.freshmilk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshmilk.MainActivity;
import com.example.freshmilk.R;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Configure Firestore offline persistence ONCE before any operations
        FirebaseHelper.configureFirestore();
        
        // Apply Dark Mode Settings globally
        android.content.SharedPreferences prefs = getSharedPreferences("settings", 0);
        int mode = prefs.getInt("dark_mode_state", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
        
        // Enqueue Payment Reminders Worker
        androidx.work.PeriodicWorkRequest reminderRequest =
                new androidx.work.PeriodicWorkRequest.Builder(com.example.freshmilk.workers.PaymentReminderWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PaymentReminderWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseHelper.getInstance().getCurrentUser() != null) {
                // User is logged in — check role
                String uid = FirebaseHelper.getInstance().getCurrentUserId();
                if (uid != null) {
                    FirebaseHelper.getInstance().getUserByUid(uid)
                            .addOnSuccessListener(documentSnapshot -> {
                                User user = documentSnapshot.toObject(User.class);
                                Intent intent;
                                if (user != null && User.ROLE_CUSTOMER.equals(user.getRole())) {
                                    intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
                                } else {
                                    intent = new Intent(SplashActivity.this, MainActivity.class);
                                }
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Default to vendor on failure
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                            });
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, SPLASH_DELAY);
    }
}
