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

    @Query("SELECT SUM(amount) FROM invoices WHERE date(date) = date(:date)")
    float getDailyIncome(Date date);

    @Query("SELECT SUM(amount) FROM invoices WHERE strftime('%m-%Y', date) = strftime('%m-%Y', :date)")
    float getMonthlyIncome(Date date);

    @Query("SELECT date, SUM(amount) as dailyTotal FROM invoices GROUP BY date ORDER BY date ASC")
    List<DailyIncome> getDailyIncomeTotals();

    @Query("SELECT strftime('%m-%Y', date) as month, SUM(amount) as monthlyTotal FROM invoices GROUP BY month ORDER BY date ASC")
    List<MonthlyIncome> getMonthlyIncomeTotals();
}
