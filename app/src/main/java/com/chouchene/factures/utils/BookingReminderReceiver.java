package com.chouchene.factures.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.chouchene.factures.MainActivity;
import com.chouchene.factures.R;

public class BookingReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "booking_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        int bookingId = intent.getIntExtra("booking_id", 0);
        String clientName = intent.getStringExtra("client_name");
        String pickup = intent.getStringExtra("pickup");

        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.putExtra("navigate_to", "agenda");
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, bookingId, activityIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.rounded_calendar_today_24)
                .setContentTitle("Rappel Course: " + clientName)
                .setContentText("Départ de: " + pickup)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(bookingId, builder.build());
        } catch (SecurityException e) {
            // Handle permission issue if needed
        }
    }
}
