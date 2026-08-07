package com.chouchene.factures.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.chouchene.factures.entity.Expense;
import java.util.Date;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insertExpense(Expense expense);

    @androidx.room.Update
    void updateExpense(Expense expense);

    @Query("SELECT * FROM expenses WHERE id = :id")
    Expense getExpenseById(int id);

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpenses();

    @Query("SELECT SUM(amount) FROM expenses")
    float getTotalExpenses();

    @Query("SELECT SUM(amount) FROM expenses WHERE date(date / 1000, 'unixepoch', 'localtime') = date(:date / 1000, 'unixepoch', 'localtime')")
    float getDailyExpenses(Date date);

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%m-%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%m-%Y', :date / 1000, 'unixepoch', 'localtime')")
    float getMonthlyExpenses(Date date);

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = strftime('%Y', :date / 1000, 'unixepoch', 'localtime')")
    float getYearlyExpenses(Date date);

    @androidx.room.Delete
    void deleteExpense(Expense expense);
}
