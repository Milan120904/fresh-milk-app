package com.example.freshmilk.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel scoped to the Activity so data persists across fragment switches.
 * Uses Firestore snapshot listeners for real-time updates with local cache.
 */
public class DashboardViewModel extends ViewModel {

    private static final String TAG = "DashboardViewModel";
    private final FirebaseHelper firebaseHelper;

    private final MutableLiveData<Integer> totalCustomers = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> todayDeliveries = new MutableLiveData<>(0);
    private final MutableLiveData<Double> monthlyRevenue = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> pendingPayments = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Customer>> customerList = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration customerListener;
    private ListenerRegistration todayEntryListener;
    private ListenerRegistration revenueListener;
    private ListenerRegistration pendingListener;

    private boolean listenersAttached = false;
    private String cachedUserId = null;

    public DashboardViewModel() {
        firebaseHelper = FirebaseHelper.getInstance();
    }

    // Getters
    public LiveData<Integer> getTotalCustomers() { return totalCustomers; }
    public LiveData<Integer> getTodayDeliveries() { return todayDeliveries; }
    public LiveData<Double> getMonthlyRevenue() { return monthlyRevenue; }
    public LiveData<Integer> getPendingPayments() { return pendingPayments; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<Customer>> getCustomerList() { return customerList; }

    /**
     * Attach snapshot listeners ONCE. They will auto-update from cache + network.
     * Does NOT reload if already attached for the same user.
     */
    public void loadDashboardData(String userId) {
        if (userId == null) return;

        // Already listening for this user — skip
        if (listenersAttached && userId.equals(cachedUserId)) return;

        // Different user or first time — attach listeners
        removeListeners();
        cachedUserId = userId;
        isLoading.setValue(true);
        listenersAttached = true;

        attachCustomerListener(userId);
        attachTodayEntryListener(userId);
        attachRevenueListener(userId);
        attachPendingListener(userId);
    }

    private void attachCustomerListener(String userId) {
        // Use simple query (no orderBy) to avoid composite index requirement
        customerListener = firebaseHelper.getCustomersByVendorSimple(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Customer listener error", error);
                        return;
                    }
                    if (value == null) return;

                    int activeCount = 0;
                    List<Customer> customers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Customer c = doc.toObject(Customer.class);
                        c.setCustomerId(doc.getId());
                        customers.add(c);
                        if (c.isActive()) activeCount++;
                    }

                    // Sort alphabetically on client side
                    customers.sort((a, b) -> {
                        String nameA = a.getName() != null ? a.getName() : "";
                        String nameB = b.getName() != null ? b.getName() : "";
                        return nameA.compareToIgnoreCase(nameB);
                    });

                    totalCustomers.setValue(activeCount);
                    customerList.setValue(customers);
                });
    }

    private void attachTodayEntryListener(String userId) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        todayEntryListener = firebaseHelper.getTodaysEntries(userId, today)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Today entries listener error", error);
                        return;
                    }
                    todayDeliveries.setValue(value != null ? value.size() : 0);
                    isLoading.setValue(false);
                });
    }

    private void attachRevenueListener(String userId) {
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);

        revenueListener = firebaseHelper.getPaymentsByUserAndMonth(userId, currentMonth, currentYear)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Revenue listener error", error);
                        return;
                    }
                    if (value == null) return;

                    double total = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Payment payment = doc.toObject(Payment.class);
                        total += payment.getPaidAmount();
                    }
                    monthlyRevenue.setValue(total);
                });
    }

    private void attachPendingListener(String userId) {
        pendingListener = firebaseHelper.getUnpaidPayments(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Pending listener error", error);
                        return;
                    }
                    pendingPayments.setValue(value != null ? value.size() : 0);
                });
    }

    private void removeListeners() {
        if (customerListener != null) { customerListener.remove(); customerListener = null; }
        if (todayEntryListener != null) { todayEntryListener.remove(); todayEntryListener = null; }
        if (revenueListener != null) { revenueListener.remove(); revenueListener = null; }
        if (pendingListener != null) { pendingListener.remove(); pendingListener = null; }
        listenersAttached = false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }
}
