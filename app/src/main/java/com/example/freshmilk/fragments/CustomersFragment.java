package com.example.freshmilk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.activities.AddCustomerActivity;
import com.example.freshmilk.activities.CustomerDetailActivity;
import com.example.freshmilk.adapters.CustomerAdapter;
import com.example.freshmilk.databinding.FragmentCustomersBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.utils.FirebaseHelper;
import com.example.freshmilk.viewmodels.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses DashboardViewModel (activity-scoped) to share customer data
 * with HomeFragment. No duplicate Firebase calls.
 */
public class CustomersFragment extends Fragment implements CustomerAdapter.OnCustomerClickListener {

    private FragmentCustomersBinding binding;
    private DashboardViewModel viewModel;
    private CustomerAdapter adapter;
    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share the same ViewModel as HomeFragment — no duplicate fetches
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        setupRecyclerView();
        setupSearch();
        observeViewModel();

        binding.btnAddCustomer
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), AddCustomerActivity.class)));

        // Ensure data is loaded (ViewModel skips if listeners already attached)
        String userId = FirebaseHelper.getInstance().getCurrentUserId();
        viewModel.loadDashboardData(userId);
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(filteredList, this);
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomers.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getCustomerList().observe(getViewLifecycleOwner(), customers -> {
            if (binding == null) return;

            allCustomers.clear();
            allCustomers.addAll(customers);

            // Reapply any active search filter
            String query = binding.etSearch.getText().toString().trim();
            if (query.isEmpty()) {
                filteredList.clear();
                filteredList.addAll(allCustomers);
            } else {
                filterCustomers(query);
                return; // filterCustomers already calls notifyDataSetChanged
            }
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void filterCustomers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allCustomers);
        } else {
            for (Customer customer : allCustomers) {
                if (customer.getName().toLowerCase().contains(query.toLowerCase()) ||
                        customer.getMobile().contains(query)) {
                    filteredList.add(customer);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            binding.rvCustomers.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvCustomers.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCustomerClick(Customer customer) {
        Intent intent = new Intent(requireContext(), CustomerDetailActivity.class);
        intent.putExtra("customerId", customer.getCustomerId());
        intent.putExtra("customerName", customer.getName());
        startActivity(intent);
    }

    // NO onResume() reload — ViewModel snapshot listeners handle real-time updates

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
