package com.chouchene.factures.model;

import com.chouchene.factures.entity.Booking;
import com.chouchene.factures.entity.Invoice;

import java.util.Date;

public class RecentActivity {
    public enum Type { INVOICE, ORDER, BOOKING }
    
    public Type type;
    public int id;
    public String title;
    public String subtitle;
    public double amount;
    public Date date;
    public String status;
    public Object originalObject;

    public RecentActivity(Invoice invoice) {
        this.type = "Facture".equals(invoice.type) ? Type.INVOICE : Type.ORDER;
        this.id = invoice.id;
        this.title = invoice.clientName;
        this.subtitle = invoice.type;
        this.amount = invoice.amount;
        this.date = invoice.date;
        this.status = invoice.status;
        this.originalObject = invoice;
    }

    public RecentActivity(Booking booking) {
        this.type = Type.BOOKING;
        this.id = booking.id;
        this.title = booking.clientName;
        this.subtitle = "Course: " + booking.pickupLocation + " → " + booking.destinationLocation;
        this.amount = booking.estimatedPrice;
        this.date = booking.dateTime;
        this.status = booking.status;
        this.originalObject = booking;
    }
}
