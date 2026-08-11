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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AnticipateInterpolator;
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
import com.chouchene.factures.fragments.FilterBottomSheet;
import com.chouchene.factures.fragments.NotificationBottomSheet;
import com.chouchene.factures.model.AppNotification;
import com.chouchene.factures.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    View searchEmptyState, filterBar;
    com.google.android.material.chip.ChipGroup smartFilterChips;
    SearchResultAdapter searchAdapter;
    AppDatabase db;
    private String filterStatus, filterType;
    private KonfettiView konfettiView;
    private List<AppNotification> currentNotifications = new ArrayList<>();

    private void triggerNotificationSheet() {
        NotificationBottomSheet sheet = new NotificationBottomSheet();
        sheet.setNotifications(currentNotifications);
        sheet.show(getSupportFragmentManager(), "NOTIFICATIONS");
        View badge = findViewById(R.id.notification_badge);
        if (badge != null) badge.setVisibility(View.GONE);
    }

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
        // 1. Install Splash Screen with a premium exit animation
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        
        splashScreen.setOnExitAnimationListener(splashScreenViewProvider -> {
            final View splashScreenView = splashScreenViewProvider.getView();
            final View iconView = splashScreenViewProvider.getIconView();

            // 1. Initial Zoom In (Anticipation)
            ObjectAnimator zoomX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.4f);
            ObjectAnimator zoomY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.4f);
            AnimatorSet zoomIn = new AnimatorSet();
            zoomIn.setDuration(400L);
            zoomIn.setInterpolator(new android.view.animation.OvershootInterpolator());
            zoomIn.playTogether(zoomX, zoomY);

            // 2. Final Exit (Portal effect)
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1.4f, 0.1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1.4f, 0.1f);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(iconView, View.TRANSLATION_Y, 0f, -1000f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f);

            AnimatorSet exitPortal = new AnimatorSet();
            exitPortal.setDuration(600L);
            exitPortal.setInterpolator(new android.view.animation.AnticipateInterpolator(1.2f));
            exitPortal.playTogether(scaleX, scaleY, translationY, alpha);

            // Sequence: Zoom -> Portal
            AnimatorSet fullAnimation = new AnimatorSet();
            fullAnimation.playSequentially(zoomIn, exitPortal);

            fullAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    splashScreenViewProvider.remove();
                }
            });

            fullAnimation.start();
        });

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
        filterBar = findViewById(R.id.filter_scroll_view);
        smartFilterChips = findViewById(R.id.smart_filter_chips);

        if (searchView != null && searchBar != null) {
            searchView.setupWithSearchBar(searchBar);
            
            // Inflate menu into SearchView as well
            searchView.inflateMenu(R.menu.search_bar_menu);
            
            // Unified listener for both SearchBar and SearchView menu items
            androidx.appcompat.widget.Toolbar.OnMenuItemClickListener menuListener = item -> {
                int id = item.getItemId();
                if (id == R.id.action_filter) {
                    if (filterBar != null) {
                        // Ensure SearchView is showing if we clicked from SearchBar
                        if (!searchView.isShowing()) searchView.show();
                        
                        int visibility = (filterBar.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                        filterBar.setVisibility(visibility);
                        
                        if (visibility == View.GONE) {
                            if (smartFilterChips != null) smartFilterChips.clearCheck();
                            filterType = null;
                            filterStatus = null;
                        }
                        String currentQuery = (searchView.getEditText() != null) ? searchView.getEditText().getText().toString() : "";
                        performSearch(currentQuery);
                    }
                    return true;
                } else if (id == R.id.action_notifications) {
                    triggerNotificationSheet();
                    return true;
                } else if (id == R.id.action_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                }
                return false;
            };

            searchBar.setOnMenuItemClickListener(menuListener);
            searchView.setOnMenuItemClickListener(menuListener);
        }

        if (smartFilterChips != null) {
            smartFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    filterType = null;
                    filterStatus = null;
                } else {
                    int id = checkedIds.get(0);
                    if (id == R.id.chip_type_invoice) { filterType = "Factures"; filterStatus = null; }
                    else if (id == R.id.chip_type_order) { filterType = "Bons"; filterStatus = null; }
                    else if (id == R.id.chip_type_booking) { filterType = "Courses"; filterStatus = null; }
                    else if (id == R.id.chip_status_paid) { filterStatus = "Payée"; filterType = null; }
                    else if (id == R.id.chip_status_pending) { filterStatus = "En attente"; filterType = null; }
                }
                String currentQuery = (searchView.getEditText() != null) ? searchView.getEditText().getText().toString() : "";
                performSearch(currentQuery);
            });
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment, R.id.agendaFragment, R.id.documentsHubFragment,
                    R.id.clientsHubFragment, R.id.parametresFragment)
                    .build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // Hide/Show BottomNav based on destination
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.webViewPdfFragment) {
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            });

            // 1. Handle Selection: Normal tab switching
            bottomNavigationView.setOnItemSelectedListener(item -> {
                // Pillar 4: Haptic Feedback
                bottomNavigationView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                
                // Animated Icon Logic: Bounce effect
                animateBottomNavigationItem(item.getItemId());

                int itemId = item.getItemId();
                // Special case for Home: if we are in a sub-page (like Journal), pop back to Home
                if (itemId == R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            // 2. Handle Reselection: Clicking the ALREADY active tab
            bottomNavigationView.setOnItemReselectedListener(item -> {
                // Pillar 4: Haptic Feedback
                bottomNavigationView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                
                // Re-trigger animation on reselect
                animateBottomNavigationItem(item.getItemId());

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
        setupNotifications();
    }

    private void setupNotifications() {
        View badge = findViewById(R.id.notification_badge);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<AppNotification> notifs = new ArrayList<>();
            
            // 1. Check for unpaid invoices (overdue)
            int overdueCount = db.invoiceDao().getOverdueInvoicesCount();
            if (overdueCount > 0) {
                notifs.add(new AppNotification("Factures en retard", 
                        overdueCount + " facture(s) sont en attente de paiement.", 
                        AppNotification.Type.ALERT, 
                        R.id.documentsHubFragment,
                        R.drawable.rounded_access_time_24));
            }

            // 2. Check for today's bookings
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            Date start = cal.getTime();
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            Date end = cal.getTime();
            
            List<com.chouchene.factures.entity.Booking> todayBookings = db.bookingDao().getBookingsBetweenDates(start, end);
            if (!todayBookings.isEmpty()) {
                notifs.add(new AppNotification("Courses du jour", 
                        "Vous avez " + todayBookings.size() + " course(s) prévue(s) aujourd'hui.", 
                        AppNotification.Type.INFO, 
                        R.id.agendaFragment,
                        R.drawable.rounded_directions_car_24));
            }

            runOnUiThread(() -> {
                this.currentNotifications = notifs;
                if (badge != null) {
                    badge.setVisibility(notifs.isEmpty() ? View.GONE : View.VISIBLE);
                }
            });
        });
    }

    private void performSearch(String query) {
        String finalQuery = (query == null) ? "" : query.trim();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchResult> combinedResults = new ArrayList<>();
            
            // 1. Clients
            if (filterType == null || "Clients".equalsIgnoreCase(filterType)) {
                List<Client> clients = db.clientDao().searchClients(finalQuery);
                for (Client c : clients) combinedResults.add(new SearchResult(c.getClientName(), c.getEmail(), "Client", null, c.getId(), 0));
            }
            
            // 2. Invoices / Bons
            List<Invoice> invoices = db.invoiceDao().searchInvoices(finalQuery);
            for (Invoice i : invoices) {
                // Apply Type Filter
                if (filterType != null) {
                    if ("Factures".equalsIgnoreCase(filterType) && !"Facture".equalsIgnoreCase(i.type)) continue;
                    if ("Bons".equalsIgnoreCase(filterType) && !"Bon".equalsIgnoreCase(i.type)) continue;
                    if ("Courses".equalsIgnoreCase(filterType)) continue; 
                }
                
                // Apply Status Filter
                if (filterStatus != null && !filterStatus.equalsIgnoreCase(i.status)) continue;

                combinedResults.add(new SearchResult(i.clientName, String.format(Locale.getDefault(), "%.2f €", i.amount), i.type, i.filePath, i.id, 0));
            }
            
            // 3. Bookings
            if (filterType == null || "Courses".equalsIgnoreCase(filterType)) {
                List<com.chouchene.factures.entity.Booking> bookings = db.bookingDao().searchBookings(finalQuery);
                for (com.chouchene.factures.entity.Booking b : bookings) {
                    combinedResults.add(new SearchResult(b.clientName, b.pickupLocation + " → " + b.destinationLocation, "Course", null, b.id, b.dateTime.getTime()));
                }
            }

            runOnUiThread(() -> {
                if (searchAdapter != null) {
                    searchAdapter.setResults(combinedResults);
                    if (searchRecyclerView != null) searchRecyclerView.scheduleLayoutAnimation();
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
            holder.title.setText(result.title);
            holder.subtitle.setText(result.subtitle);
            holder.type.setText(result.type);
            
            int iconRes = R.drawable.ic_receipt_outline;
            int typeColor;
            int typeBg;

            if ("Client".equals(result.type)) {
                iconRes = R.drawable.ic_nav_user_outline;
                typeColor = ContextCompat.getColor(MainActivity.this, R.color.white);
                typeBg = R.drawable.bg_badge_green;
            } else if ("Bon".equals(result.type)) {
                iconRes = R.drawable.ic_shopping_cart_outline;
                typeColor = ContextCompat.getColor(MainActivity.this, R.color.white);
                typeBg = R.drawable.bg_badge_orange;
            } else if ("Course".equals(result.type)) {
                iconRes = R.drawable.ic_calendar_event_outline;
                typeColor = ContextCompat.getColor(MainActivity.this, R.color.white);
                typeBg = R.drawable.bg_badge_purple;
            } else {
                iconRes = R.drawable.ic_receipt_outline;
                typeColor = ContextCompat.getColor(MainActivity.this, R.color.white);
                typeBg = R.drawable.bg_badge_blue;
            }

            holder.icon.setImageResource(iconRes);
            holder.type.setTextColor(typeColor);
            holder.type.setBackgroundResource(typeBg);

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
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomNavBadges();
    }

    public void updateBottomNavBadges() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if context is still valid
            if (isFinishing() || isDestroyed()) return;

            // 1. Pending Invoices Badge
            int pendingCount = db.invoiceDao().getCountByStatus("En attente");
            
            // 2. Today's Bookings Badge
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date start = cal.getTime();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date end = cal.getTime();
            int todayBookings = db.bookingDao().getUpcomingCount(start, end);

            runOnUiThread(() -> {
                if (bottomNavigationView == null || isFinishing() || isDestroyed()) return;

                // Documents Badge
                if (pendingCount > 0) {
                    com.google.android.material.badge.BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.documentsHubFragment);
                    badge.setVisible(true);
                    badge.setNumber(pendingCount);
                    badge.setBackgroundColor(ContextCompat.getColor(this, R.color.error));
                } else {
                    bottomNavigationView.removeBadge(R.id.documentsHubFragment);
                }

                // Agenda Badge
                if (todayBookings > 0) {
                    com.google.android.material.badge.BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.agendaFragment);
                    badge.setVisible(true);
                    badge.setNumber(todayBookings);
                    badge.setBackgroundColor(ContextCompat.getColor(this, R.color.error));
                } else {
                    bottomNavigationView.removeBadge(R.id.agendaFragment);
                }
            });
        });
    }

    public void triggerConfetti() {
        if (konfettiView == null) return;
        
        konfettiView.postDelayed(() -> {
            EmitterConfig emitterConfig = new Emitter(1, TimeUnit.SECONDS).perSecond(100);
            konfettiView.start(
                    new PartyFactory(emitterConfig)
                            .angle(nl.dionsegijn.konfetti.core.Angle.BOTTOM)
                            .spread(nl.dionsegijn.konfetti.core.Spread.ROUND)
                            .shapes(Shape.Circle.INSTANCE, Shape.Square.INSTANCE)
                            .position(0.0, 0.0, 1.0, 0.0)
                            .sizes(new Size(8, 50, 10))
                            .colors(Arrays.asList(0x3F51B5, 0x2E7D32, 0x81C784, 0x7986CB))
                            .build()
            );
        }, 200L);
    }

    private void animateBottomNavigationItem(int itemId) {
        // Use a slight delay to let the selection logic finish
        bottomNavigationView.postDelayed(() -> {
            View itemView = bottomNavigationView.findViewById(itemId);
            if (itemView != null) {
                View iconContainer = itemView.findViewById(com.google.android.material.R.id.navigation_bar_item_icon_container);
                View target = (iconContainer != null) ? iconContainer : itemView;

                // 1. Reset scale first
                target.setScaleX(1f);
                target.setScaleY(1f);

                // 2. Dramatically scale up and then bounce back
                target.animate()
                        .scaleX(1.4f)
                        .scaleY(1.4f)
                        .setDuration(300) // Slower scale up
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> target.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(600) // Much slower bounce back for premium feel
                                .setInterpolator(new android.view.animation.OvershootInterpolator(5f))
                                .start())
                        .start();
            }
        }, 50L);
    }
}
