package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.LottieUtils;
import com.chouchene.factures.utils.SwipeHistoryCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class InvoiceGenrationFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private AppDatabase db;
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private DocumentsViewModel viewModel;

    public InvoiceGenrationFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DocumentsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.empty_state);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setText("Ajouter Facture");

        adapter = new HistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new SwipeHistoryCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                Invoice invoice = adapter.getInvoiceAt(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    onStatusChange(invoice, "Payée");
                } else if (direction == ItemTouchHelper.LEFT) {
                    onShareClick(invoice);
                    adapter.notifyItemChanged(position);
                }
            }
        }).attachToRecyclerView(recyclerView);

        if (viewModel != null) {
            viewModel.getCurrentFilter().observe(getViewLifecycleOwner(), this::loadInvoices);
        } else {
            loadInvoices(null);
        }

        fab.setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = new CreateInvoiceBottomSheet();
            bottomSheet.setOnInvoiceGeneratedListener(() -> {
                if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                    ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
                }
                if (viewModel != null) loadInvoices(viewModel.getCurrentFilter().getValue());
                else loadInvoices(null);
            });
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE");
        });
    }

    private void loadInvoices(DocumentsViewModel.Filter filter) {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {}

            List<Invoice> invoices;
            if (filter == null) {
                invoices = db.invoiceDao().getInvoicesOnly();
            } else {
                if (filter.type == null && filter.status != null) {
                    invoices = db.invoiceDao().getDocumentsByStatus("Facture", filter.status);
                } else if (filter.type != null && filter.status == null) {
                    switch (filter.type) {
                        case "MONTH": invoices = db.invoiceDao().getDocumentsByMonth("Facture", filter.value); break;
                        case "YEAR": invoices = db.invoiceDao().getDocumentsByYear("Facture", filter.value); break;
                        default: invoices = db.invoiceDao().getInvoicesOnly(); break;
                    }
                } else if (filter.type != null && filter.status != null) {
                    switch (filter.type) {
                        case "MONTH": invoices = db.invoiceDao().getDocumentsByMonthAndStatus("Facture", filter.value, filter.status); break;
                        case "YEAR": invoices = db.invoiceDao().getDocumentsByYearAndStatus("Facture", filter.value, filter.status); break;
                        default: invoices = db.invoiceDao().getDocumentsByStatus("Facture", filter.status); break;
                    }
                } else {
                    invoices = db.invoiceDao().getInvoicesOnly();
                }
            }
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    adapter.setData(invoices);
                    checkEmptyState();
                });
            }
        });
    }

    private void loadInvoices() {
        if (viewModel != null) loadInvoices(viewModel.getCurrentFilter().getValue());
        else loadInvoices(null);
    }

    private void checkEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            com.airbnb.lottie.LottieAnimationView lottie = emptyState.findViewById(R.id.lottie_empty_state);
            android.widget.ImageView staticImg = emptyState.findViewById(R.id.img_empty_state);
            LottieUtils.loadLottieWithFallback(lottie, staticImg, "anim_empty_invoices.json");
        }
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer cette facture ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.invoiceDao().deleteInvoice(invoice);
                        loadInvoices();
                    });
                })
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }

    @Override
    public void onStatusClick(Invoice invoice) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_selector, null);

        com.google.android.material.chip.Chip chipPending = view.findViewById(R.id.status_pending);
        com.google.android.material.chip.Chip chipPaid = view.findViewById(R.id.status_paid);
        com.google.android.material.chip.Chip chipCancelled = view.findViewById(R.id.status_cancelled);

        if ("Payée".equals(invoice.status)) chipPaid.setChecked(true);
        else if ("Annulée".equals(invoice.status)) chipCancelled.setChecked(true);
        else chipPending.setChecked(true);

        chipPending.setOnClickListener(v -> updateStatus(invoice, "En attente", dialog));
        chipPaid.setOnClickListener(v -> updateStatus(invoice, "Payée", dialog));
        chipCancelled.setOnClickListener(v -> updateStatus(invoice, "Annulée", dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public void onShareClick(Invoice invoice) {
        if (invoice.filePath == null) return;
        File file = new File(invoice.filePath);
        if (!file.exists()) return;

        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                requireContext().getPackageName() + ".provider", file);
        
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(android.content.Intent.createChooser(intent, "Partager la facture"));
    }

    @Override
    public void onStatusChange(Invoice invoice, String newStatus) {
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = newStatus;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    loadInvoices();
                    if ("Payée".equals(newStatus) && getActivity() instanceof com.chouchene.factures.MainActivity) {
                        ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
                    }
                    android.widget.Toast.makeText(requireContext(), "Statut mis à jour: " + newStatus, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateStatus(Invoice invoice, String status, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    loadInvoices();
                    if ("Payée".equals(status) && getActivity() instanceof com.chouchene.factures.MainActivity) {
                        ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
                    }
                });
            }
        });
    }
}
