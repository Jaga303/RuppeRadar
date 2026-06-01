package com.example.roomlibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
    Context context;
    DatabaseHelper databaseHelper;
    PreferenceManager preferenceManager;
    ArrayList<ExpenseModel> expenseList = new ArrayList<ExpenseModel>();
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ExpenseModel model);
        void onItemDeleted();
    }

    ExpenseAdapter(Context context, ArrayList<ExpenseModel> expenseList, OnItemClickListener listener){
        this.context = context;
        this.expenseList = expenseList;
        this.listener = listener;
        this.preferenceManager = new PreferenceManager(context);
    }
    @NonNull
    @Override
    public ExpenseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.expense_templet,parent,false);
        databaseHelper = DatabaseHelper.getDB(context);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseAdapter.ViewHolder holder, int position) {
        ExpenseModel model = expenseList.get(position);
        String symbol = preferenceManager.getCurrencySymbol();

        holder.imgIcon.setImageResource(model.image);
        holder.txtAmount.setText(symbol + " " + model.amount);
        holder.txtCategory.setText(model.category);

        holder.llRow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            }
        });

        holder.llRow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure want to delete?")
                        .setIcon(R.drawable.delete)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int pos = holder.getAdapterPosition();

                                if (pos != RecyclerView.NO_POSITION && pos < expenseList.size()) {
                                    ExpenseModel m = expenseList.get(pos);
                                    new Thread(() -> {
                                        databaseHelper.expenseDao().deleteById(m.getId());
                                        if (context instanceof MainActivity) {
                                            ((MainActivity) context).runOnUiThread(() -> {
                                                expenseList.remove(pos);
                                                notifyItemRemoved(pos);
                                                if (listener != null) listener.onItemDeleted();
                                            });
                                        }
                                    }).start();
                                }
                            }
                        })
                        .setNegativeButton("No", null);
                dialog.show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtAmount, txtCategory;
        LinearLayout llRow;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            txtAmount = itemView.findViewById(R.id.txtExpense);
            txtCategory = itemView.findViewById(R.id.txtCat);
            llRow = itemView.findViewById(R.id.llRow);
        }
    }
}
