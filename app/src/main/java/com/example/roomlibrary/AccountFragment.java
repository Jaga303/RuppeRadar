package com.example.roomlibrary;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountFragment extends Fragment {

    private DatabaseHelper db;
    private PreferenceManager preferenceManager;
    private TextView txtUserName, txtCurrentCurrency;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        db = DatabaseHelper.getDB(getContext());
        preferenceManager = new PreferenceManager(getContext());

        txtUserName = view.findViewById(R.id.txtUserName);
        txtCurrentCurrency = view.findViewById(R.id.txtCurrentCurrency);

        updateUI();

        view.findViewById(R.id.profileSection).setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.btnReset).setOnClickListener(v -> showResetConfirmation());
        view.findViewById(R.id.btnExport).setOnClickListener(v -> exportToCSV());
        view.findViewById(R.id.btnBackup).setOnClickListener(v -> Toast.makeText(getContext(), "Cloud Backup coming soon!", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnCurrency).setOnClickListener(v -> showCurrencyDialog());
        view.findViewById(R.id.btnNotifications).setOnClickListener(v -> showNotificationSettingsDialog());
        
        view.findViewById(R.id.btnViewSummary).setOnClickListener(v -> showSummaryDialog());
        view.findViewById(R.id.btnDownloadPDF).setOnClickListener(v -> generatePDFReport());

        return view;
    }

    private void updateUI() {
        txtUserName.setText(preferenceManager.getUserName());
        txtCurrentCurrency.setText("Currency (" + preferenceManager.getCurrencySymbol() + ")");
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Name");

        final EditText input = new EditText(getContext());
        input.setText(preferenceManager.getUserName());
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                preferenceManager.setUserName(newName);
                updateUI();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showNotificationSettingsDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_notification_settings, null);
        Switch swLimit = dialogView.findViewById(R.id.swLimitExceeds);
        Switch swDaily = dialogView.findViewById(R.id.swDailyAlert);
        TextView txtTime = dialogView.findViewById(R.id.txtAlertTime);

        swLimit.setChecked(preferenceManager.isLimitExceedsNotifEnabled());
        swDaily.setChecked(preferenceManager.isDailyAlertNotifEnabled());
        txtTime.setText("Time: " + preferenceManager.getDailyAlertTime());

        swLimit.setOnCheckedChangeListener((buttonView, isChecked) -> preferenceManager.setLimitExceedsNotif(isChecked));
        swDaily.setOnCheckedChangeListener((buttonView, isChecked) -> preferenceManager.setDailyAlertNotif(isChecked));

        txtTime.setOnClickListener(v -> {
            String currentTime = preferenceManager.getDailyAlertTime();
            int hour = Integer.parseInt(currentTime.split(":")[0]);
            int minute = Integer.parseInt(currentTime.split(":")[1]);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
                String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                preferenceManager.setDailyAlertTime(selectedTime);
                txtTime.setText("Time: " + selectedTime);
            }, hour, minute, true);
            timePickerDialog.show();
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Notification Settings")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showCurrencyDialog() {
        String[] currencies = {"Indian Rupee (₹)", "US Dollar ($)", "Euro (€)", "British Pound (£)", "Japanese Yen (¥)"};
        String[] symbols = {"₹", "$", "€", "£", "¥"};

        new AlertDialog.Builder(getContext())
                .setTitle("Select Currency")
                .setItems(currencies, (dialog, which) -> {
                    preferenceManager.setCurrencySymbol(symbols[which]);
                    updateUI();
                    Toast.makeText(getContext(), "Currency updated", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Reset All Data")
                .setMessage("Are you sure you want to delete all expenses and budgets? This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    new Thread(() -> {
                        db.expenseDao().deleteAllExpenses();
                        db.budgetDao().deleteAllBudgets();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "All data has been cleared", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSummaryDialog() {
        new Thread(() -> {
            List<Expense> allExpenses = db.expenseDao().getAllExpense();
            double totalInc = 0;
            double totalExp = 0;
            Map<String, Double> categoryMap = new HashMap<>();

            for (Expense e : allExpenses) {
                double val = Double.parseDouble(e.getAmount());
                if ("Income".equals(e.getType())) {
                    totalInc += val;
                } else {
                    totalExp += val;
                    categoryMap.put(e.getCategory(), categoryMap.getOrDefault(e.getCategory(), 0.0) + val);
                }
            }

            double finalTotalInc = totalInc;
            double finalTotalExp = totalExp;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    String symbol = preferenceManager.getCurrencySymbol();
                    sb.append("Total Income: ").append(symbol).append(String.format("%.2f", finalTotalInc)).append("\n");
                    sb.append("Total Expense: ").append(symbol).append(String.format("%.2f", finalTotalExp)).append("\n");
                    sb.append("Balance: ").append(symbol).append(String.format("%.2f", finalTotalInc - finalTotalExp)).append("\n\n");
                    sb.append("Category-wise Expenses:\n");
                    
                    if (categoryMap.isEmpty()) {
                        sb.append("No expenses recorded.");
                    } else {
                        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                            sb.append("• ").append(entry.getKey()).append(": ").append(symbol).append(String.format("%.2f", entry.getValue())).append("\n");
                        }
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle("Expense Summary Overview")
                            .setMessage(sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    private void generatePDFReport() {
        new Thread(() -> {
            List<Expense> list = db.expenseDao().getAllExpense();
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                if (list.isEmpty()) {
                    Toast.makeText(getContext(), "No data available for PDF report", Toast.LENGTH_SHORT).show();
                    return;
                }

                PdfDocument pdfDocument = new PdfDocument();
                Paint paint = new Paint();
                Paint titlePaint = new Paint();

                // Page description
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Header - App Name and Logo
                titlePaint.setTextSize(24);
                titlePaint.setFakeBoldText(true);
                titlePaint.setColor(Color.BLUE);
                canvas.drawText("RuppeRadar", 40, 60, titlePaint);

                paint.setTextSize(12);
                paint.setColor(Color.GRAY);
                canvas.drawText("Version: 1.0.5", 40, 80, paint);
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                canvas.drawText("Generated on: " + sdf.format(new Date()), 400, 60, paint);

                // Divider
                paint.setColor(Color.BLACK);
                canvas.drawLine(40, 100, 555, 100, paint);

                // Summary Calculation
                double totalInc = 0;
                double totalExp = 0;
                Map<String, Double> categoryMap = new HashMap<>();
                for (Expense e : list) {
                    double val = Double.parseDouble(e.getAmount());
                    if ("Income".equals(e.getType())) totalInc += val;
                    else {
                        totalExp += val;
                        categoryMap.put(e.getCategory(), categoryMap.getOrDefault(e.getCategory(), 0.0) + val);
                    }
                }

                // Draw Summary
                paint.setFakeBoldText(true);
                paint.setTextSize(16);
                canvas.drawText("Overall Summary", 40, 130, paint);
                
                paint.setFakeBoldText(false);
                paint.setTextSize(14);
                String symbol = preferenceManager.getCurrencySymbol();
                canvas.drawText("Total Income: " + symbol + String.format("%.2f", totalInc), 40, 160, paint);
                canvas.drawText("Total Expense: " + symbol + String.format("%.2f", totalExp), 40, 185, paint);
                canvas.drawText("Net Balance: " + symbol + String.format("%.2f", totalInc - totalExp), 40, 210, paint);

                // Draw Category Table
                paint.setFakeBoldText(true);
                canvas.drawText("Expense by Category", 40, 250, paint);
                canvas.drawLine(40, 255, 250, 255, paint);

                int yPos = 280;
                paint.setFakeBoldText(false);
                for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                    canvas.drawText(entry.getKey(), 60, yPos, paint);
                    canvas.drawText(symbol + String.format("%.2f", entry.getValue()), 250, yPos, paint);
                    yPos += 25;
                    if (yPos > 800) break; // Simple overflow check
                }

                pdfDocument.finishPage(page);

                try {
                    File folder = new File(getContext().getExternalCacheDir(), "Reports");
                    if (!folder.exists()) folder.mkdir();

                    String fileName = "RuppeRadar_Report_" + System.currentTimeMillis() + ".pdf";
                    File file = new File(folder, fileName);
                    pdfDocument.writeTo(new FileOutputStream(file));
                    pdfDocument.close();

                    Uri contentUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", file);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/pdf");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "RuppeRadar Financial Report");
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Open/Download PDF"));

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "PDF generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void exportToCSV() {
        new Thread(() -> {
            List<Expense> list = db.expenseDao().getAllExpense();
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                if (list.isEmpty()) {
                    Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder data = new StringBuilder();
                data.append("ID,Title,Amount,Type,Category,Payment Method,Date\n");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                for (Expense e : list) {
                    data.append(e.getId()).append(",")
                            .append(e.getTitle().replace(",", " ")).append(",")
                            .append(e.getAmount()).append(",")
                            .append(e.getType()).append(",")
                            .append(e.getCategory()).append(",")
                            .append(e.getPaymentMethod()).append(",")
                            .append(sdf.format(new Date(e.getDate()))).append("\n");
                }

                try {
                    File folder = new File(getContext().getExternalCacheDir(), "Exports");
                    if (!folder.exists()) folder.mkdir();

                    String fileName = "Expenses_" + System.currentTimeMillis() + ".csv";
                    File file = new File(folder, fileName);
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(data.toString().getBytes());
                    out.close();

                    Uri contentUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", file);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Expense Records Export");
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Download/Share CSV"));

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
