package com.example.roomlibrary;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "expense")
public class Expense {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "amount")
    private String amount;
    @ColumnInfo(defaultValue = "0")
    private int icon;

    @ColumnInfo(name = "type")
    private String type; // "Income" or "Expense"

    @ColumnInfo(name = "date")
    private long date; // Timestamp

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "payment_method")
    private String paymentMethod; // "Card", "Cash", "Online", "Saving"

    // Constructor used by Room
    public Expense(int id, String title, String amount, int icon, String type, long date, String category, String paymentMethod) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.icon = icon;
        this.type = type;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
    }

    // Constructor used by app
    @Ignore
    public Expense(String title, String amount, int icon, String type, long date, String category, String paymentMethod) {
        this.title = title;
        this.amount = amount;
        this.icon = icon;
        this.type = type;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
