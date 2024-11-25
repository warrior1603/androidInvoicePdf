package com.chouchene.factures.POJO;

import java.util.Date;

public class DailyIncome {
    public Date date;
    public double dailyTotal;

    public DailyIncome(Date date, double dailyTotal) {
        this.date = date;
        this.dailyTotal = dailyTotal;
    }
}
