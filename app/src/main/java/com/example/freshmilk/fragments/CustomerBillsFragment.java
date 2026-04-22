package com.example.freshmilk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.DailyEntryAdapter;
import com.example.freshmilk.adapters.ExtraChargeAdapter;
import com.example.freshmilk.databinding.FragmentCustomerBillsBinding;
import com.example.freshmilk.activities.PaymentActivity;
import com.example.freshmilk.activities.ScreenshotUploadActivity;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.ExtraCharge;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.example.freshmilk.utils.PdfGenerator;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CustomerBillsFragment extends Fragment {

    private static final String TAG = "CustomerBillsFragment";
    private FragmentCustomerBillsBinding binding;
    private FirebaseHelper firebaseHelper;
    private DailyEntryAdapter entryAdapter;
    private List<DailyEntry> entryList = new ArrayList<>();
    private ExtraChargeAdapter extraChargeAdapter;
    private List<ExtraCharge> extraChargeList = new ArrayList<>();
    private int currentMonth, currentYear;
    private String customerId;
    private Customer customer;
    private String vendorName = "";
    private double milkAmount = 0;
    private double extraChargesAmount = 0;
    private double totalAmount = 0;
    private String paymentStatus = Payment.STATUS_PENDING;

    private static final String[] MONTH_NAMES = { "January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December" };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerBillsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        setupRecyclerView();
        setupClickListeners();
        updateMonthDisplay();
        findCustomerAndLoad();
    }

    private void setupRecyclerView() {
        entryAdapter = new DailyEntryAdapter(entryList);
        binding.rvEntries.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEntries.setAdapter(entryAdapter);

        extraChargeAdapter = new ExtraChargeAdapter(extraChargeList, false, null);
        binding.rvExtraCharges.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExtraCharges.setAdapter(extraChargeAdapter);
    }

    private void setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) {
                currentMonth = 12;
                currentYear--;
            }
            updateMonthDisplay();
            if (customerId != null) loadBillData();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            updateMonthDisplay();
            if (customerId != null) loadBillData();
        });

        // PDF Download button
        binding.btnDownloadPdf.setOnClickListener(v -> downloadPdf());

        // Pay Now button
        binding.btnPayNow.setOnClickListener(v -> openPaymentScreen());

        // Upload Screenshot button
        binding.btnUploadScreenshot.setOnClickListener(v -> openScreenshotUpload());
    }

    private void downloadPdf() {
        if (customer == null || entryList.isEmpty()) {
            Toast.makeText(requireContext(), "No entries to generate bill", Toast.LENGTH_SHORT).show();
            return;
        }
        PdfGenerator.generateMonthlyBill(requireContext(), vendorName, customer, entryList, extraChargeList,
                currentMonth, currentYear, totalAmount, paymentStatus);
    }

    private void findCustomerAndLoad() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) {
            showNoData("Please login to view bills");
            return;
        }

        firebaseHelper.getCustomerByUserId(userId)
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;

                    if (doc.exists()) {
                        customer = doc.toObject(Customer.class);
                        if (customer != null) {
                            customer.setCustomerId(doc.getId());
                            customerId = doc.getId();

                            // Load vendor name for PDF
                            if (customer.getVendorId() != null) {
                                firebaseHelper.getVendorById(customer.getVendorId())
                                        .addOnSuccessListener(vendorDoc -> {
                                            if (vendorDoc.exists()) {
                                                vendorName = vendorDoc.getString("name");
                                                if (vendorName == null) vendorName = "";
                                            }
                                        });
                            }
                            loadBillData();
                        }
                    } else {
                        Log.w(TAG, "No customer document for userId: " + userId);
                        showNoData("No billing data available");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to find customer", e);
                    if (binding != null) showNoData("Failed to load data");
                });
    }

    private void showNoData(String message) {
        binding.tvNoData.setVisibility(View.VISIBLE);
        binding.tvNoData.setText(message);
        binding.rvEntries.setVisibility(View.GONE);
        binding.btnDownloadPdf.setEnabled(false);
        binding.btnPayNow.setEnabled(false);
    }

    private void openPaymentScreen() {
        if (customer == null || customerId == null) {
            Toast.makeText(requireContext(), "Customer data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), PaymentActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("vendorId", customer.getVendorId());
        intent.putExtra("amount", totalAmount);
        intent.putExtra("month", currentMonth);
        intent.putExtra("year", currentYear);
        intent.putExtra("customerName", customer.getName());
        startActivity(intent);
    }

    private void openScreenshotUpload() {
        if (customer == null || customerId == null) {
            Toast.makeText(requireContext(), "Customer data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), ScreenshotUploadActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("vendorId", customer.getVendorId());
        intent.putExtra("amount", totalAmount);
        intent.putExtra("month", currentMonth);
        intent.putExtra("year", currentYear);
        intent.putExtra("customerName", customer.getName());
        startActivity(intent);
    }

    private void updateMonthDisplay() {
        binding.tvMonth.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
    }

    private void loadBillData() {
        String monthPrefix = String.format(Locale.getDefault(), "%d-%02d", currentYear, currentMonth);

        firebaseHelper.getEntriesByCustomerSimple(customerId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        Log.e(TAG, "Failed to load entries", error);
                        showNoData("Failed to load entries");
                        return;
                    }
                    if (querySnapshot == null) return;

                    entryList.clear();
                    milkAmount = 0;
                    double totalLiters = 0;

                    double rate = (customer != null) ? customer.getRatePerLiter() : 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        entry.setEntryId(doc.getId());

                        if (entry.getDate() == null || !entry.getDate().startsWith(monthPrefix)) {
                            continue;
                        }

                        if (entry.getDailyAmount() == 0 && rate > 0) {
                            double qty = entry.getMorningQty() + entry.getEveningQty();
                            entry.setTotalQty(qty);
                            entry.setDailyAmount(qty * rate);
                            entry.setRatePerLiter(rate);
                        }

                        entryList.add(entry);
                        milkAmount += entry.getDailyAmount();
                        totalLiters += entry.getTotalQty();
                    }

                    // Sort by date ascending
                    Collections.sort(entryList, Comparator.comparing(DailyEntry::getDate));

                    entryAdapter.notifyDataSetChanged();
                    binding.tvTotalEntries.setText(String.valueOf(entryList.size()));
                    binding.tvTotalLiters.setText(String.format(Locale.getDefault(), "%.1f L", totalLiters));

                    boolean hasEntries = !entryList.isEmpty();
                    binding.btnDownloadPdf.setEnabled(hasEntries);
                    if (hasEntries) {
                        binding.tvNoData.setVisibility(View.GONE);
                        binding.rvEntries.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvNoData.setVisibility(View.VISIBLE);
                        binding.tvNoData.setText("No entries for this month");
                        binding.rvEntries.setVisibility(View.GONE);
                    }

                    loadExtraCharges(monthPrefix);
                });
    }

    private void loadExtraCharges(String monthPrefix) {
        firebaseHelper.getExtraChargesByCustomerAndMonth(customerId, monthPrefix)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        Log.e(TAG, "Failed to load extra charges", error);
                        updateTotalsUI();
                        loadPaymentStatus();
                        return;
                    }
                    if (querySnapshot == null) return;

                    extraChargeList.clear();
                    extraChargesAmount = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ExtraCharge charge = doc.toObject(ExtraCharge.class);
                        charge.setChargeId(doc.getId());
                        extraChargeList.add(charge);
                        extraChargesAmount += charge.getAmount();
                    }

                    Collections.sort(extraChargeList, Comparator.comparing(ExtraCharge::getDate));
                    extraChargeAdapter.notifyDataSetChanged();

                    updateTotalsUI();
                    loadPaymentStatus();

                    if (extraChargeList.isEmpty()) {
                        binding.llExtraChargesContainer.setVisibility(View.GONE);
                    } else {
                        binding.llExtraChargesContainer.setVisibility(View.VISIBLE);
                        binding.btnDownloadPdf.setEnabled(true); // If there are extra charges, allow PDF
                    }
                });
    }

    private void updateTotalsUI() {
        totalAmount = milkAmount + extraChargesAmount;
        binding.tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", milkAmount));
        binding.tvExtraChargesAmount.setText(String.format(Locale.getDefault(), "+ ₹%.2f", extraChargesAmount));
        binding.tvFinalTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
    }

    private void loadPaymentStatus() {
        if (binding == null) return;

        firebaseHelper.getPaymentsCollection()
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("month", currentMonth)
                .whereEqualTo("year", currentYear)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) return;
                    if (querySnapshot == null) return;

                    if (!querySnapshot.isEmpty()) {
                        var doc = querySnapshot.getDocuments().get(0);
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            paymentStatus = payment.getStatus();
                            binding.tvPaymentStatus.setText(paymentStatus);

                            if (Payment.STATUS_PAID.equals(paymentStatus)) {
                                binding.tvPaymentStatus.setTextColor(
                                        requireContext().getColor(com.example.freshmilk.R.color.status_paid));
                                binding.btnPayNow.setVisibility(View.GONE);
                                binding.btnUploadScreenshot.setVisibility(View.GONE);
                                binding.tvPaymentStatus.setText("✅ Payment Completed");
                            } else if (Payment.STATUS_PROCESSING.equals(paymentStatus)) {
                                binding.tvPaymentStatus.setTextColor(
                                        requireContext().getColor(com.example.freshmilk.R.color.status_pending_verification));
                                binding.btnPayNow.setVisibility(View.GONE);
                                binding.btnUploadScreenshot.setVisibility(View.GONE);
                                binding.tvPaymentStatus.setText("⏳ Waiting for Verification");
                            } else if (Payment.STATUS_REJECTED.equals(paymentStatus)) {
                                binding.tvPaymentStatus.setTextColor(
                                        requireContext().getColor(com.example.freshmilk.R.color.status_rejected));
                                binding.tvPaymentStatus.setText("REJECTED — Please upload correct screenshot");
                                binding.btnPayNow.setVisibility(View.VISIBLE);
                                binding.btnPayNow.setEnabled(true);
                                binding.btnPayNow.setText("💳 Pay Now");
                                binding.btnUploadScreenshot.setVisibility(View.VISIBLE);
                                binding.btnUploadScreenshot.setText("📤 Re-upload Screenshot");
                            } else {
                                // PENDING
                                binding.tvPaymentStatus.setTextColor(
                                        requireContext().getColor(com.example.freshmilk.R.color.status_unpaid));
                                binding.tvPaymentStatus.setText("Pending");
                                binding.btnPayNow.setVisibility(View.VISIBLE);
                                binding.btnPayNow.setEnabled(true);
                                binding.btnPayNow.setText("💳 Pay Now");
                                binding.btnUploadScreenshot.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        paymentStatus = Payment.STATUS_PENDING;
                        binding.tvPaymentStatus.setText("Pending");
                        binding.tvPaymentStatus
                                .setTextColor(requireContext().getColor(com.example.freshmilk.R.color.status_unpaid));
                        binding.btnPayNow.setVisibility(View.VISIBLE);
                        binding.btnPayNow.setEnabled(true);
                        binding.btnPayNow.setText("💳 Pay Now");
                        binding.btnUploadScreenshot.setVisibility(View.GONE);
                    }
                });
    }

    // No onResume reload — snapshot listeners handle real-time updates

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
