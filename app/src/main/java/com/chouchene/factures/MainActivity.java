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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateInterpolator;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    com.chouchene.factures.fragments.ClientsViewModel clientsViewModel;

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
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        
        splashScreen.setOnExitAnimationListener(splashScreenViewProvider -> {
            final View splashScreenView = splashScreenViewProvider.getView();
            final View iconView = splashScreenViewProvider.getIconView();

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 5f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(500L);
            animatorSet.setInterpolator(new AccelerateInterpolator());
            animatorSet.playTogether(scaleX, scaleY, alpha);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    splashScreenViewProvider.remove();
                }
            });

            animatorSet.start();
        });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean("first_run", true)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        boolean isDarkMode = sharedPreferences.getBoolean("theme", false);
        setTheme(isDarkMode ? R.style.DarkTheme : R.style.LightTheme);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        
        try {
            if (sharedPreferences.getBoolean("biometric_lock", false) && isBiometricAvailable()) {
                showBiometricPrompt();
            } else {
                initApp();
            }
        } catch (Exception e) {
            initApp();
        }
    }

    private boolean isBiometricAvailable() {
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        int result = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        return result == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT || 
                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                    initApp();
                } else {
                    Toast.makeText(MainActivity.this, "Authentification requise", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                initApp();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentification échouée", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentification requise")
                .setSubtitle("Veuillez vous authentifier pour accéder à vos factures")
                .setNegativeButtonText("Annuler")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void initApp() {
        setContentView(R.layout.activity_main);

        db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();

        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        searchRecyclerView = findViewById(R.id.search_results_recycler);
        searchEmptyState = findViewById(R.id.search_empty_state);
        quickSearchContainer = findViewById(R.id.quick_search_container);
        ImageView imgProfile = findViewById(R.id.img_profile_top);

        searchView.setupWithSearchBar(searchBar);
        clientsViewModel = new ViewModelProvider(this).get(com.chouchene.factures.fragments.ClientsViewModel.class);

        searchView.addTransitionListener((searchView1, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWN) {
                quickSearchContainer.setVisibility(View.VISIBLE);
            }
            if (newState == SearchView.TransitionState.HIDDEN || newState == SearchView.TransitionState.HIDING) {
                searchView.getEditText().setText("");
                if (searchAdapter != null) {
                    searchAdapter.setResults(new ArrayList<>());
                }
                if (searchEmptyState != null) {
                    searchEmptyState.setVisibility(View.GONE);
                }
            }
        });

        findViewById(R.id.chip_paid).setOnClickListener(v -> searchView.getEditText().setText("Payée"));
        findViewById(R.id.chip_pending).setOnClickListener(v -> searchView.getEditText().setText("En attente"));
        findViewById(R.id.chip_recent).setOnClickListener(v -> searchView.getEditText().setText("Facture"));

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        imgProfile.setOnClickListener(v -> showProfileMenu());

        if (!sharedPreferences.getBoolean("tooltip_shown", false)) {
            imgProfile.post(() -> showTooltip(imgProfile));
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment, R.id.documentsHubFragment,
                    R.id.clientsFragment, R.id.parametresFragment)
                    .build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() != R.id.homeFragment &&
                            appBarConfiguration.getTopLevelDestinations().contains(navController.getCurrentDestination().getId())) {
                        bottomNavigationView.setSelectedItemId(R.id.homeFragment);
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            });
        }

        searchAdapter = new SearchResultAdapter();
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchRecyclerView.setAdapter(searchAdapter);

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString();
                if (quickSearchContainer != null) {
                    quickSearchContainer.setVisibility(q.isEmpty() ? View.VISIBLE : View.GONE);
                }
                performSearch(q);
            }
        });

        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            performSearch(v.getText().toString());
            return false;
        });

        askPermissions();
    }

    private void showProfileMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_profile_menu, null);
        
        SharedPreferences userPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        TextView txtName = view.findViewById(R.id.menu_user_name);
        TextView txtEmail = view.findViewById(R.id.menu_user_email);
        
        txtName.setText(userPrefs.getString("User", "Utilisateur"));
        txtEmail.setText(userPrefs.getString("email", "email@exemple.com"));

        view.findViewById(R.id.menu_item_profile).setOnClickListener(v -> {
            dialog.dismiss();
            navController.navigate(R.id.personalSettingsFragment);
        });

        view.findViewById(R.id.menu_item_settings).setOnClickListener(v -> {
            dialog.dismiss();
            navController.navigate(R.id.settingsActivity);
        });

        view.findViewById(R.id.menu_item_help).setOnClickListener(v -> {
            dialog.dismiss();
            navController.navigate(R.id.helpFragment);
        });

        view.findViewById(R.id.menu_item_about).setOnClickListener(v -> {
            dialog.dismiss();
            navController.navigate(R.id.aboutFragment);
        });

        view.findViewById(R.id.menu_item_logout).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Déconnexion...", Toast.LENGTH_SHORT).show();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (searchAdapter != null) {
                searchAdapter.setResults(new ArrayList<>());
            }
            return;
        }

        String finalQuery = query.trim();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchResult> combinedResults = new ArrayList<>();

            // Search Clients
            List<Client> clients = db.clientDao().searchClients(finalQuery);
            for (Client c : clients) {
                combinedResults.add(new SearchResult(c.getClientName(), c.getEmail(), "Client", null, c.getId()));
            }

            // Search Invoices
            List<Invoice> invoices = db.invoiceDao().searchInvoices(finalQuery);
            for (Invoice i : invoices) {
                combinedResults.add(new SearchResult(i.clientName, String.format(Locale.getDefault(), "%.2f €", i.amount), i.type, i.filePath, i.id));
            }

            runOnUiThread(() -> {
                if (searchAdapter != null) {
                    searchAdapter.setResults(combinedResults);
                    if (searchEmptyState != null) {
                        searchEmptyState.setVisibility(combinedResults.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        });
    }

    public static class SearchResult {
        public String title;
        public String subtitle;
        public String type; // "Facture", "Bon", "Client"
        public String filePath;
        public int id;

        public SearchResult(String title, String subtitle, String type, String filePath, int id) {
            this.title = title;
            this.subtitle = subtitle;
            this.type = type;
            this.filePath = filePath;
            this.id = id;
        }
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        private List<SearchResult> results = new ArrayList<>();

        public void setResults(List<SearchResult> results) {
            this.results = results;
            notifyDataSetChanged();
            if (searchRecyclerView != null) {
                searchRecyclerView.scrollToPosition(0);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult result = results.get(position);
            holder.title.setText(result.title);
            holder.subtitle.setText(result.subtitle);
            holder.type.setText(result.type);

            if ("Client".equals(result.type)) {
                holder.icon.setImageResource(R.drawable.rounded_person_24);
            } else if ("Bon".equals(result.type)) {
                holder.icon.setImageResource(R.drawable.rounded_shopping_cart_24);
            } else {
                holder.icon.setImageResource(R.drawable.rounded_receipt_long_24);
            }

            holder.itemView.setOnClickListener(v -> {
                searchView.hide();
                if ("Client".equals(result.type)) {
                    Bundle args = new Bundle();
                    args.putInt("client_id", result.id);
                    navController.navigate(R.id.clientDetailFragment, args);
                } else {
                    Bundle args = new Bundle();
                    args.putString("file_path", result.filePath);
                    
                    Executors.newSingleThreadExecutor().execute(() -> {
                        com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(result.title);
                        runOnUiThread(() -> {
                            if (client != null) {
                                args.putString("mail_client", client.getEmail());
                            }
                            navController.navigate(R.id.webViewPdfFragment, args);
                        });
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle, type;
            ImageView icon;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.result_title);
                subtitle = itemView.findViewById(R.id.result_subtitle);
                type = itemView.findViewById(R.id.result_type);
                icon = itemView.findViewById(R.id.result_icon);
            }
        }
    }

    private void showTooltip(View anchor) {
        View tooltipView = getLayoutInflater().inflate(R.layout.layout_tooltip, null);
        TextView textView = tooltipView.findViewById(R.id.tooltip_text);
        textView.setText(R.string.tooltip_profile);

        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                tooltipView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setElevation(10);
        popupWindow.showAsDropDown(anchor, 0, 10);

        sharedPreferences.edit().putBoolean("tooltip_shown", true).apply();
    }

    private void askPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_INTERNET);
    }
}
