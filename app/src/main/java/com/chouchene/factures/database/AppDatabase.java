package com.chouchene.factures.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;

@Database(entities = {Client.class, Invoice.class} ,version = 3,exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ClientDao clientDao();
    public abstract InvoiceDao invoiceDao();
}
