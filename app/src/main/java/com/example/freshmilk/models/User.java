package com.example.freshmilk.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {
    public static final String ROLE_VENDOR = "vendor";
    public static final String ROLE_CUSTOMER = "customer";

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String upiId;
    
    // For Vendor Reminder Settings
    private int reminderStartDate;
    private int reminderEndDate;

    @ServerTimestamp
    private Date createdAt;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String name, String email, String phone, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        // createdAt will be set automatically by Firestore @ServerTimestamp
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public boolean isVendor() {
        return ROLE_VENDOR.equals(role);
    }

    @Exclude
    public boolean isCustomer() {
        return ROLE_CUSTOMER.equals(role);
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public int getReminderStartDate() {
        return reminderStartDate;
    }

    public void setReminderStartDate(int reminderStartDate) {
        this.reminderStartDate = reminderStartDate;
    }

    public int getReminderEndDate() {
        return reminderEndDate;
    }

    public void setReminderEndDate(int reminderEndDate) {
        this.reminderEndDate = reminderEndDate;
    }
}
