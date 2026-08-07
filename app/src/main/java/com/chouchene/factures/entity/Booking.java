package com.chouchene.factures.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "bookings")
public class Booking {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "client_name")
    public String clientName;

    @ColumnInfo(name = "client_phone")
    public String clientPhone;

    @ColumnInfo(name = "pickup_location")
    public String pickupLocation;

    @ColumnInfo(name = "destination_location")
    public String destinationLocation;

    @ColumnInfo(name = "date_time")
    public Date dateTime;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "status")
    public String status; // "Scheduled", "Completed", "Cancelled"

    @ColumnInfo(name = "estimated_price")
    public double estimatedPrice;

    public Booking(String clientName, String clientPhone, String pickupLocation, String destinationLocation, Date dateTime, String note, double estimatedPrice) {
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.pickupLocation = pickupLocation;
        this.destinationLocation = destinationLocation;
        this.dateTime = dateTime;
        this.note = note;
        this.status = "Scheduled";
        this.estimatedPrice = estimatedPrice;
    }
}
