package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.DailyEntryAdapter;
import com.example.freshmilk.databinding.ActivityMilkDeliveryDetailBinding;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MilkDeliveryDetailActivity extends AppCompatActivity {

    private ActivityMilkDeliveryDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private DailyEntryAdapter adapter;
    private final List<DailyEntry> entryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMilkDeliveryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        loadMonthlyEntries();
    }

    private void setupRecyclerView() {
        adapter = new DailyEntryAdapter(entryList);
        binding.rvEntries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEntries.setAdapter(adapter);
    }

    private void loadMonthlyEntries() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) return;

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        String monthPrefix = String.format(Locale.getDefault(), "%d-%02d", currentYear, currentMonth);

        firebaseHelper.getAllEntriesByVendor(userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entryList.clear();
                    double totalMilk = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        entry.setEntryId(doc.getId());

                        if (entry.getDate() != null && entry.getDate().startsWith(monthPrefix)) {
                            entryList.add(entry);
                            totalMilk += entry.getTotalQty();
                        }
                    }

                    // Sort by date descending
                    Collections.sort(entryList, (a, b) -> {
                        if (a.getDate() == null || b.getDate() == null) return 0;
                        return b.getDate().compareTo(a.getDate());
                    });

                    adapter.notifyDataSetChanged();

                    binding.tvTotalMilk.setText(
                            String.format(Locale.getDefault(), "%.1f L", totalMilk));
                    binding.tvTotalEntries.setText(String.valueOf(entryList.size()));

                    if (entryList.isEmpty()) {
                        binding.rvEntries.setVisibility(View.GONE);
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvEntries.setVisibility(View.VISIBLE);
                        binding.emptyState.setVisibility(View.GONE);
                    }
                });
    }
}
