package com.example.roomlibrary;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpenseNotificationService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        String packageName = sbn.getPackageName();

        // UPI / Payment apps
        if (!isPaymentApp(packageName)) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        if (extras == null) return;

        String text = extras.getString(Notification.EXTRA_TEXT);
        if (text == null) return;

        // Example: "Paid ₹250 to Swiggy"
        Double amount = extractAmount(text);

        if (amount != null) {
            saveExpense(amount, text, packageName);
        }
    }

    private boolean isPaymentApp(String pkg) {
        return pkg.equals("com.google.android.apps.nbu.paisa.user") || // GPay
                pkg.equals("com.phonepe.app") ||
                pkg.equals("net.one97.paytm");
    }

    private Double extractAmount(String text) {
        Pattern pattern = Pattern.compile("₹\\s?(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }

    private void saveExpense(Double amount, String note, String app) {
        Log.d("EXPENSE", "Added ₹" + amount + " | " + note);

        // TODO: Save to SQLite / Room / Firebase
    }
}
