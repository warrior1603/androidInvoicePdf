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
import com.chouchene.factures.model.RecentActivity;
import com.chouchene.factures.utils.LottieUtils;
import com.chouchene.factures.utils.SwipeHistoryCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
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
                RecentActivity activity = adapter.getActivityAt(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    onStatusChange(activity, "Payée");
                } else if (direction == ItemTouchHelper.LEFT) {
                    onDeleteClick(activity);
                }
            }
        }).attachToRecyclerView(recyclerView);

        if (viewModel != null) {
            viewModel.getCurrentFilter().observe(getViewLifecycleOwner(), filter -> loadInvoices(filter, true));
        } else {
            loadInvoices(null, true);
        }

        fab.setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = new CreateInvoiceBottomSheet();
            bottomSheet.setOnInvoiceGeneratedListener(() -> {
                if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                    ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
                }
                if (viewModel != null) loadInvoices(viewModel.getCurrentFilter().getValue(), true);
                else loadInvoices(null, true);
            });
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE");
        });
    }

    private void loadInvoices(DocumentsViewModel.Filter filter, boolean showShimmer) {
        if (showShimmer && shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }

        final android.app.Activity fragmentActivity = getActivity();
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

            List<RecentActivity> finalActivities = new ArrayList<>();
            for (Invoice i : invoices) finalActivities.add(new RecentActivity(i));

            if (fragmentActivity != null) {
                fragmentActivity.runOnUiThread(() -> {
                    if (isAdded() && getView() != null) {
                        if (shimmerContainer != null) {
                            shimmerContainer.stopShimmer();
                            shimmerContainer.setVisibility(View.GONE);
                        }
                        adapter.setData(finalActivities);
                        recyclerView.scheduleLayoutAnimation();
                        checkEmptyState();
                    }
                });
            }
        });
    }

    private void loadInvoices() {
        if (viewModel != null) loadInvoices(viewModel.getCurrentFilter().getValue(), false);
        else loadInvoices(null, false);
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
    public void onItemClick(RecentActivity activity, View sharedElement) {
        Invoice invoice = (Invoice) activity.originalObject;
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("client_name", invoice.clientName);
        b.putString("doc_type", activity.type.name());
        String transitionName = androidx.core.view.ViewCompat.getTransitionName(sharedElement);
        b.putString("transition_name", transitionName);

        androidx.navigation.fragment.FragmentNavigator.Extras extras = new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedElement, transitionName != null ? transitionName : "")
                .build();

        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b, null, extras);
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {
        Invoice invoice = (Invoice) activity.originalObject;
        final android.app.Activity fragmentActivity = getActivity();
        final View fragmentView = getView();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer cette facture ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (invoice.filePath != null) {
                            File file = new File(invoice.filePath);
                            if (file.exists()) file.delete();
                        }
                        db.invoiceDao().deleteInvoice(invoice);
                        if (fragmentActivity instanceof com.chouchene.factures.MainActivity) {
                            ((com.chouchene.factures.MainActivity) fragmentActivity).updateBottomNavBadges();
                        }
                        if (fragmentActivity != null) {
                            fragmentActivity.runOnUiThread(() -> {
                                if (isAdded() && fragmentView != null) {
                                    loadInvoices();
                                    Snackbar snackbar = Snackbar.make(fragmentView, "Facture supprimée", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }

    @Override
    public void onStatusClick(RecentActivity activity) {
        Invoice invoice = (Invoice) activity.originalObject;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
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

    @Override
    public void onShareClick(RecentActivity activity) {
        Invoice invoice = (Invoice) activity.originalObject;
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
    public void onStatusChange(RecentActivity activity, String newStatus) {
        Invoice invoice = (Invoice) activity.originalObject;
        final android.app.Activity fragmentActivity = getActivity();
        final android.content.Context context = getContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = newStatus;
            db.invoiceDao().updateInvoice(invoice);
            if (fragmentActivity instanceof com.chouchene.factures.MainActivity) {
                ((com.chouchene.factures.MainActivity) fragmentActivity).updateBottomNavBadges();
            }
            if (fragmentActivity != null) {
                fragmentActivity.runOnUiThread(() -> {
                    if (isAdded() && getView() != null) {
                        if ("Payée".equals(newStatus)) {
                            if (fragmentActivity instanceof com.chouchene.factures.MainActivity) {
                                ((com.chouchene.factures.MainActivity) fragmentActivity).triggerConfetti();
                            }
                        }
                        loadInvoices();
                        if (context != null) {
                            android.widget.Toast.makeText(context, "Statut mis à jour: " + newStatus, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void updateStatus(RecentActivity activity, String status, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        Invoice invoice = (Invoice) activity.originalObject;
        final android.app.Activity fragmentActivity = getActivity();
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (fragmentActivity instanceof com.chouchene.factures.MainActivity) {
                ((com.chouchene.factures.MainActivity) fragmentActivity).updateBottomNavBadges();
            }
            if (fragmentActivity != null) {
                fragmentActivity.runOnUiThread(() -> {
                    if (isAdded() && getView() != null) {
                        if (dialog != null) dialog.dismiss();
                        if ("Payée".equals(status)) {
                            if (fragmentActivity instanceof com.chouchene.factures.MainActivity) {
                                ((com.chouchene.factures.MainActivity) fragmentActivity).triggerConfetti();
                            }
                        }
                        loadInvoices();
                    }
                });
            }
        });
    }
}
