package com.example.freshmilk.utils;

import android.util.Log;

import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.Payment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Utility to insert bulk dummy data for testing.
 * Creates 8 customers, daily entries for 4 months, and payment records.
 * Call DummyDataHelper.insertDummyData(vendorUserId, callback).
 */
public class DummyDataHelper {

    private static final String TAG = "DummyDataHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final Random random = new Random(42);

    // 8 dummy customers
    private static final String[][] CUSTOMERS = {
            {"Rajesh Sharma",    "9876543210", "rajesh@test.com",    "12 MG Road, Indore",    "Cow",     "2.0", "65"},
            {"Priya Patel",      "9876543211", "priya@test.com",     "45 Nehru Nagar, Bhopal", "Buffalo", "3.0", "75"},
            {"Amit Kumar",       "9876543212", "amit@test.com",      "78 Gandhi Colony, Jaipur","Cow",     "1.5", "60"},
            {"Sunita Devi",      "9876543213", "sunita@test.com",    "23 Laxmi Chowk, Pune",   "Mixed",   "2.5", "70"},
            {"Vikram Singh",     "9876543214", "vikram@test.com",    "56 Shivaji Nagar, Delhi", "Buffalo", "4.0", "80"},
            {"Meera Joshi",      "9876543215", "meera@test.com",     "89 Subhash Marg, Nagpur", "Cow",     "1.0", "55"},
            {"Rohit Verma",      "9876543216", "rohit@test.com",     "34 Civil Lines, Lucknow", "Cow",     "2.0", "65"},
            {"Anita Gupta",      "9876543217", "anita@test.com",     "67 Station Road, Kanpur", "Mixed",   "3.5", "72"},
    };

    public interface DummyDataCallback {
        void onSuccess(int customersAdded, int entriesAdded, int paymentsAdded);
        void onError(String error);
    }

