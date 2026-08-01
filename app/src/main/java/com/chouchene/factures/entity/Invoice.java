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

    @ColumnInfo(name = "client_name")
    public String clientName;

    @ColumnInfo(name = "file_path")
    public String filePath;

    @ColumnInfo(name = "type")
    public String type; // "Facture" or "Bon"

    public Invoice(double amount, Date date, String clientName, String filePath, String type) {
        this.amount = amount;
        this.date = date;
        this.clientName = clientName;
        this.filePath = filePath;
        this.type = type;
    }
}
