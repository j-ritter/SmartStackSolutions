package com.example.smartstackbills;

import com.google.firebase.Timestamp;

public class Notifications {

    String notificationId;
    String title;
    String amount;
    Timestamp date;


    public Notifications() {
        // Default constructor required for Firestore
    }

    public Notifications(String notificationId, String title, Timestamp date, String amount) {
        this.notificationId = notificationId;
        this.title = title;
        this.date = date;
        this.amount = amount;

    }

    // Getters and Setters

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }


}