    /**
     * Insert dummy data for the current vendor.
     * Creates customers + 4 months of daily entries + payment records.
     */
    public static void insertDummyData(String vendorUserId, DummyDataCallback callback) {
        if (vendorUserId == null) {
            callback.onError("No user logged in");
            return;
        }

        Log.d(TAG, "Starting dummy data insertion for vendor: " + vendorUserId);

        // Step 1: Create customers
        List<String> customerIds = new ArrayList<>();
        List<String> customerNames = new ArrayList<>();
        List<Double> customerRates = new ArrayList<>();
        List<Double> customerQtys = new ArrayList<>();

        WriteBatch customerBatch = db.batch();

        for (String[] cust : CUSTOMERS) {
            String docId = db.collection("Customers").document().getId();
            customerIds.add(docId);
            customerNames.add(cust[0]);

            double rate = Double.parseDouble(cust[6]);
            double qty = Double.parseDouble(cust[5]);
            customerRates.add(rate);
            customerQtys.add(qty);

            Customer customer = new Customer(vendorUserId, vendorUserId, cust[0], cust[1],
                    cust[3], cust[4], qty, rate);
            customer.setCustomerId(docId);
            customer.setEmail(cust[2]);
            customer.setActive(true);
            customer.setCreatedAt(new Date());

            customerBatch.set(db.collection("Customers").document(docId), customer);
        }

        customerBatch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Customers created: " + customerIds.size());
                    // Step 2: Create entries + payments
                    insertEntriesAndPayments(vendorUserId, customerIds, customerNames,
                            customerRates, customerQtys, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create customers", e);
                    callback.onError("Failed to create customers: " + e.getMessage());
                });
    }

    private static void insertEntriesAndPayments(String vendorUserId,
            List<String> customerIds, List<String> customerNames,
            List<Double> rates, List<Double> defaultQtys,
            DummyDataCallback callback) {

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1; // 1-indexed

        // Generate data for last 4 months (including current)
        int[] months = new int[4];
        int[] years = new int[4];
        for (int i = 0; i < 4; i++) {
            int monthOffset = 3 - i; // 3,2,1,0 months ago
            Calendar tempCal = Calendar.getInstance();
            tempCal.add(Calendar.MONTH, -monthOffset);
            months[i] = tempCal.get(Calendar.MONTH) + 1;
            years[i] = tempCal.get(Calendar.YEAR);
        }

        // We need multiple batches (Firestore limit: 500 ops per batch)
        List<WriteBatch> entryBatches = new ArrayList<>();
        WriteBatch currentBatch = db.batch();
        int opsInBatch = 0;
        int totalEntries = 0;
        int totalPayments = 0;

        for (int m = 0; m < 4; m++) {
            int month = months[m];
            int year = years[m];
            int daysInMonth = getDaysInMonth(year, month);

            // For current month, only generate entries up to today
            int maxDay = daysInMonth;
            if (month == currentMonth && year == currentYear) {
                maxDay = cal.get(Calendar.DAY_OF_MONTH);
            }

            for (int c = 0; c < customerIds.size(); c++) {
                String customerId = customerIds.get(c);
                double baseQty = defaultQtys.get(c);
                double rate = rates.get(c);
                double monthlyTotal = 0;

                for (int day = 1; day <= maxDay; day++) {
                    // Skip ~10% of days randomly (customer was away)
                    if (random.nextInt(10) == 0) continue;

                    String date = String.format(Locale.getDefault(),
                            "%d-%02d-%02d", year, month, day);

                    // Vary quantities slightly (±30%)
                    double morningQty = Math.round((baseQty * 0.5 * (0.7 + random.nextDouble() * 0.6)) * 10.0) / 10.0;
                    double eveningQty = Math.round((baseQty * 0.5 * (0.7 + random.nextDouble() * 0.6)) * 10.0) / 10.0;

                    // Ensure minimum 0.5L
                    morningQty = Math.max(0.5, morningQty);
                    eveningQty = Math.max(0.5, eveningQty);

                    DailyEntry entry = new DailyEntry(customerId, vendorUserId,
                            vendorUserId, date, morningQty, eveningQty, rate);

                    String entryDocId = db.collection("DailyEntries").document().getId();
                    currentBatch.set(db.collection("DailyEntries").document(entryDocId), entry);
                    opsInBatch++;
                    totalEntries++;

                    monthlyTotal += (morningQty + eveningQty) * rate;

                    // Check batch limit
                    if (opsInBatch >= 450) {
                        entryBatches.add(currentBatch);
                        currentBatch = db.batch();
                        opsInBatch = 0;
                    }
                }

                // Create payment record for this customer-month
                Payment payment = new Payment(customerId, customerNames.get(c),
                        vendorUserId, month, year, Math.round(monthlyTotal));
                payment.setVendorId(vendorUserId);

                // Make some paid, some unpaid, some partial
                if (m < 2) {
                    // Older months: mostly paid
                    if (random.nextInt(4) == 0) {
                        // 25% chance unpaid
                        payment.setStatus(Payment.STATUS_PENDING);
                        payment.setPaidAmount(0);
                    } else {
                        payment.setStatus(Payment.STATUS_PAID);
                        payment.setPaidAmount(Math.round(monthlyTotal));
                        payment.setPaidDate(Timestamp.now());
                    }
                } else if (m == 2) {
                    // Last month: mix
                    int rand = random.nextInt(3);
                    if (rand == 0) {
                        payment.setStatus(Payment.STATUS_PAID);
                        payment.setPaidAmount(Math.round(monthlyTotal));
                        payment.setPaidDate(Timestamp.now());
                    } else if (rand == 1) {
                        double partial = Math.round(monthlyTotal * 0.5);
                        payment.setStatus(Payment.STATUS_PARTIAL);
                        payment.setPaidAmount(partial);
                    } else {
                        payment.setStatus(Payment.STATUS_PENDING);
                        payment.setPaidAmount(0);
                    }
                } else {
                    // Current month: mostly unpaid
                    if (random.nextInt(3) == 0) {
                        payment.setStatus(Payment.STATUS_PAID);
                        payment.setPaidAmount(Math.round(monthlyTotal));
                        payment.setPaidDate(Timestamp.now());
                    } else {
                        payment.setStatus(Payment.STATUS_PENDING);
                        payment.setPaidAmount(0);
                    }
                }

                String paymentDocId = db.collection("Payments").document().getId();
                currentBatch.set(db.collection("Payments").document(paymentDocId), payment);
                opsInBatch++;
                totalPayments++;

                if (opsInBatch >= 450) {
                    entryBatches.add(currentBatch);
                    currentBatch = db.batch();
                    opsInBatch = 0;
                }
            }
        }

        // Add the last batch if it has operations
        if (opsInBatch > 0) {
            entryBatches.add(currentBatch);
        }

        // Commit all batches sequentially
        int finalTotalEntries = totalEntries;
        int finalTotalPayments = totalPayments;
        commitBatchesSequentially(entryBatches, 0,
                () -> {
                    Log.d(TAG, "All dummy data inserted! Entries: " + finalTotalEntries
                            + " Payments: " + finalTotalPayments);
                    callback.onSuccess(customerIds.size(), finalTotalEntries, finalTotalPayments);
                },
                error -> callback.onError("Failed to insert entries: " + error));
    }

    private static void commitBatchesSequentially(List<WriteBatch> batches, int index,
            Runnable onComplete, java.util.function.Consumer<String> onError) {
        if (index >= batches.size()) {
            onComplete.run();
            return;
        }

        batches.get(index).commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Batch " + (index + 1) + "/" + batches.size() + " committed");
                    commitBatchesSequentially(batches, index + 1, onComplete, onError);
                })
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    private static int getDaysInMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
