package com.example.roomlibrary;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    FloatingActionButton fltAdd;
    RecyclerView recyclerView;
    TextView txtExpense, txtIncome, txtTotal, txtCurrentMonth, txtTodayDate;
    ImageView imgPreviousMonth, imgNextMonth;

    DatabaseHelper databaseHelper;
    ExpenseAdapter expenseAdapter;
    PreferenceManager preferenceManager;
    ArrayList<ExpenseModel> arrExpense = new ArrayList<>();
    ArrayList<CategoryModel> categoryList = new ArrayList<>();

    String selectedCategory = "Food";
    int selectedIcon = R.drawable.food;
    long selectedTimestamp;

    Calendar currentCalendar;
    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fltAdd = view.findViewById(R.id.fltAdd);
        recyclerView = view.findViewById(R.id.recExp);
        txtExpense = view.findViewById(R.id.txt1);
        txtIncome = view.findViewById(R.id.txt2);
        txtTotal = view.findViewById(R.id.txt3);
        txtCurrentMonth = view.findViewById(R.id.txtCurrentMonth);
        txtTodayDate = view.findViewById(R.id.txtTodayDate);
        imgPreviousMonth = view.findViewById(R.id.imgPreviousMonth);
        imgNextMonth = view.findViewById(R.id.imgNextMonth);

        databaseHelper = DatabaseHelper.getDB(getContext());
        preferenceManager = new PreferenceManager(getContext());
        currentCalendar = Calendar.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(getContext(), arrExpense, new ExpenseAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ExpenseModel model) {
                showEditDialog(model.getId());
            }

            @Override
            public void onItemDeleted() {
                loadExpensesForMonth();
            }
        });
        recyclerView.setAdapter(expenseAdapter);

        txtTodayDate.setText("Today: " + dateFormat.format(new Date()));
        updateMonthDisplay();

        imgPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
        });

        imgNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
        });

        fltAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpensesForMonth(); // Refresh to catch currency changes
    }

    private void updateMonthDisplay() {
        txtCurrentMonth.setText(monthYearFormat.format(currentCalendar.getTime()));
        loadExpensesForMonth();
    }

    private void loadExpensesForMonth() {
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        String symbol = preferenceManager.getCurrencySymbol();

        new Thread(() -> {
            List<Expense> expenses = databaseHelper.expenseDao().getTransactionsByPeriod(start, end);
            double totalExp = databaseHelper.expenseDao().getTotalExpenseByPeriod(start, end);
            double totalInc = databaseHelper.expenseDao().getTotalIncomeByPeriod(start, end);
            double balance = totalInc - totalExp;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    arrExpense.clear();
                    for (Expense e : expenses) {
                        arrExpense.add(new ExpenseModel(e.getId(), e.getIcon(), e.getAmount(), e.getTitle()));
                    }
                    expenseAdapter.notifyDataSetChanged();

                    txtExpense.setText(String.format("%s %.2f", symbol, totalExp));
                    txtIncome.setText(String.format("%s %.2f", symbol, totalInc));
                    txtTotal.setText(String.format("%s %.2f", symbol, balance));

                    if (balance < 0) {
                        txtTotal.setTextColor(Color.parseColor("#E53935"));
                    } else if (balance > 0) {
                        txtTotal.setTextColor(Color.parseColor("#2E7D32"));
                    } else {
                        txtTotal.setTextColor(Color.BLACK);
                    }
                });
            }
        }).start();
    }

    private void showAddDialog() {
        showTransactionDialog(null);
    }

    private void showEditDialog(int expenseId) {
        new Thread(() -> {
            Expense expense = databaseHelper.expenseDao().getExpenseById(expenseId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (expense != null) {
                        showTransactionDialog(expense);
                    }
                });
            }
        }).start();
    }

    private void showTransactionDialog(@Nullable Expense existingExpense) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_add);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RadioGroup rgType = dialog.findViewById(R.id.rgType);
        RadioGroup rgPaymentMethod = dialog.findViewById(R.id.rgPaymentMethod);
        EditText edtTitle = dialog.findViewById(R.id.edtTitle);
        EditText edtAmount = dialog.findViewById(R.id.edtAmount);
        TextView txtDate = dialog.findViewById(R.id.txtDate);
        Button btnSave = dialog.findViewById(R.id.btnAdd);
        TextView typeCategory = dialog.findViewById(R.id.typeCategory);
        TextView txtDialogTitle = dialog.findViewById(R.id.txtDialogTitle);

        if (existingExpense != null) {
            txtDialogTitle.setText("Edit Transaction");
            btnSave.setText("Update");
            edtTitle.setText(existingExpense.getTitle());
            edtAmount.setText(existingExpense.getAmount());
            selectedTimestamp = existingExpense.getDate();
            selectedCategory = existingExpense.getCategory();
            selectedIcon = existingExpense.getIcon();
            
            if (existingExpense.getType().equals("Income")) rgType.check(R.id.rbIncome);
            else rgType.check(R.id.rbExpense);

            if (existingExpense.getPaymentMethod().equals("Card")) rgPaymentMethod.check(R.id.rbCard);
            else if (existingExpense.getPaymentMethod().equals("Online")) rgPaymentMethod.check(R.id.rbOnline);
            else if (existingExpense.getPaymentMethod().equals("Saving")) rgPaymentMethod.check(R.id.rbSaving);
            else rgPaymentMethod.check(R.id.rbCash);
        } else {
            txtDialogTitle.setText("Add Transaction");
            btnSave.setText("Add");
            selectedTimestamp = System.currentTimeMillis();
            selectedCategory = "Food";
            selectedIcon = R.drawable.food;
        }

        txtDate.setText(dateFormat.format(new Date(selectedTimestamp)));
        typeCategory.setText(selectedCategory);

        txtDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedTimestamp);
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                selectedTimestamp = selected.getTimeInMillis();
                txtDate.setText(dateFormat.format(selected.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        typeCategory.setOnClickListener(v -> showCategoryDialog(typeCategory));

        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String amountStr = edtAmount.getText().toString().trim();
            
            int selectedTypeId = rgType.getCheckedRadioButtonId();
            String type = (selectedTypeId == R.id.rbIncome) ? "Income" : "Expense";

            int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
            String paymentMethod = "Cash";
            if (selectedPaymentId == R.id.rbCard) paymentMethod = "Card";
            else if (selectedPaymentId == R.id.rbOnline) paymentMethod = "Online";
            else if (selectedPaymentId == R.id.rbSaving) paymentMethod = "Saving";

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Enter title & amount", Toast.LENGTH_SHORT).show();
                return;
            }

            final String finalPaymentMethod = paymentMethod;
            final String finalCategory = selectedCategory;
            new Thread(() -> {
                if (existingExpense != null) {
                    existingExpense.setTitle(title);
                    existingExpense.setAmount(amountStr);
                    existingExpense.setIcon(selectedIcon);
                    existingExpense.setType(type);
                    existingExpense.setDate(selectedTimestamp);
                    existingExpense.setCategory(finalCategory);
                    existingExpense.setPaymentMethod(finalPaymentMethod);
                    databaseHelper.expenseDao().updateTx(existingExpense);
                } else {
                    Expense newTx = new Expense(title, amountStr, selectedIcon, type, selectedTimestamp, finalCategory, finalPaymentMethod);
                    databaseHelper.expenseDao().addTx(newTx);
                }

                // Budget Check
                if (type.equals("Expense") && preferenceManager.isLimitExceedsNotifEnabled()) {
                    Budget budget = databaseHelper.budgetDao().getBudgetByCategory(finalCategory);
                    if (budget != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(selectedTimestamp);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        long monthStart = cal.getTimeInMillis();
                        
                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        long monthEnd = cal.getTimeInMillis();
                        
                        double spentInCategory = databaseHelper.expenseDao().getTotalExpenseByCategoryAndPeriod(finalCategory, monthStart, monthEnd);
                        
                        if (spentInCategory > budget.getLimitAmount()) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    NotificationHelper.showBudgetWarning(getContext(), finalCategory, spentInCategory, budget.getLimitAmount());
                                });
                            }
                        }
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), (existingExpense != null ? "Updated" : "Added"), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadExpensesForMonth();
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private void showCategoryDialog(TextView typeCategoryView) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.type_category);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RecyclerView rv = dialog.findViewById(R.id.rvCategory);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        categoryList.clear();
        categoryList.add(new CategoryModel("Food", R.drawable.food, Color.RED));
        categoryList.add(new CategoryModel("Shopping", R.drawable.shopping_card, Color.BLUE));
        categoryList.add(new CategoryModel("Transport", R.drawable.bus_transport, Color.GREEN));
        categoryList.add(new CategoryModel("Health", R.drawable.health, Color.MAGENTA));
        categoryList.add(new CategoryModel("Salary", R.drawable.ic_sports, Color.YELLOW));

        CategoryAdapter adapter = new CategoryAdapter(getContext(), categoryList, category -> {
            selectedCategory = category.name;
            selectedIcon = category.icon;
            typeCategoryView.setText(selectedCategory);
            dialog.dismiss();
        });

        rv.setAdapter(adapter);
        dialog.show();
    }
}
