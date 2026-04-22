package com.example.freshmilk.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.R;
import com.example.freshmilk.adapters.DailyEntryAdapter;
import com.example.freshmilk.databinding.ActivityCustomerDetailBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends AppCompatActivity
        implements DailyEntryAdapter.OnEntryActionListener {

    private ActivityCustomerDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private String customerId;
    private Customer customer;
    private DailyEntryAdapter entryAdapter;
    private List<DailyEntry> entryList = new ArrayList<>();
    private ListenerRegistration customerListener;
    private ListenerRegistration entriesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        customerId = getIntent().getStringExtra("customerId");

        setupRecyclerView();
        setupClickListeners();
        loadCustomerDetails();
        loadEntries();
    }

    private void setupRecyclerView() {
        entryAdapter = new DailyEntryAdapter(entryList, this);
        binding.rvEntries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEntries.setAdapter(entryAdapter);
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.ivEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCustomerActivity.class);
            intent.putExtra("customerId", customerId);
            startActivity(intent);
        });

        binding.btnAddEntry.setOnClickListener(v -> {
            Intent intent = new Intent(this, DailyEntryActivity.class);
            intent.putExtra("customerId", customerId);
            if (customer != null) {
                intent.putExtra("customerName", customer.getName());
                intent.putExtra("ratePerLiter", customer.getRatePerLiter());
                intent.putExtra("defaultQuantity", customer.getDefaultQuantity());
                intent.putExtra("vendorId", customer.getVendorId());
            }
            startActivity(intent);
        });

        binding.btnViewBill.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillActivity.class);
            intent.putExtra("customerId", customerId);
            if (customer != null) {
                intent.putExtra("customerName", customer.getName());
            }
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v -> confirmDeleteCustomer());

        // View on Map button
        binding.btnViewOnMap.setOnClickListener(v -> openCustomerLocationOnMap());
    }

    private void openCustomerLocationOnMap() {
        if (customer == null) return;

        double lat = customer.getLatitude();
        double lng = customer.getLongitude();

        // Check if location is set (0,0 means not set)
        if (lat == 0 && lng == 0) {
            Toast.makeText(this, R.string.location_not_set, Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = customer.getName() != null ? customer.getName() : "";

        // Try to open in Google Maps app
        Uri gmmIntentUri = Uri.parse(String.format(Locale.US,
                "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng,
                Uri.encode(customerName)));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            // Google Maps not installed — open in browser
            Toast.makeText(this, R.string.maps_not_installed, Toast.LENGTH_SHORT).show();
            String browserUrl = String.format(Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl));
            startActivity(browserIntent);
        }
    }

    private void loadCustomerDetails() {
        customerListener = firebaseHelper.getCustomersCollection().document(customerId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists())
                        return;

                    customer = documentSnapshot.toObject(Customer.class);
                    if (customer != null) {
                        customer.setCustomerId(documentSnapshot.getId());
                        binding.tvCustomerName.setText(customer.getName());
                        binding.tvTitle.setText(customer.getName());
                        binding.tvMilkType.setText(customer.getMilkType());
                        binding.tvMobile.setText(customer.getMobile());
                        binding.tvRate
                                .setText(String.format(Locale.getDefault(), "₹%.0f/L", customer.getRatePerLiter()));
                        binding.tvQuantity
                                .setText(String.format(Locale.getDefault(), "%.1f L", customer.getDefaultQuantity()));
                        binding.tvAddress.setText("📍 " + customer.getAddress());

                        // Show/hide map button based on location availability
                        if (customer.getLatitude() != 0 || customer.getLongitude() != 0) {
                            binding.btnViewOnMap.setVisibility(View.VISIBLE);
                        } else {
                            binding.btnViewOnMap.setVisibility(View.VISIBLE); // Always show, will show toast if not set
                        }
                    }
                });
    }

    private void loadEntries() {
        // Use simple query (no orderBy) to avoid composite index requirement
        entriesListener = firebaseHelper.getEntriesByCustomerSimple(customerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null)
                        return;

                    entryList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        entry.setEntryId(doc.getId());
                        entryList.add(entry);
                    }

                    // Sort by date descending (newest first) on client side
                    Collections.sort(entryList, (a, b) -> {
                        String dateA = a.getDate() != null ? a.getDate() : "";
                        String dateB = b.getDate() != null ? b.getDate() : "";
                        return dateB.compareTo(dateA);
                    });

                    entryAdapter.notifyDataSetChanged();

                    binding.tvNoEntries.setVisibility(entryList.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvEntries.setVisibility(entryList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // ==================== ENTRY ACTIONS (Vendor) ====================

    @Override
    public void onEditEntry(DailyEntry entry) {
        Intent intent = new Intent(this, DailyEntryActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("entryId", entry.getEntryId());
        intent.putExtra("date", entry.getDate());
        intent.putExtra("morningQty", entry.getMorningQty());
        intent.putExtra("eveningQty", entry.getEveningQty());
        if (customer != null) {
            intent.putExtra("customerName", customer.getName());
            intent.putExtra("ratePerLiter", customer.getRatePerLiter());
            intent.putExtra("defaultQuantity", customer.getDefaultQuantity());
            intent.putExtra("vendorId", customer.getVendorId());
        }
        startActivity(intent);
    }

    @Override
    public void onDeleteEntry(DailyEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Delete entry for " + entry.getDate() + "?\n\n" +
                        "Morning: " + String.format(Locale.getDefault(), "%.1f L", entry.getMorningQty()) + "\n" +
                        "Evening: " + String.format(Locale.getDefault(), "%.1f L", entry.getEveningQty()))
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (entry.getEntryId() != null) {
                        firebaseHelper.deleteDailyEntry(entry.getEntryId())
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Cannot delete — entry ID missing", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteCustomer() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete))
                .setMessage("Are you sure you want to delete this customer?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    firebaseHelper.deleteCustomer(customerId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, R.string.customer_deleted, Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (customerListener != null) customerListener.remove();
        if (entriesListener != null) entriesListener.remove();
    }
}
