package com.example.freshmilk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.freshmilk.adapters.CustomerAdapter;
import com.example.freshmilk.databinding.ActivityCustomerListBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerListActivity extends AppCompatActivity implements CustomerAdapter.OnCustomerClickListener {

    private ActivityCustomerListBinding binding;
    private FirebaseHelper firebaseHelper;
    private CustomerAdapter adapter;
    private final List<Customer> customerList = new ArrayList<>();
    private final List<Customer> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        setupSearch();
        loadCustomers();
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(filteredList, this);
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(this));
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

    private void filterCustomers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(customerList);
        } else {
            for (Customer customer : customerList) {
                if (customer.getName().toLowerCase().contains(query.toLowerCase()) ||
                        customer.getMobile().contains(query)) {
                    filteredList.add(customer);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadCustomers() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) return;

        firebaseHelper.getCustomersByVendor(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    customerList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Customer customer = doc.toObject(Customer.class);
                        customer.setCustomerId(doc.getId());
                        customerList.add(customer);
                    }
                    filteredList.clear();
                    filteredList.addAll(customerList);
                    adapter.notifyDataSetChanged();
                    updateUI();
                });
    }

    private void updateUI() {
        binding.tvCustomerCount.setText(
                String.format(Locale.getDefault(), "%d customers", filteredList.size()));

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
        Intent intent = new Intent(this, CustomerDetailActivity.class);
        intent.putExtra("customerId", customer.getCustomerId());
        intent.putExtra("customerName", customer.getName());
        startActivity(intent);
    }
}
