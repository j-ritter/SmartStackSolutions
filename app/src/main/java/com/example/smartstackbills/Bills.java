package com.example.smartstackbills;

import com.google.firebase.Timestamp;

public class Bills {

    String billId;
    String name, comment, category, amount, vendor, repeat, subcategory, attachment;
    Timestamp date;
    boolean paid;

    public Bills() {
        // Default constructor required for Firestore
    }

    public Bills(String billId, String name, Timestamp date, String comment, String category, String amount, String vendor, boolean paid, String repeat, String subcategory, String attachment) {
        this.billId = billId;
        this.name = name;
        this.date = date;
        this.comment = comment;
        this.category = category;
        this.amount = amount;
        this.vendor = vendor;
        this.paid = paid;
        this.repeat = repeat;
        this.subcategory = subcategory;
        this.attachment = attachment;

    }

    // Getters and Setters

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
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


}