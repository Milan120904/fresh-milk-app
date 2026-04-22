package com.example.freshmilk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.freshmilk.activities.AddCustomerActivity;
import com.example.freshmilk.activities.CustomerListActivity;
import com.example.freshmilk.activities.PaymentVerificationActivity;
import com.example.freshmilk.activities.PendingPaymentsActivity;
import com.example.freshmilk.activities.RevenueDetailActivity;
import com.example.freshmilk.activities.TodayDeliveryActivity;
import com.example.freshmilk.activities.VendorPaymentsActivity;
import com.example.freshmilk.databinding.FragmentHomeBinding;
import com.example.freshmilk.utils.FirebaseHelper;
import com.example.freshmilk.viewmodels.DashboardViewModel;

import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Scope ViewModel to ACTIVITY — survives fragment replacement on tab switch
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        setupGreeting();
        observeViewModel();
        setupClickListeners();
        setupCardClickListeners();

        // Attach snapshot listeners once (ViewModel skips if already attached)
        String userId = FirebaseHelper.getInstance().getCurrentUserId();
        viewModel.loadDashboardData(userId);
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning! ☀️";
        } else if (hour < 17) {
            greeting = "Good Afternoon! 🌤️";
        } else {
            greeting = "Good Evening! 🌙";
        }
        binding.tvGreeting.setText(greeting);
    }

    private void observeViewModel() {
        viewModel.getTotalCustomers().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) binding.tvTotalCustomers.setText(String.valueOf(count));
        });

        viewModel.getTodayDeliveries().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) binding.tvTodayDeliveries.setText(String.valueOf(count));
        });

        viewModel.getMonthlyRevenue().observe(getViewLifecycleOwner(), revenue -> {
            if (binding != null) binding.tvMonthlyRevenue.setText(
                    String.format(Locale.getDefault(), "₹%.0f", revenue));
        });

        viewModel.getPendingPayments().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) binding.tvPendingPayments.setText(String.valueOf(count));
        });
    }

    private void setupClickListeners() {
        binding.btnNewCustomer.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddCustomerActivity.class)));

        binding.btnVerifyPayments.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PaymentVerificationActivity.class)));
    }

    private void setupCardClickListeners() {
        binding.cardTotalCustomers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CustomerListActivity.class)));

        binding.cardTodayDelivery.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TodayDeliveryActivity.class)));

        binding.cardMonthlyRevenue.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RevenueDetailActivity.class)));

        binding.cardPendingPayments.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VendorPaymentsActivity.class)));
    }

    // NO onResume() reload — ViewModel snapshot listeners handle real-time updates

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
