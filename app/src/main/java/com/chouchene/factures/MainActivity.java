package com.chouchene.factures;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class MainActivity extends AppCompatActivity {
    final static int REQUEST_CODE_STORAGE = 1232;
    final static int REQUEST_CODE_INTERNET = 1232;

    BottomNavigationView bottomNavigationView;
    NavController navController;
    AppBarConfiguration appBarConfiguration;

    SharedPreferences sharedPreferences;

    SearchBar searchBar;
    SearchView searchView;
    RecyclerView searchRecyclerView;
    View searchEmptyState, quickSearchContainer;
    SearchResultAdapter searchAdapter;
    AppDatabase db;
    private KonfettiView konfettiView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "fr"));
    }

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 1. Install Splash Screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // 2. Check for Onboarding
        if (sharedPreferences.getBoolean("first_run", true)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        // 3. Enable Edge-to-Edge and Set Content View
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // 4. Init common components
        db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
        konfettiView = findViewById(R.id.konfettiView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        // 5. Initial App Setup (Safe to call before/after Biometric)
        initApp();

        // 6. Biometric Security
        if (sharedPreferences.getBoolean("biometric_lock", false) && isBiometricAvailable()) {
            // Hide main content until authenticated
            findViewById(R.id.main_app_bar).setAlpha(0f);
            findViewById(R.id.nav_host_fragment).setAlpha(0f);
            findViewById(R.id.bottomNavigationView).setAlpha(0f);
            showBiometricPrompt();
        } else {
            revealContent();
        }
    }

    private void revealContent() {
        findViewById(R.id.main_app_bar).setAlpha(1f);
        findViewById(R.id.nav_host_fragment).setAlpha(1f);
        findViewById(R.id.bottomNavigationView).setAlpha(1f);
    }

    private boolean isBiometricAvailable() {
        try {
            androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
            int result = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            return result == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT || 
                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                    revealContent();
                } else {
                    Toast.makeText(MainActivity.this, "Authentification requise", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                revealContent();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentification requise")
                .setSubtitle("Accès sécurisé à vos données")
                .setNegativeButtonText("Quitter")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void initApp() {
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        searchRecyclerView = findViewById(R.id.search_results_recycler);
        searchEmptyState = findViewById(R.id.search_empty_state);
        quickSearchContainer = findViewById(R.id.quick_search_container);
        ImageView imgProfile = findViewById(R.id.img_profile_top);

        if (searchView != null && searchBar != null) {
            searchView.setupWithSearchBar(searchBar);
        }

        if (imgProfile != null) imgProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment, R.id.agendaFragment, R.id.documentsHubFragment,
                    R.id.clientsHubFragment, R.id.parametresFragment)
                    .build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // 1. Handle Selection: Normal tab switching
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                // Special case for Home: if we are in a sub-page (like Journal), pop back to Home
                if (itemId == R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            // 2. Handle Reselection: Clicking the ALREADY active tab
            bottomNavigationView.setOnItemReselectedListener(item -> {
                // This is crucial: it forces the tab to go back to its root fragment
                navController.popBackStack(item.getItemId(), false);
            });

            // Robust Back Navigation
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (searchView != null && searchView.isShowing()) {
                        searchView.hide();
                    } else if (navController.getCurrentDestination() != null && 
                               navController.getCurrentDestination().getId() == R.id.globalHistoryFragment) {
                        // Specifically handle back from Journal to Home
                        navController.popBackStack(R.id.homeFragment, false);
                    } else if (navController.getPreviousBackStackEntry() != null) {
                        navController.popBackStack();
                    } else if (navController.getCurrentDestination() != null && 
                               navController.getCurrentDestination().getId() != R.id.homeFragment) {
                        bottomNavigationView.setSelectedItemId(R.id.homeFragment);
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            });
        }

        if (searchRecyclerView != null) {
            searchAdapter = new SearchResultAdapter();
            searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            searchRecyclerView.setAdapter(searchAdapter);
        }

        if (searchView != null && searchView.getEditText() != null) {
            searchView.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    performSearch(s.toString());
                }
            });
        }

        handleIntent(getIntent());
        askPermissions();
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (searchAdapter != null) searchAdapter.setResults(new ArrayList<>());
            return;
        }

        String finalQuery = query.trim();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchResult> combinedResults = new ArrayList<>();
            List<Client> clients = db.clientDao().searchClients(finalQuery);
            for (Client c : clients) combinedResults.add(new SearchResult(c.getClientName(), c.getEmail(), "Client", null, c.getId(), 0));
            List<Invoice> invoices = db.invoiceDao().searchInvoices(finalQuery);
            for (Invoice i : invoices) combinedResults.add(new SearchResult(i.clientName, String.format(Locale.getDefault(), "%.2f €", i.amount), i.type, i.filePath, i.id, 0));
            
            List<com.chouchene.factures.entity.Booking> bookings = db.bookingDao().searchBookings(finalQuery);
            for (com.chouchene.factures.entity.Booking b : bookings) {
                combinedResults.add(new SearchResult(b.clientName, b.pickupLocation + " → " + b.destinationLocation, "Course", null, b.id, b.dateTime.getTime()));
            }

            runOnUiThread(() -> {
                if (searchAdapter != null) {
                    searchAdapter.setResults(combinedResults);
                    if (searchEmptyState != null) searchEmptyState.setVisibility(combinedResults.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    public static class SearchResult {
        public String title, subtitle, type, filePath;
        public int id;
        public long timestamp;
        public SearchResult(String title, String subtitle, String type, String filePath, int id, long timestamp) {
            this.title = title; this.subtitle = subtitle; this.type = type; this.filePath = filePath; this.id = id; this.timestamp = timestamp;
        }
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        private List<SearchResult> results = new ArrayList<>();
        public void setResults(List<SearchResult> results) {
            this.results = results; notifyDataSetChanged();
        }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult result = results.get(position);
            holder.title.setText(result.title); holder.subtitle.setText(result.subtitle); holder.type.setText(result.type);
            
            int iconRes = R.drawable.rounded_receipt_long_24;
            if ("Client".equals(result.type)) iconRes = R.drawable.rounded_person_24;
            else if ("Bon".equals(result.type)) iconRes = R.drawable.rounded_shopping_cart_24;
            else if ("Course".equals(result.type)) iconRes = R.drawable.rounded_calendar_today_24;
            holder.icon.setImageResource(iconRes);

            holder.itemView.setOnClickListener(v -> {
                searchView.hide();
                if ("Client".equals(result.type)) {
                    Bundle args = new Bundle(); args.putInt("client_id", result.id);
                    navController.navigate(R.id.clientDetailFragment, args);
                } else if ("Course".equals(result.type)) {
                    Bundle args = new Bundle(); args.putLong("selected_date", result.timestamp);
                    navController.navigate(R.id.agendaFragment, args);
                } else {
                    Bundle args = new Bundle(); args.putString("file_path", result.filePath);
                    navController.navigate(R.id.webViewPdfFragment, args);
                }
            });
        }
        @Override public int getItemCount() { return results.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle, type; ImageView icon;
            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.result_title); subtitle = itemView.findViewById(R.id.result_subtitle);
                type = itemView.findViewById(R.id.result_type); icon = itemView.findViewById(R.id.result_icon);
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "agenda".equals(intent.getStringExtra("navigate_to"))) {
            if (bottomNavigationView != null) bottomNavigationView.setSelectedItemId(R.id.agendaFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    private void askPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void triggerConfetti() {
        // Confetti logic using konfettiView...
    }
}
