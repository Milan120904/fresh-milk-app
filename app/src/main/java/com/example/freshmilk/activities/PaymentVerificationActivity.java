package com.example.freshmilk.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.PaymentVerificationAdapter;
import com.example.freshmilk.databinding.ActivityPaymentVerificationBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentVerificationActivity extends AppCompatActivity
        implements PaymentVerificationAdapter.OnVerificationActionListener {

    private static final String TAG = "PaymentVerification";
    private ActivityPaymentVerificationBinding binding;
    private FirebaseHelper firebaseHelper;
    private PaymentVerificationAdapter adapter;
    private final List<Payment> pendingPayments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        setupToolbar();
        setupRecyclerView();
        loadPendingPayments();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new PaymentVerificationAdapter(pendingPayments, this);
        binding.rvPayments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPayments.setAdapter(adapter);
    }

    private void loadPendingPayments() {
        String vendorId = firebaseHelper.getCurrentUserId();
        if (vendorId == null) return;

        // Query by vendorId
        firebaseHelper.getPendingVerificationPayments(vendorId)
                .addSnapshotListener((vendorSnapshot, vendorError) -> {
                    if (vendorError != null) {
                        Log.e(TAG, "Vendor query error", vendorError);
                    }

                    pendingPayments.clear();

                    if (vendorSnapshot != null) {
                        for (QueryDocumentSnapshot doc : vendorSnapshot) {
                            Payment payment = doc.toObject(Payment.class);
                            payment.setPaymentId(doc.getId());
                            pendingPayments.add(payment);
                        }
                    }

                    // Also query by userId for legacy data
                    firebaseHelper.getPendingVerificationByUserId(vendorId)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                for (QueryDocumentSnapshot doc : userSnapshot) {
                                    String docId = doc.getId();
                                    boolean alreadyAdded = false;
                                    for (Payment p : pendingPayments) {
                                        if (docId.equals(p.getPaymentId())) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }
                                    if (!alreadyAdded) {
                                        Payment payment = doc.toObject(Payment.class);
                                        payment.setPaymentId(docId);
                                        pendingPayments.add(payment);
                                    }
                                }
                                updateUI();
                            })
                            .addOnFailureListener(e -> updateUI());
                });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        binding.tvPendingCount.setText(String.valueOf(pendingPayments.size()));

        if (pendingPayments.isEmpty()) {
            binding.rvPayments.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvPayments.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onApprove(Payment payment) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Payment")
                .setMessage("Approve payment of ₹" + String.format("%.0f", payment.getTotalAmount())
                        + " from " + payment.getCustomerName() + "?")
                .setPositiveButton("Approve", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", Payment.STATUS_PAID);
                    updates.put("paidAmount", payment.getTotalAmount());
                    updates.put("paidDate", new Timestamp(new Date()));

                    firebaseHelper.updatePaymentFields(payment.getPaymentId(), updates)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Payment approved ✅", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to approve: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReject(Payment payment) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Payment")
                .setMessage("Reject payment screenshot from " + payment.getCustomerName()
                        + "? The customer will be asked to upload a correct screenshot.")
                .setPositiveButton("Reject", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", Payment.STATUS_PENDING);

                    firebaseHelper.updatePaymentFields(payment.getPaymentId(), updates)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Payment rejected ❌", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to reject: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
