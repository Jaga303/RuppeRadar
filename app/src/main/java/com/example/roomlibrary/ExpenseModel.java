package com.example.roomlibrary;

public class ExpenseModel {
    int id,image;
    String amount, category;
    ExpenseModel(int id,int image, String amount,String category){
        this.id = id;
        this.image = image;
        this.amount = amount;
        this.category = category;
    }

    public int getId() {
        return id;
    }
}
