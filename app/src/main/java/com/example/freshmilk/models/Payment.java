package com.example.freshmilk.models;

import com.google.firebase.Timestamp;

public class Payment {
    public static final String STATUS_PAID = "Paid";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String METHOD_UPI = "UPI";
    public static final String METHOD_QR = "QR";
    public static final String METHOD_CARD = "CARD";

    private String paymentId;
    private String customerId;
    private String customerName;
    private String userId;
    private String vendorId;
    private int month;
    private int year;
    private double totalAmount;
    private double paidAmount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private Timestamp paidDate;
    private String screenshotUrl;

    public Payment() {
        // Required empty constructor for Firestore
    }

    public Payment(String customerId, String customerName, String userId,
            int month, int year, double totalAmount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.userId = userId;
        this.month = month;
        this.year = year;
        this.totalAmount = totalAmount;
        this.paidAmount = 0;
        this.status = STATUS_PENDING;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(Timestamp paidDate) {
        this.paidDate = paidDate;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }

    public String getMonthYearString() {
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        return months[month - 1] + " " + year;
    }
}
