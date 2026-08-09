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
import com.chouchene.factures.entity.Booking;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.model.RecentActivity;
import com.chouchene.factures.utils.SwipeHistoryCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.content.Intent;

public class HomeFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private TextView txtGreeting, txtRevenue, txtRevenueDaily, txtInvoiceCount, txtBonCount, txtBookingCount, txtCurrentDate;
    private TextView txtOverdueAlert;
    private View badgeOverdue, cardOverdue;
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
        txtRevenueDaily = view.findViewById(R.id.txt_home_revenue_daily);
        txtInvoiceCount = view.findViewById(R.id.txt_home_invoice_count);
        txtBonCount = view.findViewById(R.id.txt_home_bon_count);
        txtBookingCount = view.findViewById(R.id.txt_home_booking_count);
        rvRecent = view.findViewById(R.id.rv_home_recent);
        badgeOverdue = view.findViewById(R.id.badge_overdue);
        cardOverdue = view.findViewById(R.id.card_overdue_alert);
        txtOverdueAlert = view.findViewById(R.id.txt_overdue_count);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        
        // Set dynamic date
        String dateStr = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new java.util.Date());
        txtCurrentDate.setText(dateStr);

        MaterialCardView cardDocuments = view.findViewById(R.id.card_documents);
        MaterialCardView cardClients = view.findViewById(R.id.card_clients);
        MaterialCardView cardAgenda = view.findViewById(R.id.card_agenda);
        MaterialCardView cardDashboard = view.findViewById(R.id.card_dashboard);
        MaterialCardView cardProfile = view.findViewById(R.id.card_profile);
        MaterialCardView cardSettings = view.findViewById(R.id.card_settings);

        BottomNavigationView navView = requireActivity().findViewById(R.id.bottomNavigationView);

        cardDocuments.setOnClickListener(v -> navView.setSelectedItemId(R.id.documentsHubFragment));
        cardClients.setOnClickListener(v -> navView.setSelectedItemId(R.id.clientsHubFragment));
        cardAgenda.setOnClickListener(v -> navView.setSelectedItemId(R.id.agendaFragment));
        cardDashboard.setOnClickListener(v -> navView.setSelectedItemId(R.id.parametresFragment));
        cardProfile.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.personalSettingsFragment));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.chouchene.factures.SettingsActivity.class)));

        view.findViewById(R.id.stat_invoices).setOnClickListener(v -> {
            Bundle b = new Bundle();
            b.putInt("start_tab", 0);
            Navigation.findNavController(v).navigate(R.id.documentsHubFragment, b);
        });
        view.findViewById(R.id.stat_orders).setOnClickListener(v -> {
            Bundle b = new Bundle();
            b.putInt("start_tab", 1);
            Navigation.findNavController(v).navigate(R.id.documentsHubFragment, b);
        });
        view.findViewById(R.id.stat_bookings).setOnClickListener(v -> navView.setSelectedItemId(R.id.agendaFragment));

        view.findViewById(R.id.btn_view_all_recent).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.globalHistoryFragment)
        );

        setupRecyclerView();
        loadHomeData(true);
    }

    private void setupNextTripCard(View view, Booking booking) {
        View card = view.findViewById(R.id.card_next_trip);
        if (booking == null) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        TextView txtTime = view.findViewById(R.id.txt_next_trip_time);
        TextView txtRoute = view.findViewById(R.id.txt_next_trip_route);
        
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        txtTime.setText(timeFmt.format(booking.dateTime) + " • " + booking.clientName);
        txtRoute.setText(booking.pickupLocation + " → " + booking.destinationLocation);

        card.setOnClickListener(v -> {
            Bundle b = new Bundle();
            b.putLong("selected_date", booking.dateTime.getTime());
            Navigation.findNavController(requireView()).navigate(R.id.agendaFragment, b);
        });
        
        view.findViewById(R.id.btn_view_trip).setOnClickListener(v -> card.performClick());
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setAdapter(adapter);

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
        }).attachToRecyclerView(rvRecent);
    }

    @Override
    public void onShareClick(RecentActivity activity) {
        if (activity.type == RecentActivity.Type.BOOKING) return;
        
        Invoice invoice = (Invoice) activity.originalObject;
        if (invoice.filePath == null) return;
        File file = new File(invoice.filePath);
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Partager le document"));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStatusChange(RecentActivity activity, String newStatus) {
        if ("Payée".equals(newStatus)) {
            if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
            }
        }
        updateStatus(activity, newStatus, null);
    }

    private void loadHomeData(boolean showShimmer) {
        if (showShimmer && shimmerContainer != null) {
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
            float income = db.invoiceDao().getMonthlyIncome(new java.util.Date());
            float dailyIncome = db.invoiceDao().getDailyIncome(new java.util.Date());
            float dailyExpenses = db.expenseDao().getDailyExpenses(new java.util.Date());
            float dailyProfit = dailyIncome - dailyExpenses;

            float expenses = db.expenseDao().getMonthlyExpenses(new java.util.Date());
            float profit = income - expenses;
            
            int invoiceCount = db.invoiceDao().getMonthlyInvoicesCount(new java.util.Date());
            int bonCount = db.invoiceDao().getMonthlyBonsCount(new java.util.Date());
            int bookingCount = db.bookingDao().getMonthlyBookingsCount(new java.util.Date());

            Booking nextBooking = db.bookingDao().getNextUpcomingBooking(new Date());

            List<Invoice> latestInvoices = db.invoiceDao().getLatestInvoices();
            List<Booking> activeBookings = db.bookingDao().getActiveUpcomingBookings(new java.util.Date());
            
            List<RecentActivity> allActivity = new ArrayList<>();
            for (Invoice i : latestInvoices) allActivity.add(new RecentActivity(i));
            for (Booking b : activeBookings) allActivity.add(new RecentActivity(b));
            
            // Sort by date descending
            Collections.sort(allActivity, (a1, a2) -> a2.date.compareTo(a1.date));
            
            // Keep only latest 5 if needed, or all
            if (allActivity.size() > 5) allActivity = allActivity.subList(0, 5);

            int overdueCount = db.invoiceDao().getOverdueInvoicesCount();

            List<RecentActivity> finalActivity = allActivity;
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    View v = getView();
                    if (v == null) return;

                    txtRevenue.setText(String.format(Locale.getDefault(), "%.2f €", profit));
                    txtRevenueDaily.setText(String.format(Locale.getDefault(), "Aujourd'hui: %.2f €", dailyProfit));
                    txtInvoiceCount.setText(String.valueOf(invoiceCount));
                    txtBonCount.setText(String.valueOf(bonCount));
                    txtBookingCount.setText(String.valueOf(bookingCount));
                    adapter.setData(finalActivity);
                    rvRecent.scheduleLayoutAnimation();

                    setupNextTripCard(v, nextBooking);
                    
                    if (badgeOverdue != null) {
                        badgeOverdue.setVisibility(overdueCount > 0 ? View.VISIBLE : View.GONE);
                    }

                    if (cardOverdue != null) {
                        if (overdueCount > 0) {
                            cardOverdue.setVisibility(View.VISIBLE);
                            txtOverdueAlert.setText(getString(R.string.label_overdue_alert, overdueCount));
                            cardOverdue.setOnClickListener(v_overdue -> {
                                Bundle b = new Bundle();
                                b.putInt("start_tab", 0);
                                // We could also set a status filter here if we wanted
                                Navigation.findNavController(v_overdue).navigate(R.id.documentsHubFragment, b);
                            });
                        } else {
                            cardOverdue.setVisibility(View.GONE);
                        }
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
    public void onItemClick(RecentActivity activity, View sharedElement) {
        if (activity.type == RecentActivity.Type.BOOKING) {
            Bundle b = new Bundle();
            b.putLong("selected_date", activity.date.getTime());
            Navigation.findNavController(requireView()).navigate(R.id.agendaFragment, b);
            return;
        }

        Invoice invoice = (Invoice) activity.originalObject;
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("transition_name", androidx.core.view.ViewCompat.getTransitionName(sharedElement));

        androidx.navigation.fragment.FragmentNavigator.Extras extras = new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedElement, androidx.core.view.ViewCompat.getTransitionName(sharedElement))
                .build();

        Executors.newSingleThreadExecutor().execute(() -> {
            com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(invoice.clientName);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (client != null) {
                        b.putString("mail_client", client.getEmail());
                    }
                    Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b, null, extras);
                });
            }
        });
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {
        String title = activity.type == RecentActivity.Type.BOOKING ? "Supprimer la course" : "Supprimer le document";
        String message = activity.type == RecentActivity.Type.BOOKING ? 
                "Voulez-vous vraiment supprimer cette course ?" : 
                "Voulez-vous vraiment supprimer ce document ? Cette action est irréversible.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (activity.type == RecentActivity.Type.BOOKING) {
                            db.bookingDao().deleteBooking((Booking) activity.originalObject);
                        } else {
                            Invoice invoice = (Invoice) activity.originalObject;
                            // Delete physical file
                            if (invoice.filePath != null) {
                                File file = new File(invoice.filePath);
                                if (file.exists()) file.delete();
                            }
                            db.invoiceDao().deleteInvoice(invoice);
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadHomeData(false);
                                com.google.android.material.snackbar.Snackbar.make(requireView(), "Supprimé avec succès", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }

    @Override
    public void onStatusClick(RecentActivity activity) {
        if (activity.type == RecentActivity.Type.BOOKING) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_selector, null);

        view.findViewById(R.id.status_pending).setOnClickListener(v -> updateStatus(activity, "En attente", dialog));
        view.findViewById(R.id.status_paid).setOnClickListener(v -> updateStatus(activity, "Payée", dialog));
        view.findViewById(R.id.status_cancelled).setOnClickListener(v -> updateStatus(activity, "Annulée", dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void updateStatus(RecentActivity activity, String status, BottomSheetDialog dialog) {
        if (activity.type == RecentActivity.Type.BOOKING) return;
        
        Invoice invoice = (Invoice) activity.originalObject;
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (dialog != null) dialog.dismiss();
                    if ("Payée".equals(status)) {
                        if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                            ((com.chouchene.factures.MainActivity) getActivity()).triggerConfetti();
                        }
                    }
                    loadHomeData(false);
                });
            }
        });
    }
}
