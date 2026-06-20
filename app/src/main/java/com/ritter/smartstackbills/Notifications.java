package com.ritter.smartstackbills;

import com.google.firebase.Timestamp;

public class Notifications {

    private String notificationId;
    private String billId;
    private String type;
    private String message;
    private String paymentName;
    private String title;
    private Double amount;
    private Timestamp date;
    private Timestamp createdAt;
    private boolean isUnread;

    // Default constructor required for Firestore
    public Notifications() {
        this.createdAt = Timestamp.now();  // Initialize to the current time by default
    }

    public Notifications(String notificationId, String title, Timestamp date, double amount, boolean isUnread) {
        this.notificationId = notificationId;
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.createdAt = Timestamp.now();  // Set createdAt to the current time
        this.isUnread = isUnread;
    }

    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPaymentName() { return paymentName; }
    public void setPaymentName(String paymentName) { this.paymentName = paymentName; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }
}
