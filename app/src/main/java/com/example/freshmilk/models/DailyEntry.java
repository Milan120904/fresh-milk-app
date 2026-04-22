package com.example.freshmilk.models;

public class DailyEntry {
    private String entryId;
    private String customerId;
    private String userId;
    private String vendorId;
    private String date; // yyyy-MM-dd format
    private double morningQty;
    private double eveningQty;
    private double totalQty;
    private double ratePerLiter;
    private double dailyAmount;

    public DailyEntry() {
        // Required empty constructor for Firestore
    }

    public DailyEntry(String customerId, String userId, String vendorId, String date,
            double morningQty, double eveningQty, double ratePerLiter) {
        this.customerId = customerId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.date = date;
        this.morningQty = morningQty;
        this.eveningQty = eveningQty;
        this.totalQty = morningQty + eveningQty;
        this.ratePerLiter = ratePerLiter;
        this.dailyAmount = this.totalQty * ratePerLiter;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getMorningQty() {
        return morningQty;
    }

    public void setMorningQty(double morningQty) {
        this.morningQty = morningQty;
        recalculate();
    }

    public double getEveningQty() {
        return eveningQty;
    }

    public void setEveningQty(double eveningQty) {
        this.eveningQty = eveningQty;
        recalculate();
    }

    public double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(double totalQty) {
        this.totalQty = totalQty;
    }

    public double getRatePerLiter() {
        return ratePerLiter;
    }

    public void setRatePerLiter(double ratePerLiter) {
        this.ratePerLiter = ratePerLiter;
        recalculate();
    }

    public double getDailyAmount() {
        return dailyAmount;
    }

    public void setDailyAmount(double dailyAmount) {
        this.dailyAmount = dailyAmount;
    }

    private void recalculate() {
        this.totalQty = this.morningQty + this.eveningQty;
        this.dailyAmount = this.totalQty * this.ratePerLiter;
    }
}
