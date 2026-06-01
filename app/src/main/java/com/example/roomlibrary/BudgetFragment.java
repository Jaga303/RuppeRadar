package com.example.roomlibrary;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BudgetFragment extends Fragment {

    private RecyclerView rvBudgets;
    private DatabaseHelper dbHelper;
    private PreferenceManager preferenceManager;
    private BudgetAdapter adapter;
    private List<BudgetViewItem> budgetList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        rvBudgets = view.findViewById(R.id.rvBudgets);
        dbHelper = DatabaseHelper.getDB(getContext());
        preferenceManager = new PreferenceManager(getContext());

        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter();
        rvBudgets.setAdapter(adapter);

        loadBudgets();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBudgets(); // Refresh currency
    }

    private void loadBudgets() {
        new Thread(() -> {
            String[] categories = {"Food", "Shopping", "Transport", "Health", "Salary"};
            budgetList.clear();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long start = cal.getTimeInMillis();
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            long end = cal.getTimeInMillis();

            for (String cat : categories) {
                Budget b = dbHelper.budgetDao().getBudgetByCategory(cat);
                double limit = (b != null) ? b.getLimitAmount() : 0;
                
                double spent = dbHelper.expenseDao().getTotalExpenseByCategoryAndPeriod(cat, start, end);
                budgetList.add(new BudgetViewItem(cat, limit, spent));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }).start();
    }

    private void showSetLimitDialog(BudgetViewItem item) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_set_limit);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView txtTitle = dialog.findViewById(R.id.txtTitle);
        EditText edtLimit = dialog.findViewById(R.id.edtLimit);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        txtTitle.setText("Set Limit for " + item.category);
        edtLimit.setText(String.valueOf(item.limit));

        btnSave.setOnClickListener(v -> {
            String limitStr = edtLimit.getText().toString();
            if (limitStr.isEmpty()) return;
            double newLimit = Double.parseDouble(limitStr);

            new Thread(() -> {
                dbHelper.budgetDao().insertOrUpdateBudget(new Budget(item.category, newLimit));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        loadBudgets();
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private class BudgetViewItem {
        String category;
        double limit;
        double spent;

        BudgetViewItem(String category, double limit, double spent) {
            this.category = category;
            this.limit = limit;
            this.spent = spent;
        }
    }

    private class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BudgetViewItem item = budgetList.get(position);
            String symbol = preferenceManager.getCurrencySymbol();

            holder.txtCategory.setText(item.category);
            holder.txtLimit.setText(String.format("Limit: %s %.0f", symbol, item.limit));
            holder.txtSpent.setText(String.format("Spent: %s %.0f", symbol, item.spent));
            
            double remaining = item.limit - item.spent;
            if (remaining >= 0) {
                holder.txtRemaining.setText(String.format("Left: %s %.0f", symbol, remaining));
                holder.txtRemaining.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                holder.txtRemaining.setText(String.format("Exceeded: %s %.0f", symbol, Math.abs(remaining)));
                holder.txtRemaining.setTextColor(Color.RED);
            }
            
            if (item.limit > 0) {
                int progress = (int) ((item.spent / item.limit) * 100);
                holder.progressBudget.setProgress(Math.min(100, progress));
                holder.imgWarning.setVisibility(progress > 100 ? View.VISIBLE : View.GONE);
            } else {
                holder.progressBudget.setProgress(0);
                holder.imgWarning.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> showSetLimitDialog(item));
        }

        @Override
        public int getItemCount() { return budgetList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtCategory, txtLimit, txtSpent, txtRemaining;
            ProgressBar progressBudget;
            View imgWarning;
            ViewHolder(View v) {
                super(v);
                txtCategory = v.findViewById(R.id.txtCategory);
                txtLimit = v.findViewById(R.id.txtLimit);
                txtSpent = v.findViewById(R.id.txtSpent);
                txtRemaining = v.findViewById(R.id.txtRemaining);
                progressBudget = v.findViewById(R.id.progressBudget);
                imgWarning = v.findViewById(R.id.imgWarning);
            }
        }
    }
}
