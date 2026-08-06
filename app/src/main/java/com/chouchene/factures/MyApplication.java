package com.chouchene.factures;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.utils.LocaleHelper;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import android.content.Context;
import android.app.Activity;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "fr"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Handle Light/Dark mode globally
        boolean isDarkMode = sharedPreferences.getBoolean("theme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
