package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.PaymentAdapter;
import com.example.freshmilk.databinding.ActivityVendorPaymentsBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VendorPaymentsActivity extends AppCompatActivity {

    private ActivityVendorPaymentsBinding binding;
    private FirebaseHelper firebaseHelper;
    private PaymentAdapter adapter;
    private final List<Payment> allPayments = new ArrayList<>();
    private final List<Payment> filteredPayments = new ArrayList<>();
    private int currentMonth, currentYear;
    private int currentFilter = 0; // 0=All, 1=Paid, 2=Pending

    private static final String[] MONTH_NAMES = {"January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVendorPaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        setupToolbar();
        setupRecyclerView();
        setupMonthSelector();
        setupTabs();
        updateMonthDisplay();
        loadPayments();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new PaymentAdapter(filteredPayments, null);
        binding.rvPayments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPayments.setAdapter(adapter);
    }

    private void setupMonthSelector() {
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) {
                currentMonth = 12;
                currentYear--;
            }
            updateMonthDisplay();
            loadPayments();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            updateMonthDisplay();
            loadPayments();
        });
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Paid"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Verification"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getPosition();
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateMonthDisplay() {
        binding.tvMonth.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
    }

    private void loadPayments() {
        String vendorId = firebaseHelper.getCurrentUserId();
        if (vendorId == null) return;

        // Query by userId (legacy data from DummyDataHelper / Payment constructor)
        firebaseHelper.getPaymentsByUserAndMonth(vendorId, currentMonth, currentYear)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot == null) return;

                    allPayments.clear();
                    double totalCollected = 0;
                    double totalPending = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Payment payment = doc.toObject(Payment.class);
                        payment.setPaymentId(doc.getId());
                        allPayments.add(payment);

                        if (Payment.STATUS_PAID.equals(payment.getStatus())) {
                            totalCollected += payment.getPaidAmount();
                        } else {
                            totalPending += payment.getTotalAmount() - payment.getPaidAmount();
                        }
                    }

                    // Also query by vendorId field (payments saved via PaymentActivity)
                    double finalCollected = totalCollected;
                    double finalPending = totalPending;
                    firebaseHelper.getPaymentsByVendorAndMonth(vendorId, currentMonth, currentYear)
                            .get()
                            .addOnSuccessListener(vendorSnapshot -> {
                                double collected = finalCollected;
                                double pending = finalPending;

                                for (QueryDocumentSnapshot doc : vendorSnapshot) {
                                    // Skip if already added from userId query
                                    String docId = doc.getId();
                                    boolean alreadyAdded = false;
                                    for (Payment p : allPayments) {
                                        if (docId.equals(p.getPaymentId())) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }
                                    if (alreadyAdded) continue;

                                    Payment payment = doc.toObject(Payment.class);
                                    payment.setPaymentId(docId);
                                    allPayments.add(payment);

                                    if (Payment.STATUS_PAID.equals(payment.getStatus())) {
                                        collected += payment.getPaidAmount();
                                    } else {
                                        pending += payment.getTotalAmount() - payment.getPaidAmount();
                                    }
                                }

                                // Update summary
                                binding.tvTotalCollected.setText(
                                        String.format(Locale.getDefault(), "₹%.0f", collected));
                                binding.tvTotalPending.setText(
                                        String.format(Locale.getDefault(), "₹%.0f", pending));
                                binding.tvCustomerCount.setText(String.valueOf(allPayments.size()));

                                applyFilter();
                            });
                });
    }

    private void applyFilter() {
        filteredPayments.clear();

        for (Payment p : allPayments) {
            switch (currentFilter) {
                case 0: // All
                    filteredPayments.add(p);
                    break;
                case 1: // Paid
                    if (Payment.STATUS_PAID.equals(p.getStatus())) {
                        filteredPayments.add(p);
                    }
                    break;
                case 2: // Pending (Unpaid + Partial)
                    if (!Payment.STATUS_PAID.equals(p.getStatus())
                            && !Payment.STATUS_PROCESSING.equals(p.getStatus())
                            && !Payment.STATUS_REJECTED.equals(p.getStatus())) {
                        filteredPayments.add(p);
                    }
                    break;
                case 3: // Verification (Processing + Rejected)
                    if (Payment.STATUS_PROCESSING.equals(p.getStatus())
                            || Payment.STATUS_REJECTED.equals(p.getStatus())) {
                        filteredPayments.add(p);
                    }
                    break;
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredPayments.isEmpty()) {
            binding.rvPayments.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvPayments.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }
}
