package com.example.freshmilk.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.freshmilk.R;
import com.example.freshmilk.activities.PaymentActivity;
import com.example.freshmilk.models.Payment;
import com.example.freshmilk.models.User;
import com.example.freshmilk.utils.FirebaseHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PaymentReminderWorker extends Worker {

    private static final String CHANNEL_ID = "freshmilk_customer_reminder";

    public PaymentReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseHelper helper = FirebaseHelper.getInstance();
        String uid = helper.getCurrentUserId();
        
        if (uid == null) {
            return Result.success(); // User not logged in, ignore
        }

        try {
            // First identify if it's a customer
            DocumentSnapshot userDoc = Tasks.await(helper.getUserRef(uid).get());
            User user = userDoc.toObject(User.class);
            
            if (user == null || !User.ROLE_CUSTOMER.equals(user.getRole())) {
                return Result.success(); // Only runs for customers
            }
            
            // Get unpaid payments for this customer
            QuerySnapshot paymentsSnapshot = Tasks.await(
                FirebaseFirestore.getInstance().collection("payments")
                    .whereEqualTo("customerId", uid)
                    .whereIn("status", java.util.Arrays.asList(Payment.STATUS_PENDING, Payment.STATUS_PARTIAL))
                    .get()
            );
            
            if (paymentsSnapshot.isEmpty()) {
                return Result.success(); // No unpaid payments
            }
            
            List<DocumentSnapshot> paymentDocs = paymentsSnapshot.getDocuments();
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            
            for (DocumentSnapshot pDoc : paymentDocs) {
                Payment payment = pDoc.toObject(Payment.class);
                if (payment != null && payment.getVendorId() != null) {
                    // Get vendor details to check reminder dates
                    DocumentSnapshot vendorDoc = Tasks.await(
                            FirebaseFirestore.getInstance().collection("users").document(payment.getVendorId()).get()
                    );
                    
                    User vendor = vendorDoc.toObject(User.class);
                    if (vendor != null) {
                        int startDay = vendor.getReminderStartDate() == 0 ? 1 : vendor.getReminderStartDate();
                        int endDay = vendor.getReminderEndDate() == 0 ? 10 : vendor.getReminderEndDate();
                        
                        if (currentDay >= startDay && currentDay <= endDay) {
                            sendPaymentReminderNotification(payment, vendor.getName());
                            // Stop after sending one to avoid spamming the user
                            break; 
                        }
                    }
                }
            }

            return Result.success();
            
        } catch (Exception e) {
            Log.e("PaymentReminderWorker", "Error processing reminders", e);
            return Result.retry();
        }
    }

    private void sendPaymentReminderNotification(Payment payment, String vendorName) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Customer Payment Reminders", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        double dueAmount = payment.getTotalAmount() - payment.getPaidAmount();

        Intent payIntent = new Intent(context, PaymentActivity.class);
        payIntent.putExtra("paymentId", payment.getPaymentId());
        payIntent.putExtra("dueAmount", dueAmount);
        payIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), payIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Payment Pending for " + vendorName)
                .setContentText("Your milk payment of ₹" + dueAmount + " is pending. Please pay your bill.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_notification, "Pay Now", pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
