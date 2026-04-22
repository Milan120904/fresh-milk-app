package com.example.freshmilk.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.MonthlyBreakdownAdapter;
import com.example.freshmilk.databinding.ActivityRevenueDetailBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RevenueDetailActivity extends AppCompatActivity {

    private static final String TAG = "RevenueDetailActivity";
    private ActivityRevenueDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private int selectedYear;
    private ListenerRegistration revenueListener;

    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final String[] MONTH_SHORT = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRevenueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        updateYearDisplay();
        setupChart();
        setupYearNavigation();
        loadRevenueData();
    }

    private void setupYearNavigation() {
        binding.btnPrevYear.setOnClickListener(v -> {
            selectedYear--;
            updateYearDisplay();
            loadRevenueData();
        });

        binding.btnNextYear.setOnClickListener(v -> {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (selectedYear < currentYear + 1) {
                selectedYear++;
                updateYearDisplay();
                loadRevenueData();
            }
        });
    }

    private void updateYearDisplay() {
        binding.tvCurrentYear.setText(String.valueOf(selectedYear));

        // Disable next button if already at current year + 1
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        binding.btnNextYear.setAlpha(selectedYear >= currentYear + 1 ? 0.3f : 1.0f);
        binding.btnNextYear.setEnabled(selectedYear < currentYear + 1);
    }

    private void setupChart() {
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setDrawBarShadow(false);
        binding.barChart.setFitBars(true);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.setExtraBottomOffset(10f);

        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextSize(11f);

        binding.barChart.getAxisLeft().setDrawGridLines(true);
        binding.barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        binding.barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) {
                    return String.format(Locale.getDefault(), "₹%.0fK", value / 1000);
                }
                return String.format(Locale.getDefault(), "₹%.0f", value);
            }
        });
        binding.barChart.getAxisRight().setEnabled(false);
    }

    private void loadRevenueData() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) return;

        // Remove previous listener
        if (revenueListener != null) {
            revenueListener.remove();
        }

        // Use snapshot listener for real-time updates + cache
        // Use simple query (getPaymentsByUser) to avoid composite index issues
        revenueListener = firebaseHelper.getPaymentsByUser(userId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Revenue listener error", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    float[] monthlyEarnings = new float[12];
                    double totalRevenue = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Payment payment = doc.toObject(Payment.class);
                        // Filter for selected year and PAID status on client side
                        if (payment.getYear() == selectedYear && Payment.STATUS_PAID.equals(payment.getStatus())) {
                            int monthIndex = payment.getMonth() - 1;
                            if (monthIndex >= 0 && monthIndex < 12) {
                                monthlyEarnings[monthIndex] += (float) payment.getPaidAmount();
                                totalRevenue += payment.getPaidAmount();
                            }
                        }
                    }

                    // Format in Indian Rupee
                    binding.tvTotalRevenue.setText(formatIndianRupee(totalRevenue));
                    updateChart(monthlyEarnings);
                    updateBreakdownList(monthlyEarnings);
                });
    }

    /**
     * Formats amount in Indian Rupee style: ₹1,23,456
     */
    private String formatIndianRupee(double amount) {
        if (amount == 0) return "₹0";

        long rupees = Math.round(amount);
        boolean negative = rupees < 0;
        if (negative) rupees = -rupees;

        String numStr = String.valueOf(rupees);
        StringBuilder formatted = new StringBuilder();

        int len = numStr.length();
        if (len <= 3) {
            formatted.append(numStr);
        } else {
            // Last 3 digits
            formatted.insert(0, numStr.substring(len - 3));
            int remaining = len - 3;
            int pos = len - 3;
            // Group remaining in pairs
            while (remaining > 0) {
                int take = Math.min(2, remaining);
                formatted.insert(0, ",");
                formatted.insert(0, numStr.substring(pos - take, pos));
                pos -= take;
                remaining -= take;
            }
        }

        return (negative ? "-₹" : "₹") + formatted;
    }

    private void updateChart(float[] monthlyEarnings) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, monthlyEarnings[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Earnings");
        dataSet.setColor(Color.parseColor("#1976D2"));
        dataSet.setValueTextColor(Color.parseColor("#424242"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                if (value >= 1000) {
                    return String.format(Locale.getDefault(), "₹%.0fK", value / 1000);
                }
                return String.format(Locale.getDefault(), "₹%.0f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        binding.barChart.setData(barData);
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(MONTH_SHORT));
        binding.barChart.animateY(1500, Easing.EaseInOutQuad);
        binding.barChart.invalidate();
    }

    private void updateBreakdownList(float[] monthlyEarnings) {
        List<MonthlyBreakdownAdapter.MonthData> breakdownList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            if (monthlyEarnings[i] > 0) {
                breakdownList.add(new MonthlyBreakdownAdapter.MonthData(
                        MONTHS[i], monthlyEarnings[i]));
            }
        }

        MonthlyBreakdownAdapter adapter = new MonthlyBreakdownAdapter(breakdownList);
        binding.rvBreakdown.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBreakdown.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (revenueListener != null) {
            revenueListener.remove();
        }
    }
}
