package com.example.freshmilk.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityCustomerMainBinding;
import com.example.freshmilk.fragments.CustomerBillsFragment;
import com.example.freshmilk.fragments.CustomerHomeFragment;
import com.example.freshmilk.fragments.ProfileFragment;

public class CustomerMainActivity extends AppCompatActivity {

    private ActivityCustomerMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new CustomerHomeFragment());
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_customer_home) {
                fragment = new CustomerHomeFragment();
            } else if (itemId == R.id.nav_customer_bills) {
                fragment = new CustomerBillsFragment();
            } else if (itemId == R.id.nav_customer_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
