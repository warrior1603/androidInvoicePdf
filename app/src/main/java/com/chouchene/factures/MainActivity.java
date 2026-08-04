package com.chouchene.factures;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

import com.chouchene.factures.utils.LocaleHelper;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.entity.Client;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.splashscreen.SplashScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    final static int REQUEST_CODE_STORAGE = 1232;
    final static int REQUEST_CODE_INTERNET = 1232;

    BottomNavigationView bottomNavigationView;
    NavController navController;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    AppBarConfiguration appBarConfiguration;

    SharedPreferences sharedPreferences;

    SearchBar searchBar;
    SearchView searchView;
    RecyclerView searchRecyclerView;
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
        SplashScreen.installSplashScreen(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean("theme", false);
        setTheme(isDarkMode ? R.style.DarkTheme : R.style.LightTheme);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyClients")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        searchRecyclerView = findViewById(R.id.search_results_recycler);

        searchView.setupWithSearchBar(searchBar);
        clientsViewModel = new ViewModelProvider(this).get(com.chouchene.factures.fragments.ClientsViewModel.class);

        searchView.addTransitionListener((searchView1, previousState, newState) -> {
            if (newState == SearchView.TransitionState.HIDDEN || newState == SearchView.TransitionState.HIDING) {
                searchView.getEditText().setText("");
                if (searchAdapter != null) {
                    searchAdapter.setResults(new ArrayList<>());
                }
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);

        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.documentsHubFragment, R.id.clientsFragment, R.id.parametresFragment)
                    .setOpenableLayout(drawerLayout)
                    .build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        searchAdapter = new SearchResultAdapter();
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchRecyclerView.setAdapter(searchAdapter);

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                performSearch(s.toString());
            }
        });

        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            performSearch(v.getText().toString());
            return false;
        });

        askPermissions();
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
        List<SearchResult> combinedResults = new ArrayList<>();

        if (db == null) return;

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

        if (searchAdapter != null) {
            searchAdapter.setResults(combinedResults);
        }
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
                holder.icon.setImageResource(R.drawable.baseline_person_24);
            } else if ("Bon".equals(result.type)) {
                holder.icon.setImageResource(R.drawable.baseline_shopping_cart_24);
            } else {
                holder.icon.setImageResource(R.drawable.baseline_receipt_long_24);
            }

            holder.itemView.setOnClickListener(v -> {
                searchView.hide();
                if ("Client".equals(result.type)) {
                    clientsViewModel.setHighlightClientId(result.id);
                    
                    NavOptions options = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setRestoreState(true)
                            .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                            .build();
                    navController.navigate(R.id.clientsFragment, null, options);
                } else {
                    Bundle args = new Bundle();
                    args.putString("file_path", result.filePath);
                    
                    com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(result.title);
                    if (client != null) {
                        args.putString("mail_client", client.getEmail());
                    }
                    
                    navController.navigate(R.id.webViewPdfFragment, args);
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

    private void askPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_INTERNET);
    }
}