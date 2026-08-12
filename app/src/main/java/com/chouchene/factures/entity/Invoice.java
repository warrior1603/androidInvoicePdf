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

    @ColumnInfo(name = "status", defaultValue = "En attente")
    public String status; // "En attente", "Payée", "Annulée"

    // RESTORED FIELDS FOR FACTURE
    public String email;
    public String tel;
    public String street;
    public String codePostale;
    public String city;
    public String country;
    public String siren;
    public String tva_client;
    public String invoice_date;
    public String description;
    public String payment_mode;
    public double qty;
    public double price_ttc;
    public double tva_rate;

    // RESTORED FIELDS FOR BON
    public String passenger_name;
    public String passenger_tel;
    public String order_date;
    public String order_time;
    public String pickup_date;
    public String pickup_time;
    public String pickup_location;
    public String destination;
    public String via;
    public double fare;

    public String signature_base64;

    public Invoice() {}

    public Invoice(double amount, Date date, String clientName, String filePath, String type) {
        this.amount = amount;
        this.date = date;
        this.clientName = clientName;
        this.filePath = filePath;
        this.type = type;
        this.status = "En attente";
    }
}
