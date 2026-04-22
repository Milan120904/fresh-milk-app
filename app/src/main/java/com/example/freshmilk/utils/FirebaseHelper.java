package com.example.freshmilk.utils;

import com.example.freshmilk.models.Customer;
import com.example.freshmilk.models.DailyEntry;
import com.example.freshmilk.models.ExtraCharge;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.Map;

public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static boolean firestoreConfigured = false;

    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_CUSTOMERS = "Customers";
    private static final String COLLECTION_DAILY_ENTRIES = "DailyEntries";
    private static final String COLLECTION_PAYMENTS = "Payments";
    private static final String COLLECTION_EXTRA_CHARGES = "extra_charges";

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Call this ONCE from Application.onCreate() or SplashActivity
     * BEFORE any Firestore operations.
     */
    public static void configureFirestore() {
        if (!firestoreConfigured) {
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                                .setSizeBytes(100 * 1024 * 1024) // 100 MB
                                .build())
                        .build();
                firestore.setFirestoreSettings(settings);
            } catch (Exception e) {
                // Settings already applied or Firestore already in use — safe to ignore
            }
            firestoreConfigured = true;
        }
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    // ==================== AUTH ====================

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public void logout() {
        auth.signOut();
    }

    // ==================== USERS ====================

    public Task<Void> saveUser(User user) {
        return db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user);
    }

    public Task<Void> saveUserData(String uid, Map<String, Object> userData) {
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .set(userData);
    }

    public DocumentReference getUserRef(String uid) {
        return db.collection(COLLECTION_USERS).document(uid);
    }

    public Task<DocumentSnapshot> getUserByUid(String uid) {
        return db.collection(COLLECTION_USERS).document(uid).get();
    }

    // ==================== CUSTOMERS ====================

    public CollectionReference getCustomersCollection() {
        return db.collection(COLLECTION_CUSTOMERS);
    }

    public Task<DocumentSnapshot> getCustomerByUserId(String userId) {
        return db.collection(COLLECTION_CUSTOMERS).document(userId).get();
    }

    public Task<Void> addCustomer(Customer customer) {
        DocumentReference docRef = db.collection(COLLECTION_CUSTOMERS).document();
        customer.setCustomerId(docRef.getId());
        return docRef.set(customer);
    }

    public Task<Void> updateCustomer(String customerId, Map<String, Object> customerData) {
        return db.collection(COLLECTION_CUSTOMERS)
                .document(customerId)
                .set(customerData, SetOptions.merge());
    }

    public Task<Void> deleteCustomer(String customerId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .document(customerId)
                .delete();
    }

    public Query getCustomersByUser(String userId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("userId", userId)
                .orderBy("name");
    }

    public Query getCustomersByVendor(String vendorId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("vendorId", vendorId)
                .orderBy("name");
    }

    /** Simple vendor query without orderBy — avoids composite index requirement */
    public Query getCustomersByVendorSimple(String vendorId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("vendorId", vendorId);
    }

    public Query searchCustomers(String userId, String searchQuery) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("userId", userId)
                .orderBy("name")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff");
    }

    public Task<QuerySnapshot> getActiveCustomerCount(String vendorId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("active", true)
                .get();
    }

    /** Get active customer count from CACHE first (no network) */
    public Task<QuerySnapshot> getActiveCustomerCountCached(String vendorId) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("active", true)
                .get(Source.CACHE);
    }

    public Query getCustomerByEmail(String email) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("email", email)
                .limit(1);
    }

    public Task<QuerySnapshot> findVendorByPhone(String phone) {
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("role", "vendor")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get();
    }

    public Task<QuerySnapshot> checkDuplicateCustomerByPhone(String vendorId, String phone) {
        return db.collection(COLLECTION_CUSTOMERS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("mobile", phone)
                .limit(1)
                .get();
    }

    // ==================== DAILY ENTRIES ====================

    public CollectionReference getDailyEntriesCollection() {
        return db.collection(COLLECTION_DAILY_ENTRIES);
    }

    public Task<DocumentReference> addDailyEntry(DailyEntry entry) {
        return db.collection(COLLECTION_DAILY_ENTRIES).add(entry);
    }

    public Task<Void> updateDailyEntry(String entryId, DailyEntry entry) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .document(entryId)
                .set(entry);
    }

    public Task<Void> deleteDailyEntry(String entryId) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .document(entryId)
                .delete();
    }

    public Query getEntriesByCustomer(String customerId) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("customerId", customerId)
                .orderBy("date", Query.Direction.DESCENDING);
    }

    public Query getEntriesByCustomerSimple(String customerId) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("customerId", customerId);
    }

    public Query getEntriesByCustomerAndMonth(String customerId, String startDate, String endDate) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("customerId", customerId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate);
    }

    public Query getTodaysEntries(String userId, String today) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today);
    }

    // ==================== EXTRA CHARGES ====================

    public CollectionReference getExtraChargesCollection() {
        return db.collection(COLLECTION_EXTRA_CHARGES);
    }

    public Task<DocumentReference> addExtraCharge(ExtraCharge charge) {
        return db.collection(COLLECTION_EXTRA_CHARGES).add(charge);
    }

    public Task<Void> updateExtraCharge(String chargeId, ExtraCharge charge) {
        return db.collection(COLLECTION_EXTRA_CHARGES)
                .document(chargeId)
                .set(charge);
    }

    public Task<Void> deleteExtraCharge(String chargeId) {
        return db.collection(COLLECTION_EXTRA_CHARGES)
                .document(chargeId)
                .delete();
    }

    public Query getExtraChargesByCustomerAndMonth(String customerId, String monthPrefix) {
        return db.collection(COLLECTION_EXTRA_CHARGES)
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("month", monthPrefix);
    }

    public Query getExtraChargesByCustomer(String customerId) {
        return db.collection(COLLECTION_EXTRA_CHARGES)
                .whereEqualTo("customerId", customerId);
    }

    // ==================== PAYMENTS ====================

    public CollectionReference getPaymentsCollection() {
        return db.collection(COLLECTION_PAYMENTS);
    }

    public Task<DocumentReference> addPayment(Payment payment) {
        return db.collection(COLLECTION_PAYMENTS).add(payment);
    }

    public Task<Void> updatePayment(String paymentId, Payment payment) {
        return db.collection(COLLECTION_PAYMENTS)
                .document(paymentId)
                .set(payment);
    }

    public Query getPaymentsByCustomer(String customerId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("customerId", customerId)
                .orderBy("year", Query.Direction.DESCENDING)
                .orderBy("month", Query.Direction.DESCENDING);
    }

    public Query getPaymentsByUser(String userId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("userId", userId);
    }

    public Query getPaymentsByUserAndMonth(String userId, int month, int year) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year);
    }

    public Query getUnpaidPayments(String userId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", Payment.STATUS_PENDING);
    }

    public Query getMonthlyEarnings(String userId, int year) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("year", year)
                .whereEqualTo("status", Payment.STATUS_PAID);
    }

    public Task<DocumentSnapshot> getVendorById(String vendorId) {
        return db.collection(COLLECTION_USERS).document(vendorId).get();
    }

    public Query getEntriesByVendorAndMonth(String vendorId, String startDate, String endDate) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("vendorId", vendorId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate);
    }

    public Query getAllEntriesByVendor(String vendorId) {
        return db.collection(COLLECTION_DAILY_ENTRIES)
                .whereEqualTo("vendorId", vendorId);
    }

    // ==================== VENDOR PAYMENT TRACKING ====================

    public Query getPaymentsByVendor(String vendorId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("vendorId", vendorId);
    }

    public Query getPaymentsByVendorAndMonth(String vendorId, int month, int year) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year);
    }

    public Query getPaymentsByVendorAndStatus(String vendorId, String status) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("status", status);
    }

    public Query getPaymentByCustomerAndMonth(String customerId, int month, int year) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .limit(1);
    }

    public Task<Void> updatePaymentFields(String paymentId, Map<String, Object> fields) {
        return db.collection(COLLECTION_PAYMENTS)
                .document(paymentId)
                .update(fields);
    }

    public Task<Void> updateUserFields(String uid, Map<String, Object> fields) {
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .update(fields);
    }

    // ==================== PENDING VERIFICATION QUERIES ====================

    public Query getPendingVerificationPayments(String vendorId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("status", Payment.STATUS_PROCESSING);
    }

    public Query getPendingVerificationByUserId(String userId) {
        return db.collection(COLLECTION_PAYMENTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", Payment.STATUS_PROCESSING);
    }
}
