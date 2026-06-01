package com.example.roomlibrary;

import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static void showBudgetWarning(Context context, String category, double spent, double limit) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sports) // Use a generic icon if needed
                .setContentTitle("Budget Exceeded!")
                .setContentText("You've spent ₹" + String.format("%.2f", spent) + " on " + category + ". Limit: ₹" + String.format("%.2f", limit))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
