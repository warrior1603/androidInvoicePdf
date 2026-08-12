package com.chouchene.factures.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.chouchene.factures.dao.BookingDao;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.dao.ExpenseDao;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.entity.Booking;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Expense;
import com.chouchene.factures.entity.Invoice;

@Database(entities = {Client.class, Invoice.class, Expense.class, Booking.class} ,version = 11,exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ClientDao clientDao();
    public abstract InvoiceDao invoiceDao();
    public abstract ExpenseDao expenseDao();
    public abstract BookingDao bookingDao();
}
