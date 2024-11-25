package com.chouchene.factures.POJO;

public class MonthlyIncome {
    public String month;    // Using a String to represent "MM-YYYY" format
    public double monthlyTotal;

    public MonthlyIncome(String month, double monthlyTotal) {
        this.month = month;
        this.monthlyTotal = monthlyTotal;
    }
}
