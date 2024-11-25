package com.chouchene.factures;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import com.chouchene.factures.fragments.BonDeCommandeFragment;
import com.chouchene.factures.fragments.RapportsFragment;
import com.chouchene.factures.fragments.InvoiceGenrationFragment;
import com.chouchene.factures.fragments.ListeClientsFragment;
import com.chouchene.factures.fragments.PersonalSettingsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView
        .OnNavigationItemSelectedListener{
    final static int REQUEST_CODE_STORAGE = 1232;
    final static int REQUEST_CODE_INTERNET = 1232;

    BottomNavigationView bottomNavigationView;

    InvoiceGenrationFragment generateInvoice = new InvoiceGenrationFragment();
    PersonalSettingsFragment personalSettingsFragment = new PersonalSettingsFragment();
    ListeClientsFragment listeClientsFragment = new ListeClientsFragment();
    RapportsFragment rapportsFragment = new RapportsFragment();
    BonDeCommandeFragment bonDeCommandeFragment = new BonDeCommandeFragment();

    SharedPreferences sharedPreferences;

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

    // Displaying the main layout
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        myToolbar.setOnMenuItemClickListener(
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent = new Intent(getApplication(), SettingsActivity.class);
                        startActivity(intent);
                        return true;
                    }
                }
        );

        bottomNavigationView
                = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        String lastFragment = sharedPreferences.getString("last_fragment", "MainFragment");

        if ("SettingsFragment".equals(lastFragment)) {
            bottomNavigationView.setSelectedItemId(R.id.parametresFragment);
            sharedPreferences.edit().putString("last_fragment", "MainFragment").apply();
        }

        else {
            bottomNavigationView.setSelectedItemId(R.id.factureFragment);
        }

        askPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_app_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void applyTheme() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean("theme", false);
        if (isDarkMode) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }
    }

    // Asking necessary permissions
    private void askPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_INTERNET);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.factureFragment:
                //animateBottomNav(bottomNavigationView, item);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, generateInvoice)
                        .commit();
                return true;

            case R.id.bonCommandeFragment:
                //animateBottomNav(bottomNavigationView, item);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, bonDeCommandeFragment)
                        .commit();
                return true;

            case R.id.entrepriseFragment:
                //animateBottomNav(bottomNavigationView, item);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, personalSettingsFragment)
                        .commit();
                return true;

            case R.id.clientsFragment:
                //animateBottomNav(bottomNavigationView, item);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, listeClientsFragment)
                        .commit();
                return true;

            case R.id.parametresFragment:
                //animateBottomNav(bottomNavigationView, item);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, rapportsFragment, "SettingsFragment")
                        .commit();
                return true;
        }
        return false;
    }

    private void animateBottomNav(BottomNavigationView bottomNavigationView, MenuItem item) {
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            MenuItem menuItem = bottomNavigationView.getMenu().getItem(i);
            View view = bottomNavigationView.findViewById(menuItem.getItemId());
            if (menuItem == item) {
                // Animate selected item
                //view.animate().scaleX(1.3f).scaleY(1.3f).setDuration(300).start();
                view.animate()
                        .rotationYBy(360f)
                        .setDuration(500);
            } else {
                // Reset other items
                view.animate()
                        .rotationYBy(0f)
                        .setDuration(300);
            }
        }
    }

}