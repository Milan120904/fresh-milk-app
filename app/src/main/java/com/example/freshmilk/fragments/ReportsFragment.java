package com.example.freshmilk.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.activities.CustomerListActivity;
import com.example.freshmilk.activities.MilkDeliveryDetailActivity;
import com.example.freshmilk.activities.PendingPaymentsActivity;
import com.example.freshmilk.activities.RevenueDetailActivity;
import com.example.freshmilk.adapters.PaymentAdapter;
import com.example.freshmilk.databinding.FragmentReportsBinding;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.example.freshmilk.viewmodels.ReportsViewModel;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel viewModel;
    private PaymentAdapter paymentAdapter;
    private List<Payment> paymentList = new ArrayList<>();

    private static final String[] MONTH_SHORT = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Scope ViewModel to ACTIVITY — survives fragment replacement on tab switch
        viewModel = new ViewModelProvider(requireActivity()).get(ReportsViewModel.class);

        setupRecyclerView();
        setupBarChart();
        setupPieChart();
        setupCardClickListeners();
        observeViewModel();

        // Attach snapshot listeners once (ViewModel skips if already attached)
        String userId = FirebaseHelper.getInstance().getCurrentUserId();
        viewModel.loadReportsData(userId);
    }

    private void setupRecyclerView() {
        paymentAdapter = new PaymentAdapter(paymentList, null);
        binding.rvPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPayments.setAdapter(paymentAdapter);
    }

    private void observeViewModel() {
        viewModel.getTotalCustomers().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) binding.tvTotalCustomers.setText(String.valueOf(count));
        });

        viewModel.getTotalMilk().observe(getViewLifecycleOwner(), milk -> {
            if (binding != null) binding.tvTotalMilk.setText(
                    String.format(Locale.getDefault(), "%.1f L", milk));
        });

        viewModel.getTotalEarned().observe(getViewLifecycleOwner(), earned -> {
            if (binding != null) binding.tvTotalEarned.setText(
                    String.format(Locale.getDefault(), "₹%.0f", earned));
        });

        viewModel.getTotalPending().observe(getViewLifecycleOwner(), pending -> {
            if (binding != null) binding.tvTotalPending.setText(
                    String.format(Locale.getDefault(), "₹%.0f", pending));
        });

        viewModel.getCollectionRate().observe(getViewLifecycleOwner(), rate -> {
            if (binding != null) binding.tvCollectionRate.setText(
                    String.format(Locale.getDefault(), "%.0f%%", rate));
        });

        viewModel.getPaymentList().observe(getViewLifecycleOwner(), payments -> {
            if (binding != null) {
                paymentList.clear();
                paymentList.addAll(payments);
                paymentAdapter.notifyDataSetChanged();
            }
        });

        viewModel.getMonthlyEarnings().observe(getViewLifecycleOwner(), earnings -> {
            if (binding != null) updateBarChart(earnings);
        });

        viewModel.getPaidCount().observe(getViewLifecycleOwner(), paid -> {
            Integer unpaid = viewModel.getUnpaidCount().getValue();
            if (binding != null && unpaid != null) updatePieChart(paid, unpaid);
        });

        viewModel.getUnpaidCount().observe(getViewLifecycleOwner(), unpaid -> {
            Integer paid = viewModel.getPaidCount().getValue();
            if (binding != null && paid != null) updatePieChart(paid, unpaid);
        });
    }

    private void setupBarChart() {
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
        xAxis.setTextSize(10f);

        binding.barChart.getAxisLeft().setDrawGridLines(true);
        binding.barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        binding.barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) return String.format(Locale.getDefault(), "₹%.0fK", value / 1000);
                return String.format(Locale.getDefault(), "₹%.0f", value);
            }
        });
        binding.barChart.getAxisRight().setEnabled(false);
    }

    private void updateBarChart(float[] monthlyEarnings) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, monthlyEarnings[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Earnings");
        dataSet.setColor(Color.parseColor("#1976D2"));
        dataSet.setValueTextColor(Color.parseColor("#424242"));
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                if (value >= 1000) return String.format(Locale.getDefault(), "₹%.0fK", value / 1000);
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

    private void setupPieChart() {
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setHoleRadius(45f);
        binding.pieChart.setTransparentCircleRadius(50f);
        binding.pieChart.setTransparentCircleColor(Color.WHITE);
        binding.pieChart.setTransparentCircleAlpha(100);
        binding.pieChart.setDrawCenterText(true);
        binding.pieChart.setCenterText("Payments");
        binding.pieChart.setCenterTextSize(14f);
        binding.pieChart.setCenterTextColor(Color.parseColor("#424242"));
        binding.pieChart.getLegend().setEnabled(true);
        binding.pieChart.getLegend().setTextSize(12f);
        binding.pieChart.setEntryLabelTextSize(12f);
        binding.pieChart.setEntryLabelColor(Color.WHITE);
    }

    private void updatePieChart(int paid, int unpaid) {
        if (paid == 0 && unpaid == 0) {
            binding.pieChart.clear();
            binding.pieChart.setCenterText("No Data");
            binding.pieChart.invalidate();
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        if (paid > 0) pieEntries.add(new PieEntry(paid, "Paid"));
        if (unpaid > 0) pieEntries.add(new PieEntry(unpaid, "Unpaid"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        List<Integer> colors = new ArrayList<>();
        if (paid > 0) colors.add(Color.parseColor("#4CAF50"));
        if (unpaid > 0) colors.add(Color.parseColor("#F44336"));
        pieDataSet.setColors(colors);
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(8f);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueFormatter(new PercentFormatter(binding.pieChart));

        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.setCenterText("Payments");
        binding.pieChart.animateY(1200, Easing.EaseInOutQuad);
        binding.pieChart.invalidate();
    }

    private void setupCardClickListeners() {
        binding.cardTotalCustomers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CustomerListActivity.class)));

        binding.cardTotalMilk.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MilkDeliveryDetailActivity.class)));

        binding.cardTotalEarned.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RevenueDetailActivity.class)));

        binding.cardTotalPending.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PendingPaymentsActivity.class)));
    }

    // NO onResume() reload — ViewModel snapshot listeners handle real-time updates

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
