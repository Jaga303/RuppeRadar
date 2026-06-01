package com.example.roomlibrary;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Query("select * from expense")
    List<Expense> getAllExpense();

    @Insert
    void addTx(Expense expense);

    @Update
    void updateTx(Expense expense);

    @Query("SELECT * FROM expense WHERE id = :id")
    Expense getExpenseById(int id);

    @Query("DELETE FROM expense WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM expense")
    void deleteAllExpenses();

    @Query("SELECT SUM(amount) FROM expense WHERE type = 'Expense'")
    double getTotalExpense();

    @Query("SELECT SUM(amount) FROM expense WHERE type = 'Income'")
    double getTotalIncome();

    @Query("SELECT * FROM expense WHERE type = :type")
    List<Expense> getTransactionsByType(String type);

    @Query("SELECT * FROM expense WHERE date = :date")
    List<Expense> getTransactionsByDate(long date);

    @Query("SELECT * FROM expense WHERE date >= :start AND date <= :end ORDER BY date DESC")
    List<Expense> getTransactionsByPeriod(long start, long end);

    @Query("SELECT SUM(amount) FROM expense WHERE type = 'Expense' AND date >= :start AND date <= :end")
    double getTotalExpenseByPeriod(long start, long end);

    @Query("SELECT SUM(amount) FROM expense WHERE type = 'Income' AND date >= :start AND date <= :end")
    double getTotalIncomeByPeriod(long start, long end);

    @Query("SELECT SUM(amount) FROM expense WHERE type = 'Expense' AND category = :category AND date >= :start AND date <= :end")
    double getTotalExpenseByCategoryAndPeriod(String category, long start, long end);
}
