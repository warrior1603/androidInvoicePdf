package com.chouchene.factures.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "invoices")
public class Invoice {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "date")
    public Date date;

    public Invoice(double amount, Date date) {
        this.amount = amount;
        this.date = date;
    }
}