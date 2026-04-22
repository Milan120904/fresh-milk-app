package com.example.freshmilk.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Customer {
    private String customerId;
    private String userId;
    private String vendorId;
    private String name;
    private String mobile;
    private String email;
    private String address;
    private String milkType; // Cow, Buffalo, Mixed
    private double defaultQuantity;
    private double ratePerLiter;
    private double latitude;
    private double longitude;
    private boolean active;

    @ServerTimestamp
    private Date createdAt;

    public Customer() {
        // Required empty constructor for Firestore
    }

    public Customer(String userId, String name, String mobile, String address,
            String milkType, double defaultQuantity, double ratePerLiter) {
        this.userId = userId;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.milkType = milkType;
        this.defaultQuantity = defaultQuantity;
        this.ratePerLiter = ratePerLiter;
        this.active = true;
        // createdAt will be set automatically by Firestore @ServerTimestamp
    }

    public Customer(String userId, String vendorId, String name, String mobile, String address,
            String milkType, double defaultQuantity, double ratePerLiter) {
        this.userId = userId;
        this.vendorId = vendorId;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.milkType = milkType;
        this.defaultQuantity = defaultQuantity;
        this.ratePerLiter = ratePerLiter;
        this.active = true;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMilkType() {
        return milkType;
    }

    public void setMilkType(String milkType) {
        this.milkType = milkType;
    }

    public double getDefaultQuantity() {
        return defaultQuantity;
    }

    public void setDefaultQuantity(double defaultQuantity) {
        this.defaultQuantity = defaultQuantity;
    }

    public double getRatePerLiter() {
        return ratePerLiter;
    }

    public void setRatePerLiter(double ratePerLiter) {
        this.ratePerLiter = ratePerLiter;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
