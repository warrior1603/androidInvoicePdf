package com.chouchene.factures;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.utils.LocaleHelper;
import com.chouchene.factures.utils.NotificationHelper;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import android.content.Context;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "fr"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Force Light Mode as requested to preserve the specific executive design
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // 2. Create Notification Channel
        NotificationHelper.createNotificationChannel(this);
    }
}
