package com.chouchene.factures.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.chouchene.factures.entity.Booking;

public class NotificationHelper {

    private static final String CHANNEL_ID = "booking_reminders";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Rappels de courses";
            String description = "Notifications pour les courses programmées";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleBookingReminder(Context context, Booking booking) {
        long now = System.currentTimeMillis();
        long reminderTime = booking.dateTime.getTime() - (60 * 60 * 1000); // 1 heure avant
        
        // Si l'heure du rappel (1h avant) est déjà passée, mais que la course n'a pas encore commencé
        if (reminderTime < now) {
            if (booking.dateTime.getTime() > now) {
                // On programme le rappel pour dans 10 secondes pour que le chauffeur le voie immédiatement
                reminderTime = now + 10000; 
            } else {
                return; // Course déjà commencée ou passée
            }
        }

        Intent intent = new Intent(context, BookingReminderReceiver.class);
        intent.putExtra("booking_id", booking.id);
        intent.putExtra("client_name", booking.clientName);
        intent.putExtra("pickup", booking.pickupLocation);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, booking.id, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                } else {
                    // Fallback to inexact alarm if permission is missing
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            }
        }
    }
}
