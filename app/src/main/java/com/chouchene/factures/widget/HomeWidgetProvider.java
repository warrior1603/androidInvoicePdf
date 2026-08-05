package com.chouchene.factures.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.chouchene.factures.MainActivity;
import com.chouchene.factures.R;
import com.chouchene.factures.database.DatabaseClient;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HomeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_home_widget);

        // Click to open app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_add, pendingIntent);

        // Fetch data
        Executors.newSingleThreadExecutor().execute(() -> {
            float revenue = DatabaseClient.getInstance(context).getAppDatabase().invoiceDao().getMonthlyIncome(new Date());
            int count = DatabaseClient.getInstance(context).getAppDatabase().invoiceDao().getMonthlyCount(new Date());

            views.setTextViewText(R.id.widget_revenue, String.format(Locale.getDefault(), "%.2f €", revenue));
            views.setTextViewText(R.id.widget_docs, count + " documents");

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }
}
