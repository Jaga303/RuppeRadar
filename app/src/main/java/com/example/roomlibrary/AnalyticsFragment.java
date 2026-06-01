package com.example.roomlibrary;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AnalyticsFragment extends Fragment {

    private PieChart pieChart;
    private LineChart lineChart;
    private RecyclerView rvCategoryStats;
    private RadioGroup rgTypeSelection;
    private TextView txtOverviewTitle, txtFlowTitle;
    private CalendarView calendarView;
    private DatabaseHelper dbHelper;
    private PreferenceManager preferenceManager;
    private CategoryStatsAdapter statsAdapter;
    private List<CategoryStat> statsList = new ArrayList<>();
    
    // AI Components
    private CardView cardAiInsights;
    private ProgressBar aiLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        lineChart = view.findViewById(R.id.lineChart);
        rvCategoryStats = view.findViewById(R.id.rvCategoryStats);
        rgTypeSelection = view.findViewById(R.id.rgTypeSelection);
        txtOverviewTitle = view.findViewById(R.id.txtOverviewTitle);
        txtFlowTitle = view.findViewById(R.id.txtFlowTitle);
        calendarView = view.findViewById(R.id.calendarView);
        
        cardAiInsights = view.findViewById(R.id.cardAiInsights);
        aiLoading = view.findViewById(R.id.aiLoading);

        dbHelper = DatabaseHelper.getDB(getContext());
        preferenceManager = new PreferenceManager(getContext());

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(getContext()));
        statsAdapter = new CategoryStatsAdapter(statsList);
        rvCategoryStats.setAdapter(statsAdapter);

        rgTypeSelection.setOnCheckedChangeListener((group, checkedId) -> {
            String type = (checkedId == R.id.rbIncome) ? "Income" : "Expense";
            updateUI(type);
        });

        cardAiInsights.setOnClickListener(v -> fetchAiInsights());

        // Default to Expense
        updateUI("Expense");
        setupCalendar();

        return view;
    }

    private void fetchAiInsights() {
        aiLoading.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            List<Expense> transactions = dbHelper.expenseDao().getAllExpense();
            Map<String, Double> summary = new HashMap<>();
            String symbol = preferenceManager.getCurrencySymbol();

            for (Expense e : transactions) {
                if ("Expense".equalsIgnoreCase(e.getType())) {
                    double amt = 0;
                    try { amt = Double.parseDouble(e.getAmount()); } catch (Exception ignored) {}
                    summary.put(e.getCategory(), summary.getOrDefault(e.getCategory(), 0.0) + amt);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> entry : summary.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(symbol).append(" ").append(entry.getValue()).append("\n");
            }

            if (sb.length() == 0) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        aiLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Add some expenses first for AI analysis!", Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }

            AiHelper.getFinancialInsights(sb.toString(), new AiHelper.AiResponseListener() {
                @Override
                public void onSuccess(String response) {
                    if (getActivity() != null) {
                        aiLoading.setVisibility(View.GONE);
                        showAiDialog(response);
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        aiLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "AI Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }).start();
    }

    private void showAiDialog(String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("RuppeRadar AI Advisor")
                .setMessage(message)
                .setPositiveButton("Got it!", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        rgTypeSelection.check(rgTypeSelection.getCheckedRadioButtonId());
        String type = (rgTypeSelection.getCheckedRadioButtonId() == R.id.rbIncome) ? "Income" : "Expense";
        updateUI(type); 
    }

    private void updateUI(String type) {
        txtOverviewTitle.setText(type + " Overview");
        txtFlowTitle.setText(type + " Flow");
        
        new Thread(() -> {
            List<Expense> allTransactions = dbHelper.expenseDao().getAllExpense();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setupPieChartAndStats(allTransactions, type);
                    setupLineChart(allTransactions, type);
                });
            }
        }).start();
    }

    private void setupPieChartAndStats(List<Expense> transactions, String type) {
        Map<String, Double> categoryMap = new HashMap<>();
        double totalAmount = 0;

        for (Expense t : transactions) {
            if (t.getType() != null && t.getType().equalsIgnoreCase(type)) {
                double amount = 0;
                try {
                    amount = Double.parseDouble(t.getAmount());
                } catch (Exception e) { amount = 0; }
                
                String category = t.getCategory() != null ? t.getCategory() : "Other";
                categoryMap.put(category, categoryMap.getOrDefault(category, 0.0) + amount);
                totalAmount += amount;
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        statsList.clear();
        int[] colors = ColorTemplate.MATERIAL_COLORS;
        int colorIndex = 0;

        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            float percent = totalAmount > 0 ? (float) (entry.getValue() / totalAmount * 100) : 0;
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            
            statsList.add(new CategoryStat(
                entry.getKey(), 
                entry.getValue(), 
                percent, 
                colors[colorIndex % colors.length]
            ));
            colorIndex++;
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No data available");
            statsAdapter.notifyDataSetChanged();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.animateY(1000);
        pieChart.invalidate();

        statsAdapter.notifyDataSetChanged();
    }

    private void setupLineChart(List<Expense> transactions, String type) {
        Map<Long, Double> dateMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (Expense t : transactions) {
            if (t.getType() != null && t.getType().equalsIgnoreCase(type)) {
                try {
                    long dateKey = Long.parseLong(sdf.format(new Date(t.getDate())));
                    double amount = Double.parseDouble(t.getAmount());
                    dateMap.put(dateKey, dateMap.getOrDefault(dateKey, 0.0) + amount);
                } catch (Exception ignored) {}
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Long, Double> entry : dateMap.entrySet()) {
            entries.add(new Entry(index++, entry.getValue().floatValue()));
        }

        if (entries.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("No data available");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, type);
        int themeColor = type.equals("Expense") ? Color.RED : Color.parseColor("#2E7D32");
        dataSet.setColor(themeColor);
        dataSet.setCircleColor(themeColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(themeColor);
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long selectedDate = calendar.getTimeInMillis();

            new Thread(() -> {
                List<Expense> allTx = dbHelper.expenseDao().getAllExpense();
                double dayExp = 0, dayInc = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String target = sdf.format(new Date(selectedDate));

                for (Expense e : allTx) {
                    if (sdf.format(new Date(e.getDate())).equals(target)) {
                        double amt = 0;
                        try { amt = Double.parseDouble(e.getAmount()); } catch (Exception ignored) {}
                        if ("Expense".equalsIgnoreCase(e.getType())) dayExp += amt;
                        else dayInc += amt;
                    }
                }
                
                final double finalExp = dayExp;
                final double finalInc = dayInc;
                String symbol = preferenceManager.getCurrencySymbol();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), target + "\nIncome: " + symbol + " " + finalInc + "\nExpense: " + symbol + " " + finalExp, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private static class CategoryStat {
        String name;
        double amount;
        float percent;
        int color;

        CategoryStat(String name, double amount, float percent, int color) {
            this.name = name;
            this.amount = amount;
            this.percent = percent;
            this.color = color;
        }
    }

    private class CategoryStatsAdapter extends RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder> {
        private List<CategoryStat> list;

        CategoryStatsAdapter(List<CategoryStat> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_analytics_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryStat stat = list.get(position);
            String symbol = preferenceManager.getCurrencySymbol();
            holder.txtName.setText(stat.name);
            holder.txtAmount.setText(String.format("%s %.2f", symbol, stat.amount));
            holder.txtPercent.setText(String.format("%.1f%%", stat.percent));
            holder.progressBar.setProgress((int) stat.percent);
            holder.viewColor.setBackgroundColor(stat.color);
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtAmount, txtPercent;
            ProgressBar progressBar;
            View viewColor;

            ViewHolder(View v) {
                super(v);
                txtName = v.findViewById(R.id.txtCategoryName);
                txtAmount = v.findViewById(R.id.txtCategoryAmount);
                txtPercent = v.findViewById(R.id.txtCategoryPercent);
                progressBar = v.findViewById(R.id.progressCategory);
                viewColor = v.findViewById(R.id.viewColor);
            }
        }
    }
}
