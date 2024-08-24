package com.example.smartstackbills;

import com.google.firebase.Timestamp;


public class Spendings {

    String spendingId;
    String name, comment, category, amount, vendor, subcategory, attachment;
    Timestamp date;
    boolean paid;
    boolean isEssential;

    public Spendings() {
        // Default constructor needed for Firestore
    }

    public Spendings(String spendingId, String name, Timestamp date, String comment, String category, String amount, String vendor, String subcategory, String attachment, boolean isEssential) {
        this.spendingId = spendingId;
        this.name = name;
        this.date = date;
        this.comment = comment;
        this.category = category;
        this.amount = amount;
        this.vendor = vendor;
        this.subcategory = subcategory;
        this.attachment = attachment;
        this.paid = paid;
        this.isEssential = isEssential;
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
}

