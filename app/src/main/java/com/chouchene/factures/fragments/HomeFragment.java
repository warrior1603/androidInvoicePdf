package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.SwipeHistoryCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.content.Intent;

public class HomeFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private TextView txtGreeting, txtRevenue, txtDocCount, txtCurrentDate;
    private View badgeOverdue;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private RecyclerView rvRecent;
    private HistoryAdapter adapter;
    private AppDatabase db;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();

        txtGreeting = view.findViewById(R.id.txt_greeting);
        txtCurrentDate = view.findViewById(R.id.txt_current_date);
        txtRevenue = view.findViewById(R.id.txt_home_revenue);
        txtDocCount = view.findViewById(R.id.txt_home_doc_count);
        rvRecent = view.findViewById(R.id.rv_home_recent);
        badgeOverdue = view.findViewById(R.id.badge_overdue);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        
        // Set dynamic date
        String dateStr = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new java.util.Date());
        txtCurrentDate.setText(dateStr);

        MaterialCardView cardDocuments = view.findViewById(R.id.card_documents);
        MaterialCardView cardClients = view.findViewById(R.id.card_clients);
        MaterialCardView cardDashboard = view.findViewById(R.id.card_dashboard);
        MaterialCardView cardProfile = view.findViewById(R.id.card_profile);

        BottomNavigationView navView = requireActivity().findViewById(R.id.bottomNavigationView);

        cardDocuments.setOnClickListener(v -> navView.setSelectedItemId(R.id.documentsHubFragment));
        cardClients.setOnClickListener(v -> navView.setSelectedItemId(R.id.clientsFragment));
        cardDashboard.setOnClickListener(v -> navView.setSelectedItemId(R.id.parametresFragment));
        cardProfile.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.personalSettingsFragment));

        view.findViewById(R.id.btn_view_all_recent).setOnClickListener(v -> navView.setSelectedItemId(R.id.documentsHubFragment));

        setupRecyclerView();
        loadHomeData();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setAdapter(adapter);

        new ItemTouchHelper(new SwipeHistoryCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                Invoice invoice = adapter.getInvoiceAt(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    onStatusChange(invoice, "Payée");
                } else if (direction == ItemTouchHelper.LEFT) {
                    onShareClick(invoice);
                }
            }
        }).attachToRecyclerView(rvRecent);
    }

    @Override
    public void onShareClick(Invoice invoice) {
        if (invoice.filePath == null) return;
        File file = new File(invoice.filePath);
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Partager la facture"));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStatusChange(Invoice invoice, String newStatus) {
        updateStatus(invoice, newStatus, null);
    }

    private void loadHomeData() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            rvRecent.setVisibility(View.GONE);
        }

        SharedPreferences userPrefs = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String name = userPrefs.getString("User", "");
        if (!name.isEmpty()) {
            txtGreeting.setText("Bonjour, " + name + " !");
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            float revenue = db.invoiceDao().getMonthlyIncome(new java.util.Date());
            int count = db.invoiceDao().getMonthlyCount(new java.util.Date());
            List<Invoice> latest = db.invoiceDao().getLatestInvoices();
            int overdueCount = db.invoiceDao().getOverdueInvoicesCount();

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    txtRevenue.setText(String.format(Locale.getDefault(), "%.2f €", revenue));
                    txtDocCount.setText(String.valueOf(count));
                    adapter.setData(latest);
                    
                    if (badgeOverdue != null) {
                        badgeOverdue.setVisibility(overdueCount > 0 ? View.VISIBLE : View.GONE);
                    }

                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                        rvRecent.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    public void onItemClick(Invoice invoice) {
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        Executors.newSingleThreadExecutor().execute(() -> {
            com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(invoice.clientName);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (client != null) {
                        b.putString("mail_client", client.getEmail());
                    }
                    Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
                });
            }
        });
    }

    @Override
    public void onDeleteClick(Invoice invoice) {
        // Not used on home screen
    }

    @Override
    public void onStatusClick(Invoice invoice) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_selector, null);

        view.findViewById(R.id.status_pending).setOnClickListener(v -> updateStatus(invoice, "En attente", dialog));
        view.findViewById(R.id.status_paid).setOnClickListener(v -> updateStatus(invoice, "Payée", dialog));
        view.findViewById(R.id.status_cancelled).setOnClickListener(v -> updateStatus(invoice, "Annulée", dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void updateStatus(Invoice invoice, String status, BottomSheetDialog dialog) {
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (dialog != null) dialog.dismiss();
                    loadHomeData();
                });
            }
        });
    }
}
