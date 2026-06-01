package com.example.roomlibrary;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BudgetDao {
    @Query("SELECT * FROM budgets")
    List<Budget> getAllBudgets();

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    Budget getBudgetByCategory(String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBudget(Budget budget);

    @Query("DELETE FROM budgets")
    void deleteAllBudgets();
}
