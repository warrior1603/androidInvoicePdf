package com.chouchene.factures.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String description;
    public double amount;
    public Date date;
    public String category; // e.g., "Fuel", "Insurance", "Repair"

    public Expense(String description, double amount, Date date, String category) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
    }
}
