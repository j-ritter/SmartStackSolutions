package com.example.smartstackbills;

import com.google.firebase.Timestamp;

public class Spendings {

    String spendingId;
    String name, comment, category, amount, vendor, subcategory, attachment;
    Timestamp date;
    boolean paid;
    boolean isEssential;
    boolean isRecurring;

    // New fields to avoid Firestore mapping issues
    private String repeat = "No";  // Default to "No"
    private String billId;  // Optional, set to null if not used
    private String recurrenceInterval;  // Optional, set to null if not used

    public Spendings() {
        // Default constructor needed for Firestore
    }

    public Spendings(String spendingId, String name, Timestamp date, String comment, String category, String amount, String vendor, String subcategory, String attachment, boolean isEssential, boolean isRecurring, boolean paid) {
        this.spendingId = spendingId;
        this.name = name;
        this.date = date;
        this.comment = comment;
        this.category = category;
        this.amount = amount;
        this.vendor = vendor;
        this.subcategory = subcategory;
        this.attachment = attachment;
        this.paid = paid; // Added to constructor
        this.isEssential = isEssential;
        this.isRecurring = isRecurring;
    }

    public String getSpendingId() {
        return spendingId;
    }

    public void setSpendingId(String spendingId) {
        this.spendingId = spendingId;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isEssential() {
        return isEssential;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    // Getters and setters for new optional fields
    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(String recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }
}
