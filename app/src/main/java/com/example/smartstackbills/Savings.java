package com.example.smartstackbills;

import java.security.Timestamp;

public class Savings {
    String savingsId;
    String targetName;
    double targetAmount;
    Timestamp startDate;
    Timestamp endDate;
    double monthlySavings;

    // Default constructor needed for Firestore
    public Savings() {
    }

    // Parameterized constructor
    public Savings(String savingsId, String targetName, double targetAmount, Timestamp startDate, Timestamp endDate, double monthlySavings) {
        this.savingsId = savingsId;
        this.targetName = targetName;
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlySavings = monthlySavings;
    }

    // Getters and Setters
    public String getSavingsId() {
        return savingsId;
    }

    public void setSavingsId(String savingsId) {
        this.savingsId = savingsId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public double getMonthlySavings() {
        return monthlySavings;
    }

    public void setMonthlySavings(double monthlySavings) {
        this.monthlySavings = monthlySavings;
    }
}

