package com.example.freshmilk.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel scoped to the Activity so data persists across fragment switches.
 * Uses snapshot listeners for real-time data with cache-first approach.
 */
public class ReportsViewModel extends ViewModel {

    private static final String TAG = "ReportsViewModel";
    private final FirebaseHelper firebaseHelper;

    private final MutableLiveData<Integer> totalCustomers = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalMilk = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalEarned = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalPending = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> collectionRate = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<Payment>> paymentList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<float[]> monthlyEarnings = new MutableLiveData<>(new float[12]);
    private final MutableLiveData<Integer> paidCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unpaidCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private ListenerRegistration customerListener;
    private ListenerRegistration entriesListener;
    private ListenerRegistration paymentsListener;

    private boolean listenersAttached = false;
    private String cachedUserId = null;

    public ReportsViewModel() {
        firebaseHelper = FirebaseHelper.getInstance();
    }

    // Getters
    public LiveData<Integer> getTotalCustomers() { return totalCustomers; }
    public LiveData<Double> getTotalMilk() { return totalMilk; }
    public LiveData<Double> getTotalEarned() { return totalEarned; }
    public LiveData<Double> getTotalPending() { return totalPending; }
    public LiveData<Double> getCollectionRate() { return collectionRate; }
    public LiveData<List<Payment>> getPaymentList() { return paymentList; }
    public LiveData<float[]> getMonthlyEarnings() { return monthlyEarnings; }
    public LiveData<Integer> getPaidCount() { return paidCount; }
    public LiveData<Integer> getUnpaidCount() { return unpaidCount; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * Attach snapshot listeners ONCE. They auto-update from cache + network.
     */
    public void loadReportsData(String userId) {
        if (userId == null) return;
        if (listenersAttached && userId.equals(cachedUserId)) return;

        removeListeners();
        cachedUserId = userId;
        isLoading.setValue(true);
        listenersAttached = true;

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

        attachCustomerListener(userId);
        attachEntriesListener(userId, currentYear, currentMonth);
        attachPaymentsListener(userId, currentYear);
    }

    private void attachCustomerListener(String userId) {
        customerListener = firebaseHelper.getCustomersByVendor(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.w(TAG, "Customer listener error", error); return; }
                    if (value == null) return;

                    int activeCount = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Boolean active = doc.getBoolean("active");
                        if (active != null && active) activeCount++;
                    }
                    totalCustomers.setValue(activeCount);
                });
    }

    private void attachEntriesListener(String userId, int currentYear, int currentMonth) {
        String monthPrefix = String.format(Locale.getDefault(), "%d-%02d", currentYear, currentMonth);

        entriesListener = firebaseHelper.getAllEntriesByVendor(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.w(TAG, "Entries listener error", error); return; }
                    if (value == null) return;

                    double milk = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        DailyEntry entry = doc.toObject(DailyEntry.class);
                        if (entry.getDate() != null && entry.getDate().startsWith(monthPrefix)) {
                            milk += entry.getTotalQty();
                        }
                    }
                    totalMilk.setValue(milk);
                });
    }

    private void attachPaymentsListener(String userId, int currentYear) {
        paymentsListener = firebaseHelper.getPaymentsByUser(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.w(TAG, "Payments listener error", error); return; }
                    if (value == null) return;

                    double earned = 0, pending = 0;
                    int paid = 0, unpaidVal = 0;
                    List<Payment> payments = new ArrayList<>();
                    float[] earnings = new float[12];

                    for (QueryDocumentSnapshot doc : value) {
                        Payment payment = doc.toObject(Payment.class);
                        payment.setPaymentId(doc.getId());
                        payments.add(payment);

                        if (Payment.STATUS_PAID.equals(payment.getStatus())) {
                            earned += payment.getPaidAmount();
                            paid++;
                            if (payment.getYear() == currentYear) {
                                int monthIndex = payment.getMonth() - 1;
                                if (monthIndex >= 0 && monthIndex < 12) {
                                    earnings[monthIndex] += (float) payment.getPaidAmount();
                                }
                            }
                        } else {
                            pending += payment.getTotalAmount() - payment.getPaidAmount();
                            unpaidVal++;
                        }
                    }

                    paymentList.setValue(payments);
                    totalEarned.setValue(earned);
                    totalPending.setValue(pending);
                    paidCount.setValue(paid);
                    unpaidCount.setValue(unpaidVal);
                    monthlyEarnings.setValue(earnings);

                    double total = earned + pending;
                    double rate = total > 0 ? (earned / total) * 100 : 0;
                    collectionRate.setValue(rate);

                    isLoading.setValue(false);
                });
    }

    private void removeListeners() {
        if (customerListener != null) { customerListener.remove(); customerListener = null; }
        if (entriesListener != null) { entriesListener.remove(); entriesListener = null; }
        if (paymentsListener != null) { paymentsListener.remove(); paymentsListener = null; }
        listenersAttached = false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }
}
