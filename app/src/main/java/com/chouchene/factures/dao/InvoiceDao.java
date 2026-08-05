package com.chouchene.factures.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.chouchene.factures.POJO.DailyIncome;
import com.chouchene.factures.POJO.MonthlyIncome;
import com.chouchene.factures.entity.Invoice;

import java.util.Date;
import java.util.List;

@Dao
public interface InvoiceDao {

    @Insert
    void insertInvoice(Invoice invoice);

    @Update
    void updateInvoice(Invoice invoice);

    @androidx.room.Delete
    void deleteInvoice(Invoice invoice);

    @Query("SELECT SUM(amount) FROM invoices WHERE date(date / 1000, 'unixepoch', 'localtime') = date(:date / 1000, 'unixepoch', 'localtime')")
    float getDailyIncome(Date date);

    @Query("SELECT SUM(amount) FROM invoices WHERE strftime('%m-%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%m-%Y', :date / 1000, 'unixepoch', 'localtime')")
    float getMonthlyIncome(Date date);

    @Query("SELECT SUM(amount) FROM invoices WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%Y', :date / 1000, 'unixepoch', 'localtime')")
    float getYearlyIncome(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE date(date / 1000, 'unixepoch', 'localtime') = date(:date / 1000, 'unixepoch', 'localtime')")
    int getDailyCount(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE strftime('%m-%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%m-%Y', :date / 1000, 'unixepoch', 'localtime')")
    int getMonthlyCount(Date date);

    @Query("SELECT COUNT(*) FROM invoices WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%Y', :date / 1000, 'unixepoch', 'localtime')")
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

    @Query("SELECT * FROM invoices WHERE type = :type AND date(date / 1000, 'unixepoch', 'localtime') = date(:date / 1000, 'unixepoch', 'localtime') ORDER BY date DESC")
    List<Invoice> getDocumentsByDay(String type, Date date);

    @Query("SELECT * FROM invoices WHERE type = :type AND strftime('%m-%Y', date / 1000, 'unixepoch', 'localtime') = :monthYear ORDER BY date DESC")
    List<Invoice> getDocumentsByMonth(String type, String monthYear);

    @Query("SELECT * FROM invoices WHERE type = :type AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year ORDER BY date DESC")
    List<Invoice> getDocumentsByYear(String type, String year);

    @Query("SELECT * FROM invoices ORDER BY date DESC LIMIT 3")
    List<Invoice> getLatestInvoices();

    @Query("SELECT SUM(amount) FROM invoices")
    float getTotalRevenue();

    @Query("SELECT COUNT(*) FROM invoices")
    int getTotalCount();

    @Query("SELECT * FROM invoices WHERE client_name = :clientName ORDER BY date DESC")
    List<Invoice> getInvoicesByClient(String clientName);

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'En attente' AND (date / 1000) < (strftime('%s', 'now') - 30*24*60*60)")
    int getOverdueInvoicesCount();
}
