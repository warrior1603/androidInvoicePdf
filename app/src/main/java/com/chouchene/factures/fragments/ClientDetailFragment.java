package com.chouchene.factures.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.model.RecentActivity;
import com.chouchene.factures.utils.AvatarHelper;
import com.chouchene.factures.utils.SwipeHistoryCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.transition.MaterialContainerTransform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.core.content.FileProvider;

public class ClientDetailFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private Client client;
    private AppDatabase db;
    private HistoryAdapter adapter;
    private RecyclerView rvHistory;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(R.id.nav_host_fragment);
        transform.setDuration(450);
        transform.setScrimColor(Color.TRANSPARENT);
        setSharedElementEnterTransition(transform);
        
        db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();

        if (getArguments() != null) {
            int clientId = getArguments().getInt("client_id", -1);
            if (clientId != -1) {
                client = db.clientDao().getClientById(clientId);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_client_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postponeEnterTransition();

        if (client == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(client.getClientName());

        TextView txtInitials = view.findViewById(R.id.detail_initials);
        MaterialCardView avatarContainer = view.findViewById(R.id.avatar_container);
        
        if (txtInitials != null) {
            txtInitials.setText(AvatarHelper.getInitials(client.getClientName()));
        }
        if (avatarContainer != null) {
            avatarContainer.setCardBackgroundColor(AvatarHelper.getColorForName(client.getClientName()));
        }

        TextView txtAddress = view.findViewById(R.id.detail_address);
        TextView txtPhone = view.findViewById(R.id.detail_phone);
        TextView txtEmail = view.findViewById(R.id.detail_email);
        TextView txtSiren = view.findViewById(R.id.detail_siren);
        TextView txtTva = view.findViewById(R.id.detail_tva);
        
        String fullAddress = client.getStreet() + "\n" + client.getCodePostale() + " " + client.getVille() + ", " + client.getPays();
        txtAddress.setText(fullAddress);
        
        txtPhone.setText(client.phone != null ? client.phone : "N/A");
        txtEmail.setText(client.getEmail());
        
        txtSiren.setText("SIREN: " + (client.getNumeroSiren() != null ? client.getNumeroSiren() : "N/A"));
        txtTva.setText("TVA: " + (client.getNumeroTVA() != null ? client.getNumeroTVA() : "N/A"));

        view.findViewById(R.id.layout_call).setOnClickListener(v -> {
            if (client.phone != null && !client.phone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + client.phone)));
            }
        });

        view.findViewById(R.id.layout_email).setOnClickListener(v -> {
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + client.getEmail())));
            }
        });

        view.findViewById(R.id.fab_edit_client).setOnClickListener(v -> {
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(client);
            bottomSheet.setOnClientSavedListener(() -> {
                client = db.clientDao().getClientById(client.getId());
                onViewCreated(view, null); // Refresh
            });
            bottomSheet.show(getChildFragmentManager(), "EDIT_CLIENT");
        });

        view.findViewById(R.id.btn_create_invoice_for_client).setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = CreateInvoiceBottomSheet.newInstance(client.getId());
            bottomSheet.setOnInvoiceGeneratedListener(this::loadHistory);
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE_FOR_CLIENT");
        });

        rvHistory = view.findViewById(R.id.rv_client_history);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        setupRecyclerView();
        loadHistory();
        
        view.post(this::startPostponedEnterTransition);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        new ItemTouchHelper(new SwipeHistoryCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                RecentActivity activity = adapter.getActivityAt(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    onStatusChange(activity, "Payée");
                } else if (direction == ItemTouchHelper.LEFT) {
                    onShareClick(activity);
                }
            }
        }).attachToRecyclerView(rvHistory);
    }

    @Override
    public void onShareClick(RecentActivity activity) {
        Invoice invoice = (Invoice) activity.originalObject;
        if (invoice.filePath == null) return;
        File file = new File(invoice.filePath);
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Partager la facture"));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStatusChange(RecentActivity activity, String newStatus) {
        updateStatus(activity, newStatus, null);
    }

    private void loadHistory() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            rvHistory.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try { Thread.sleep(600); } catch (Exception ignored) {}
            List<Invoice> history = db.invoiceDao().getInvoicesByClient(client.getClientName());
            float totalRevenue = db.invoiceDao().getTotalRevenueByClient(client.getClientName());
            
            List<RecentActivity> activities = new ArrayList<>();
            for (Invoice i : history) activities.add(new RecentActivity(i));

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    adapter.setData(activities);
                    rvHistory.setVisibility(View.VISIBLE);
                    TextView txtTotalRevenue = getView().findViewById(R.id.detail_total_revenue);
                    if (txtTotalRevenue != null) {
                        txtTotalRevenue.setText(String.format(java.util.Locale.getDefault(), "%.2f €", totalRevenue));
                    }
                });
            }
        });
    }

    @Override
    public void onItemClick(RecentActivity activity, View sharedElement) {
        Invoice invoice = (Invoice) activity.originalObject;
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("mail_client", client.getEmail());
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {
        // Implementation can be added if deletion from here is desired
    }

    @Override
    public void onStatusClick(RecentActivity activity) {
        Invoice invoice = (Invoice) activity.originalObject;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_selector, null);

        com.google.android.material.chip.Chip chipPending = view.findViewById(R.id.status_pending);
        com.google.android.material.chip.Chip chipPaid = view.findViewById(R.id.status_paid);
        com.google.android.material.chip.Chip chipCancelled = view.findViewById(R.id.status_cancelled);

        if ("Payée".equals(invoice.status)) chipPaid.setChecked(true);
        else if ("Annulée".equals(invoice.status)) chipCancelled.setChecked(true);
        else chipPending.setChecked(true);

        chipPending.setOnClickListener(v -> updateStatus(activity, "En attente", dialog));
        chipPaid.setOnClickListener(v -> updateStatus(activity, "Payée", dialog));
        chipCancelled.setOnClickListener(v -> updateStatus(activity, "Annulée", dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void updateStatus(RecentActivity activity, String status, BottomSheetDialog dialog) {
        Invoice invoice = (Invoice) activity.originalObject;
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (dialog != null) dialog.dismiss();
                    loadHistory();
                });
            }
        });
    }
}
