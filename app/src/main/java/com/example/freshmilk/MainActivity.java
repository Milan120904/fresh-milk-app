package com.example.freshmilk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.freshmilk.databinding.ActivityMainBinding;
import com.example.freshmilk.fragments.CustomersFragment;
import com.example.freshmilk.fragments.HomeFragment;
import com.example.freshmilk.fragments.ProfileFragment;
import com.example.freshmilk.fragments.ReportsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_customers) {
                fragment = new CustomersFragment();
            } else if (itemId == R.id.nav_reports) {
                fragment = new ReportsFragment();
            } else if (itemId == R.id.nav_profile) {
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