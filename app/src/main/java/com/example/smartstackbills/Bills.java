package com.example.smartstackbills;

public class Bills {

    String name, date, comment, category, amount, vendor, repeat, subcategory, attachment;
    boolean paid;

    public Bills() {
        // Constructor por defecto necesario para Firestore
    }

    public Bills(String name, String date, String comment, String category, String amount, String vendor, boolean paid, String repeat, String subcategory, String attachment) {
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

    public String getAttachment(){return attachment;}

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat){
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
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
