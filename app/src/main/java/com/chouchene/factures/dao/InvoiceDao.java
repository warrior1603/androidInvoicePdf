package com.chouchene.factures.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.chouchene.factures.POJO.DailyIncome;
import com.chouchene.factures.POJO.MonthlyIncome;
import com.chouchene.factures.entity.Invoice;

import java.util.Date;
import java.util.List;

@Dao
public interface InvoiceDao {

    @Insert
    void insertInvoice(Invoice invoice);

    @androidx.room.Delete
    void deleteInvoice(Invoice invoice);

    @Query("SELECT SUM(amount) FROM invoices WHERE date(date / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch')")
    float getDailyIncome(Date date);

    @Query("SELECT SUM(amount) FROM invoices WHERE strftime('%m-%Y', date / 1000, 'unixepoch') = strftime('%m-%Y', :date / 1000, 'unixepoch')")
    float getMonthlyIncome(Date date);

    @Query("SELECT SUM(amount) FROM invoices WHERE strftime('%Y', date / 1000, 'unixepoch') = strftime('%Y', :date / 1000, 'unixepoch')")
    float getYearlyIncome(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE date(date / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch')")
    int getDailyCount(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE strftime('%m-%Y', date / 1000, 'unixepoch') = strftime('%m-%Y', :date / 1000, 'unixepoch')")
    int getMonthlyCount(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE strftime('%Y', date / 1000, 'unixepoch') = strftime('%Y', :date / 1000, 'unixepoch')")
    int getYearlyCount(Date date);

    @Query("SELECT date, SUM(amount) as dailyTotal FROM invoices GROUP BY date(date / 1000, 'unixepoch') ORDER BY date ASC")
    List<DailyIncome> getDailyIncomeTotals();

    @Query("SELECT strftime('%m-%Y', date / 1000, 'unixepoch') as month, SUM(amount) as monthlyTotal FROM invoices GROUP BY month ORDER BY date ASC")
    List<MonthlyIncome> getMonthlyIncomeTotals();

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    List<Invoice> getAllInvoices();

    @Query("SELECT * FROM invoices WHERE type = 'Facture' ORDER BY date DESC")
    List<Invoice> getInvoicesOnly();

    @Query("SELECT * FROM invoices WHERE type = 'Bon' ORDER BY date DESC")
    List<Invoice> getBonsOnly();

    @Query("SELECT * FROM invoices WHERE client_name LIKE '%' || :query || '%' ORDER BY date DESC")
    List<Invoice> searchInvoices(String query);
}
