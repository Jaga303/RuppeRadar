package com.example.roomlibrary;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "limit_amount")
    private double limitAmount;

    public Budget(String category, double limitAmount) {
        this.category = category;
        this.limitAmount = limitAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
}
