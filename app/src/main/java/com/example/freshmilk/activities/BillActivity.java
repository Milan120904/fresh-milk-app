package com.example.freshmilk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.R;
import com.example.freshmilk.adapters.DailyEntryAdapter;
import com.example.freshmilk.adapters.ExtraChargeAdapter;
import com.example.freshmilk.databinding.ActivityBillBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.ExtraCharge;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.example.freshmilk.utils.PdfGenerator;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BillActivity extends AppCompatActivity {

    private static final String TAG = "BillActivity";
    private ActivityBillBinding binding;
    private FirebaseHelper firebaseHelper;
    private String customerId;
    private Customer customer;
    private String vendorName = "";
    private DailyEntryAdapter entryAdapter;
    private List<DailyEntry> entryList = new ArrayList<>();
    private int currentMonth, currentYear;
    private double milkAmount = 0;
    private double extraChargesAmount = 0;
    private double totalAmount = 0;
    private Payment currentPayment;
    private String currentPaymentId;

    private ExtraChargeAdapter extraChargeAdapter;
    private List<ExtraCharge> extraChargeList = new ArrayList<>();

    private static final String[] MONTH_NAMES = { "January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        customerId = getIntent().getStringExtra("customerId");
        String customerName = getIntent().getStringExtra("customerName");

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        binding.tvCustomerName.setText(customerName != null ? customerName : "");
        setupRecyclerView();
        setupClickListeners();
        updateMonthDisplay();
        // Load customer first, then load bill data in the callback
        loadCustomer();
    }

    private void setupRecyclerView() {
        entryAdapter = new DailyEntryAdapter(entryList);
        binding.rvEntries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEntries.setAdapter(entryAdapter);

        extraChargeAdapter = new ExtraChargeAdapter(extraChargeList, true, new ExtraChargeAdapter.OnExtraChargeActionListener() {
            @Override
            public void onEditCharge(ExtraCharge charge) {
                Intent intent = new Intent(BillActivity.this, AddExtraChargeActivity.class);
                intent.putExtra("customerId", customerId);
                if (customer != null) {
                    intent.putExtra("vendorId", customer.getVendorId());
                    intent.putExtra("customerName", customer.getName());
                }
                intent.putExtra("chargeId", charge.getChargeId());
                intent.putExtra("amount", charge.getAmount());
                intent.putExtra("description", charge.getDescription());
                intent.putExtra("date", charge.getDate());
                startActivity(intent);
            }

            @Override
            public void onDeleteCharge(ExtraCharge charge) {
                new AlertDialog.Builder(BillActivity.this)
                        .setTitle("Delete Extra Charge")
                        .setMessage("Are you sure you want to delete this charge: " + charge.getDescription() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (charge.getChargeId() != null) {
                                firebaseHelper.deleteExtraCharge(charge.getChargeId())
                                        .addOnSuccessListener(aVoid -> Toast.makeText(BillActivity.this, "Deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(BillActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.rvExtraCharges.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExtraCharges.setAdapter(extraChargeAdapter);
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) {
                currentMonth = 12;
                currentYear--;
            }
            updateMonthDisplay();
            loadBillData();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            updateMonthDisplay();
            loadBillData();
        });

        binding.btnMarkPaid.setOnClickListener(v -> togglePaymentStatus());
        binding.btnDownloadPdf.setOnClickListener(v -> downloadPdf());

        binding.btnAddExtraCharge.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExtraChargeActivity.class);
            intent.putExtra("customerId", customerId);
            if (customer != null) {
                intent.putExtra("vendorId", customer.getVendorId());
                intent.putExtra("customerName", customer.getName());
            }
            startActivity(intent);
        });
    }

    private void loadCustomer() {
        firebaseHelper.getCustomersCollection().document(customerId)
                .get()
                .addOnSuccessListener(doc -> {
                    customer = doc.toObject(Customer.class);
                    if (customer != null) {
                        customer.setCustomerId(doc.getId());
                        // Load vendor name for PDF generation
                        if (customer.getVendorId() != null) {
                            firebaseHelper.getVendorById(customer.getVendorId())
                                    .addOnSuccessListener(vendorDoc -> {
                                        if (vendorDoc.exists()) {
                                            vendorName = vendorDoc.getString("name");
                                            if (vendorName == null) vendorName = "";
                                        }
                                    });
                        }
                    }
                    // Now load bill data after customer is loaded
                    loadBillData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load customer", e);
                    // Still try to load bill data
                    loadBillData();
                });
    }

    private void updateMonthDisplay() {
        binding.tvMonth.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
    }

    private void loadBillData() {
        String monthPrefix = String.format(Locale.getDefault(), "%d-%02d", currentYear, currentMonth);

        // Use simple query (no orderBy) to avoid Firestore composite index requirement
        firebaseHelper.getEntriesByCustomerSimple(customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entryList.clear();
                    milkAmount = 0;
                    double totalLiters = 0;

                    // Get the customer's current rate for recalculation
                    double rate = (customer != null) ? customer.getRatePerLiter() : 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        entry.setEntryId(doc.getId());

                        // Filter: only include entries for the selected month
                        if (entry.getDate() == null || !entry.getDate().startsWith(monthPrefix)) {
                            continue;
                        }

                        // Recalculate daily amount using current rate if stored amount is 0
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

                    // Sort entries by date ascending
                    Collections.sort(entryList, Comparator.comparing(DailyEntry::getDate));

                    entryAdapter.notifyDataSetChanged();
                    binding.tvTotalEntries.setText(String.valueOf(entryList.size()));
                    binding.tvTotalLiters.setText(String.format(Locale.getDefault(), "%.1f", totalLiters));

                    loadExtraCharges(monthPrefix);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load bill data", e);
                    Toast.makeText(this, "Failed to load bill data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loadExtraCharges(monthPrefix);
                });
    }

    private void loadExtraCharges(String monthPrefix) {
        firebaseHelper.getExtraChargesByCustomerAndMonth(customerId, monthPrefix)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    extraChargeList.clear();
                    extraChargesAmount = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ExtraCharge charge = doc.toObject(ExtraCharge.class);
                        charge.setChargeId(doc.getId());
                        extraChargeList.add(charge);
                        extraChargesAmount += charge.getAmount();
                    }

                    // Sort extra charges by date
                    Collections.sort(extraChargeList, Comparator.comparing(ExtraCharge::getDate));
                    extraChargeAdapter.notifyDataSetChanged();

                    updateTotalsUI();
                    loadPaymentStatus();

                    if (extraChargeList.isEmpty()) {
                        binding.llExtraChargesContainer.setVisibility(View.GONE);
                    } else {
                        binding.llExtraChargesContainer.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load extra charges", e);
                    updateTotalsUI();
                    loadPaymentStatus();
                });
    }

    private void updateTotalsUI() {
        totalAmount = milkAmount + extraChargesAmount;
        binding.tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", milkAmount));
        binding.tvExtraChargesAmount.setText(String.format(Locale.getDefault(), "+ ₹%.2f", extraChargesAmount));
        binding.tvFinalTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
    }

    private void loadPaymentStatus() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null)
            return;

        firebaseHelper.getPaymentsCollection()
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("month", currentMonth)
                .whereEqualTo("year", currentYear)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        var doc = querySnapshot.getDocuments().get(0);
                        currentPayment = doc.toObject(Payment.class);
                        currentPaymentId = doc.getId();
                        if (currentPayment != null) {
                            updatePaymentUI(currentPayment.getStatus());
                        }
                    } else {
                        currentPayment = null;
                        currentPaymentId = null;
                        updatePaymentUI(Payment.STATUS_PENDING);
                    }
                });
    }

    private void updatePaymentUI(String status) {
        binding.tvPaymentStatus.setText(status);
        if (Payment.STATUS_PAID.equals(status)) {
            binding.tvPaymentStatus.setTextColor(getColor(R.color.status_paid));
            binding.btnMarkPaid.setText(R.string.mark_as_unpaid);
        } else {
            binding.tvPaymentStatus.setTextColor(getColor(R.color.status_unpaid));
            binding.btnMarkPaid.setText(R.string.mark_as_paid);
        }
    }

    private void togglePaymentStatus() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null)
            return;

        if (currentPayment != null && Payment.STATUS_PAID.equals(currentPayment.getStatus())) {
            // Mark as unpaid
            currentPayment.setStatus(Payment.STATUS_PENDING);
            currentPayment.setPaidAmount(0);
            currentPayment.setPaidDate(null);
            firebaseHelper.updatePayment(currentPaymentId, currentPayment)
                    .addOnSuccessListener(aVoid -> {
                        updatePaymentUI(Payment.STATUS_PENDING);
                        Toast.makeText(this, R.string.payment_updated, Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Mark as paid
            if (currentPayment == null) {
                String customerName = binding.tvCustomerName.getText().toString();
                currentPayment = new Payment(customerId, customerName, userId,
                        currentMonth, currentYear, totalAmount);
            }
            currentPayment.setStatus(Payment.STATUS_PAID);
            currentPayment.setPaidAmount(totalAmount);
            currentPayment.setPaidDate(Timestamp.now());
            currentPayment.setTotalAmount(totalAmount);

            if (currentPaymentId != null) {
                firebaseHelper.updatePayment(currentPaymentId, currentPayment)
                        .addOnSuccessListener(aVoid -> {
                            updatePaymentUI(Payment.STATUS_PAID);
                            Toast.makeText(this, R.string.payment_updated, Toast.LENGTH_SHORT).show();
                        });
            } else {
                firebaseHelper.addPayment(currentPayment)
                        .addOnSuccessListener(docRef -> {
                            currentPaymentId = docRef.getId();
                            updatePaymentUI(Payment.STATUS_PAID);
                            Toast.makeText(this, R.string.payment_updated, Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void downloadPdf() {
        if (customer == null || entryList.isEmpty()) {
            Toast.makeText(this, "No entries to generate bill", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentStatus = (currentPayment != null) ? currentPayment.getStatus() : Payment.STATUS_PENDING;

        PdfGenerator.generateMonthlyBill(this, vendorName, customer, entryList, extraChargeList,
                currentMonth, currentYear, totalAmount, paymentStatus);
    }
}
