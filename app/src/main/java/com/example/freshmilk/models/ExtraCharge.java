package com.example.freshmilk.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ExtraCharge {
    private String chargeId;
    private String customerId;
    private String vendorId;
    private double amount;
    private String description;
    private String date; // format: "yyyy-MM-dd"
    private String month; // format: "yyyy-MM"
    
    @ServerTimestamp
    private Date createdAt;

    public ExtraCharge() {
        // Required empty public constructor for Firestore
    }

    public ExtraCharge(String customerId, String vendorId, double amount, 
                       String description, String date, String month) {
        this.customerId = customerId;
        this.vendorId = vendorId;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.month = month;
    }

    // Getters and Setters
    public String getChargeId() { return chargeId; }
    public void setChargeId(String chargeId) { this.chargeId = chargeId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
