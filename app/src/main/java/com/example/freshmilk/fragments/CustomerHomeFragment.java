package com.example.freshmilk.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.freshmilk.databinding.FragmentCustomerHomeBinding;
import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CustomerHomeFragment extends Fragment {

    private static final String TAG = "CustomerHomeFragment";
    private FragmentCustomerHomeBinding binding;
    private FirebaseHelper firebaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();

        setupGreeting();
        loadCustomerData();
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

    private void loadCustomerData() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) {
            showNoData();
            return;
        }

        // Use snapshot listener for real-time updates from cache
        firebaseHelper.getCustomersCollection().document(userId)
                .addSnapshotListener((doc, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        Log.e(TAG, "Customer listener error", error);
                        showNoData();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer != null) {
                            customer.setCustomerId(doc.getId());
                            displayCustomerInfo(customer);
                            loadTodayEntry(doc.getId());
                        }
                    } else {
                        Log.w(TAG, "No customer document for userId: " + userId);
                        showNoData();
                    }
                });
    }

    private void showNoData() {
        binding.tvCustomerName.setText("Welcome!");
        binding.tvMilkType.setText("No linked account yet");
        binding.tvQuantity.setText("—");
        binding.tvRate.setText("—");
        binding.tvNoEntry.setVisibility(View.VISIBLE);
        binding.cardTodayEntry.setVisibility(View.GONE);
    }

    private void displayCustomerInfo(Customer customer) {
        binding.tvCustomerName.setText(customer.getName());
        binding.tvMilkType.setText(customer.getMilkType());
        binding.tvQuantity.setText(String.format(Locale.getDefault(), "%.1f L/day", customer.getDefaultQuantity()));
        binding.tvRate.setText(String.format(Locale.getDefault(), "₹%.0f/L", customer.getRatePerLiter()));
    }

    private void loadTodayEntry(String customerId) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Use snapshot listener for today's entries — auto-updates when vendor adds entry
        firebaseHelper.getEntriesByCustomerSimple(customerId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        Log.e(TAG, "Entry listener error", error);
                        binding.tvNoEntry.setVisibility(View.VISIBLE);
                        binding.cardTodayEntry.setVisibility(View.GONE);
                        return;
                    }
                    if (querySnapshot == null) return;

                    boolean foundToday = false;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        if (today.equals(entry.getDate())) {
                            foundToday = true;
                            binding.cardTodayEntry.setVisibility(View.VISIBLE);
                            binding.tvNoEntry.setVisibility(View.GONE);
                            binding.tvMorningQty
                                    .setText(String.format(Locale.getDefault(), "%.1f L", entry.getMorningQty()));
                            binding.tvEveningQty
                                    .setText(String.format(Locale.getDefault(), "%.1f L", entry.getEveningQty()));
                            binding.tvTotalQty
                                    .setText(String.format(Locale.getDefault(), "%.1f L", entry.getTotalQty()));
                            binding.tvDailyAmount
                                    .setText(String.format(Locale.getDefault(), "₹%.2f", entry.getDailyAmount()));
                            break;
                        }
                    }
                    if (!foundToday) {
                        binding.tvNoEntry.setVisibility(View.VISIBLE);
                        binding.cardTodayEntry.setVisibility(View.GONE);
                    }
                });
    }

    // No onResume() reload — snapshot listeners handle real-time updates

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
