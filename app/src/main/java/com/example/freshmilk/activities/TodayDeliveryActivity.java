package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.DailyEntryAdapter;
import com.example.freshmilk.databinding.ActivityTodayDeliveryBinding;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayDeliveryActivity extends AppCompatActivity {

    private ActivityTodayDeliveryBinding binding;
    private FirebaseHelper firebaseHelper;
    private DailyEntryAdapter adapter;
    private final List<DailyEntry> entryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTodayDeliveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Set today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String displayDate = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date());
        binding.tvDate.setText(displayDate);

        setupRecyclerView();
        loadTodayEntries(today);
    }

    private void setupRecyclerView() {
        adapter = new DailyEntryAdapter(entryList);
        binding.rvEntries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEntries.setAdapter(adapter);
    }

    private void loadTodayEntries(String today) {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) return;

        firebaseHelper.getTodaysEntries(userId, today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entryList.clear();
                    double totalMilk = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        entry.setEntryId(doc.getId());
                        entryList.add(entry);
                        totalMilk += entry.getTotalQty();
                    }

                    adapter.notifyDataSetChanged();

                    binding.tvTotalEntries.setText(String.valueOf(entryList.size()));
                    binding.tvTotalMilk.setText(
                            String.format(Locale.getDefault(), "%.1f L", totalMilk));

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
