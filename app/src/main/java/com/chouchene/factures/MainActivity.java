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
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import com.chouchene.factures.utils.LocaleHelper;
import com.chouchene.factures.fragments.BonDeCommandeFragment;
import com.chouchene.factures.fragments.RapportsFragment;
import com.chouchene.factures.fragments.InvoiceGenrationFragment;
import com.chouchene.factures.fragments.ListeClientsFragment;
import com.chouchene.factures.fragments.PersonalSettingsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.CredentialManagerCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;

import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.entity.Client;
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
    CredentialManager credentialManager;

    SearchBar searchBar;
    SearchView searchView;
    RecyclerView searchRecyclerView;
    SearchResultAdapter searchAdapter;
    AppDatabase db;

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

    // Displaying the main layout
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean("theme", false);
        setTheme(isDarkMode ? R.style.DarkTheme : R.style.LightTheme);
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

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.factureFragment, R.id.bonCommandeFragment,
                    R.id.clientsFragment, R.id.parametresFragment)
                    .setOpenableLayout(drawerLayout)
                    .build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        searchBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.search_profile) {
                showLoginBottomSheet();
                return true;
            }
            return false;
        });

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

        credentialManager = CredentialManager.create(this);

        loadUserProfile();

        String lastFragment = sharedPreferences.getString("last_fragment", "MainFragment");

        if ("SettingsFragment".equals(lastFragment)) {
            bottomNavigationView.setSelectedItemId(R.id.parametresFragment);
            sharedPreferences.edit().putString("last_fragment", "MainFragment").apply();
        }

        askPermissions();
    }

    private void loadUserProfile() {
        String profilePicUrl = sharedPreferences.getString("profile_pic_url", null);
        MenuItem profileItem = searchBar.getMenu().findItem(R.id.search_profile);
        if (profileItem == null) return;

        if (profilePicUrl != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(profilePicUrl)
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            profileItem.setIcon(new BitmapDrawable(getResources(), resource));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            profileItem.setIcon(placeholder);
                        }
                    });
        } else {
            profileItem.setIcon(R.drawable.baseline_person_24);
        }
    }

    private void showLoginBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_google_login, null);
        bottomSheetDialog.setContentView(view);

        com.google.android.material.button.MaterialButton loginBtn = view.findViewById(R.id.btn_google_login);
        TextView title = view.findViewById(R.id.login_title);
        TextView subtitle = view.findViewById(R.id.login_subtitle);

        String userName = sharedPreferences.getString("user_name", null);
        if (userName != null) {
            // User is logged in, show logout option
            title.setText(userName);
            subtitle.setText(sharedPreferences.getString("user_email", "Connecté"));
            loginBtn.setText("Se déconnecter");
            loginBtn.setIcon(null); 
            loginBtn.setOnClickListener(v -> {
                logoutUser();
                bottomSheetDialog.dismiss();
            });
        } else {
            // User not logged in, show login option
            loginBtn.setOnClickListener(v -> {
                handleGoogleLogin();
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void logoutUser() {
        sharedPreferences.edit()
                .remove("user_id")
                .remove("user_name")
                .remove("profile_pic_url")
                .apply();
        loadUserProfile();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void handleGoogleLogin() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential credential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            String id = credential.getId();
                            String displayName = credential.getDisplayName();
                            String email = credential.getId(); // Credential manager uses ID which is often email or unique ID
                            String profilePictureUri = credential.getProfilePictureUri() != null ? credential.getProfilePictureUri().toString() : null;

                            sharedPreferences.edit()
                                    .putString("user_id", id)
                                    .putString("user_name", displayName)
                                    .putString("user_email", email)
                                    .putString("profile_pic_url", profilePictureUri)
                                    .apply();

                            loadUserProfile();
                            Toast.makeText(MainActivity.this, "Welcome " + displayName, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("GoogleLogin", "Error parsing credential", e);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e("GoogleLogin", "Error getting credential", e);
                        Toast.makeText(MainActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            searchAdapter.setResults(new ArrayList<>());
            return;
        }

        List<SearchResult> combinedResults = new ArrayList<>();

        // Search Clients
        List<Client> clients = db.clientDao().searchClients(query);
        for (Client c : clients) {
            combinedResults.add(new SearchResult(c.getClientName(), c.getEmail(), "Client", null, c.getId()));
        }

        // Search Invoices
        List<Invoice> invoices = db.invoiceDao().searchInvoices(query);
        for (Invoice i : invoices) {
            combinedResults.add(new SearchResult(i.clientName, String.format(Locale.getDefault(), "%.2f €", i.amount), i.type, i.filePath, i.id));
        }

        searchAdapter.setResults(combinedResults);
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
                    bottomNavigationView.setSelectedItemId(R.id.clientsFragment);
                } else {
                    Bundle args = new Bundle();
                    args.putString("file_path", result.filePath);
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

    // Asking necessary permissions
    private void askPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_INTERNET);
    }

}