package com.example.smartstackbills;

import com.google.firebase.Timestamp;

public class Income {

    String incomeId;
    String name, comment, category, amount, repeat, subcategory;
    Timestamp date;


    public Income() {
        // Default constructor needed for Firestore
    }

    public Income(String incomeId, String name, Timestamp date, String comment, String category, String amount, String repeat, String subcategory) {
        this.incomeId = incomeId;
        this.name = name;
        this.date = date;
        this.comment = comment;
        this.category = category;
        this.amount = amount;
        this.repeat = repeat;
        this.subcategory = subcategory;
    }

    public String getIncomeId() {
        return incomeId;
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
}
