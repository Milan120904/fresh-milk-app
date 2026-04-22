package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.PaymentAdapter;
import com.example.freshmilk.databinding.ActivityPendingPaymentsBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PendingPaymentsActivity extends AppCompatActivity {

    private ActivityPendingPaymentsBinding binding;
    private FirebaseHelper firebaseHelper;
    private PaymentAdapter adapter;
    private final List<Payment> paymentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingPaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        loadPendingPayments();
    }

    private void setupRecyclerView() {
        adapter = new PaymentAdapter(paymentList, null);
        binding.rvPayments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPayments.setAdapter(adapter);
    }

    private void loadPendingPayments() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) return;

        firebaseHelper.getUnpaidPayments(userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    paymentList.clear();
                    double totalPending = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Payment payment = doc.toObject(Payment.class);
                        payment.setPaymentId(doc.getId());
                        paymentList.add(payment);
                        totalPending += payment.getTotalAmount() - payment.getPaidAmount();
                    }

                    adapter.notifyDataSetChanged();

                    binding.tvTotalPending.setText(
                            String.format(Locale.getDefault(), "₹%.0f", totalPending));
                    binding.tvPendingCount.setText(String.valueOf(paymentList.size()));

                    if (paymentList.isEmpty()) {
                        binding.rvPayments.setVisibility(View.GONE);
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvPayments.setVisibility(View.VISIBLE);
                        binding.emptyState.setVisibility(View.GONE);
                    }
                });
    }
}
