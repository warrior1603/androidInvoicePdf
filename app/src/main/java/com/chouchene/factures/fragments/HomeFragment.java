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
import androidx.core.content.ContextCompat;
import android.net.Uri;
import android.content.Intent;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.graphics.drawable.GradientDrawable;

public class HomeFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private TextView txtGreeting, txtRevenue, txtRevenueDaily, txtIncome, txtExpenses, txtInvoiceCount, txtBonCount, txtBookingCount, txtCurrentDate, txtBriefingLine, txtRevenueTrend;
    private boolean showingMonthly = true;
    private float monthlyProfitValue = 0f, dailyProfitValue = 0f;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressExpenseRatio;
    private View segmentInvoices, segmentOrders, segmentBookings;
    private TextView txtOverdueAlert;
    private View badgeOverdue, cardOverdue;
    private View badgeDocuments, badgeClients, badgeAgenda;
    private ImageView imgServiceDocs, imgServiceClients, imgServiceAgenda, imgServiceStats, imgServiceProfile, imgServiceSettings;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private androidx.core.widget.NestedScrollView scrollView;
    private LineChart activitySparkline;
    private View statInvoices, statOrders, statBookings;
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
        txtRevenueTrend = view.findViewById(R.id.txt_home_revenue_trend);
        txtIncome = view.findViewById(R.id.txt_home_income);
        txtExpenses = view.findViewById(R.id.txt_home_expenses);
        progressExpenseRatio = view.findViewById(R.id.progress_expense_ratio);
        txtInvoiceCount = view.findViewById(R.id.txt_home_invoice_count);
        txtBonCount = view.findViewById(R.id.txt_home_bon_count);
        txtBookingCount = view.findViewById(R.id.txt_home_booking_count);
        txtBriefingLine = view.findViewById(R.id.txt_briefing_line);
        segmentInvoices = view.findViewById(R.id.segment_invoices);
        segmentOrders = view.findViewById(R.id.segment_orders);
        segmentBookings = view.findViewById(R.id.segment_bookings);
        rvRecent = view.findViewById(R.id.rv_home_recent);
        badgeOverdue = view.findViewById(R.id.badge_documents);
        badgeDocuments = view.findViewById(R.id.badge_documents);
        badgeClients = view.findViewById(R.id.badge_clients);
        badgeAgenda = view.findViewById(R.id.badge_agenda);
        imgServiceDocs = view.findViewById(R.id.img_service_docs);
        imgServiceClients = view.findViewById(R.id.img_service_clients);
        imgServiceAgenda = view.findViewById(R.id.img_service_agenda);
        imgServiceStats = view.findViewById(R.id.img_service_stats);
        imgServiceProfile = view.findViewById(R.id.img_service_profile);
        imgServiceSettings = view.findViewById(R.id.img_service_settings);
        cardOverdue = view.findViewById(R.id.card_overdue_alert);
        txtOverdueAlert = view.findViewById(R.id.txt_overdue_count);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        scrollView = view.findViewById(R.id.home_scroll_view);
        activitySparkline = view.findViewById(R.id.activity_sparkline);
        setupActivitySparkline();
        statInvoices = view.findViewById(R.id.stat_invoices);
        statOrders = view.findViewById(R.id.stat_orders);
        statBookings = view.findViewById(R.id.stat_bookings);
        
        // Revenue Toggle Logic
        txtRevenue.setOnClickListener(v -> {
            showingMonthly = !showingMonthly;
            if (txtBriefingLine != null) {
                txtBriefingLine.setText(showingMonthly ? "RÉSUMÉ DU MOIS" : "RÉSUMÉ DU JOUR");
            }
            animateNumber(txtRevenue, showingMonthly ? monthlyProfitValue : dailyProfitValue);
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        });

        // Set dynamic date
        String dateStr = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new java.util.Date());
        txtCurrentDate.setText(dateStr);

        View cardDocuments = view.findViewById(R.id.card_documents);
        View cardClients = view.findViewById(R.id.card_clients);
        View cardAgenda = view.findViewById(R.id.card_agenda);
        View cardDashboard = view.findViewById(R.id.card_dashboard);
        View cardProfile = view.findViewById(R.id.card_profile);
        View cardSettings = view.findViewById(R.id.card_settings);
        View distributionContainer = view.findViewById(R.id.container_distribution_rail);

        MaterialCardView cardAvatar = view.findViewById(R.id.card_home_user_avatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.entrepriseSettingsFragment));
        }

        cardDocuments.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.documentsHubFragment);
        });
        cardClients.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.clientsHubFragment);
        });
        cardAgenda.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.agendaFragment);
        });
        cardDashboard.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.parametresFragment);
        });
        cardProfile.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.entrepriseSettingsFragment);
        });
        cardSettings.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Navigation.findNavController(v).navigate(R.id.settingsActivity);
        });
        
        if (distributionContainer != null) {
            distributionContainer.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.parametresFragment));
        }

        // Apply touch animations
        setupClickAnimations(
                cardDocuments, cardClients, cardAgenda, cardDashboard, cardProfile, cardSettings,
                view.findViewById(R.id.stat_invoices),
                view.findViewById(R.id.stat_orders),
                view.findViewById(R.id.stat_bookings),
                distributionContainer
        );

        // Entrance cascade for services
        android.view.ViewGroup servicesGrid = view.findViewById(R.id.services_grid);
        if (servicesGrid != null) {
            servicesGrid.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_liquid));
            servicesGrid.scheduleLayoutAnimation();
        }

        setupScrollParallax();
        
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
        view.findViewById(R.id.stat_bookings).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.agendaFragment));

        view.findViewById(R.id.btn_view_all_recent).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.globalHistoryFragment)
        );

        setupRecyclerView();
        loadHomeData(true);
        startPulseAnimations();
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

    private void setupScrollParallax() {
        if (scrollView == null) return;
        
        scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Parallax factor for sparkline
            if (activitySparkline != null) {
                activitySparkline.setTranslationY(scrollY * 0.15f);
            }
            
            // Subtle floating for stats cards
            float statsParallax = scrollY * 0.05f;
            if (statInvoices != null) statInvoices.setTranslationY(-statsParallax);
            if (statOrders != null) statOrders.setTranslationY(-statsParallax * 0.8f);
            if (statBookings != null) statBookings.setTranslationY(-statsParallax * 1.2f);
        });
    }

    private void pulseView(View view) {
        if (view == null) return;
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .withEndAction(() -> view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start())
            .start();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setAdapter(adapter);
        rvRecent.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_liquid));

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
    public void onShareClick(RecentActivity activity) {}

    @Override
    public void onEditClick(RecentActivity activity) {
        if (activity.type == RecentActivity.Type.BOOKING) {
            AddBookingBottomSheet sheet = AddBookingBottomSheet.newInstance(activity.id);
            sheet.setOnBookingAddedListener(() -> loadHomeData(false));
            sheet.show(getChildFragmentManager(), "edit_booking");
            return;
        }
        
        Invoice invoice = (Invoice) activity.originalObject;
        Intent intent = new Intent(requireContext(), com.chouchene.factures.DocumentStudioActivity.class);
        intent.putExtra(com.chouchene.factures.DocumentStudioActivity.EXTRA_MODE, com.chouchene.factures.DocumentStudioActivity.MODE_EDIT);
        intent.putExtra(com.chouchene.factures.DocumentStudioActivity.EXTRA_TYPE, invoice.type);
        intent.putExtra(com.chouchene.factures.DocumentStudioActivity.EXTRA_DOC_ID, invoice.id);
        startActivity(intent);
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
        Context context = getContext();
        if (context == null) return;

        if (showShimmer && shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            rvRecent.setVisibility(View.GONE);
        }

        SharedPreferences userPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String name = userPrefs.getString("User", "");
        if (!name.isEmpty()) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 5 && hour < 12) greeting = "Bonjour,";
            else if (hour >= 12 && hour < 18) greeting = "Bon après-midi,";
            else greeting = "Bonsoir,";

            txtGreeting.setText(greeting + " " + name);
            TextView txtInitials = getView().findViewById(R.id.txt_home_user_initials);
            if (txtInitials != null) {
                txtInitials.setText(com.chouchene.factures.utils.AvatarHelper.getInitials(name));
            }
            MaterialCardView cardAvatar = getView().findViewById(R.id.card_home_user_avatar);
            if (cardAvatar != null) {
                cardAvatar.setCardBackgroundColor(com.chouchene.factures.utils.AvatarHelper.getColorForName(name));
            }
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            float income = db.invoiceDao().getMonthlyIncome(new java.util.Date());
            float dailyIncome = db.invoiceDao().getDailyIncome(new java.util.Date());
            float dailyExpenses = db.expenseDao().getDailyExpenses(new java.util.Date());
            float dailyProfit = dailyIncome - dailyExpenses;

            float expenses = db.expenseDao().getMonthlyExpenses(new java.util.Date());
            float profit = income - expenses;
            this.monthlyProfitValue = profit;
            this.dailyProfitValue = dailyProfit;
            
            int invoiceCount = db.invoiceDao().getMonthlyInvoicesCount(new java.util.Date());
            int bonCount = db.invoiceDao().getMonthlyBonsCount(new java.util.Date());
            int bookingCount = db.bookingDao().getMonthlyBookingsCount(new java.util.Date());

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            Date start = cal.getTime();
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            Date end = cal.getTime();
            
            int todayBookingsCount = db.bookingDao().getBookingsBetweenDates(start, end).size();
            int overdueCount = db.invoiceDao().getOverdueInvoicesCount();
            int clientsCount = db.clientDao().getAllClients().size();

            Booking nextBooking = db.bookingDao().getNextUpcomingBooking(new Date());

            List<Invoice> latestInvoices = db.invoiceDao().getLatestInvoices();
            List<Booking> latestBookings = db.bookingDao().getLatestBookings();
            
            List<RecentActivity> allActivity = new ArrayList<>();
            for (Invoice i : latestInvoices) allActivity.add(new RecentActivity(i));
            for (Booking b : latestBookings) allActivity.add(new RecentActivity(b));
            
            Collections.sort(allActivity, (a1, a2) -> a2.date.compareTo(a1.date));
            if (allActivity.size() > 8) allActivity = allActivity.subList(0, 8);

            List<RecentActivity> finalActivity = allActivity;

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    View v = getView();
                    if (v == null) return;

                    animateNumber(txtRevenue, showingMonthly ? monthlyProfitValue : dailyProfitValue);
                    txtIncome.setText(String.format(Locale.getDefault(), "▲ %.2f €", income));
                    txtExpenses.setText(String.format(Locale.getDefault(), "▼ %.2f €", expenses));

                    if (income > 0) {
                        int ratio = (int) ((expenses / income) * 100);
                        progressExpenseRatio.setProgress(Math.min(ratio, 100), true);
                    } else {
                        progressExpenseRatio.setProgress(0, true);
                    }
                    txtInvoiceCount.setText(String.valueOf(invoiceCount));
                    txtBonCount.setText(String.valueOf(bonCount));
                    txtBookingCount.setText(String.valueOf(bookingCount));

                    // Dynamic Briefing based on context
                    if (txtBriefingLine != null) {
                        String briefing = showingMonthly ? "RÉSUMÉ DU MOIS" : "RÉSUMÉ DU JOUR";
                        if (todayBookingsCount > 0) briefing = todayBookingsCount + " COURSE" + (todayBookingsCount > 1 ? "S" : "") + " AUJOURD'HUI";
                        else if (overdueCount > 0) briefing = overdueCount + " FACTURE" + (overdueCount > 1 ? "S" : "") + " EN RETARD";
                        txtBriefingLine.setText(briefing);
                    }

                    if (invoiceCount > 0) pulseView(statInvoices);
                    if (bonCount > 0) pulseView(statOrders);
                    if (bookingCount > 0) pulseView(statBookings);

                    updateDistributionChart(invoiceCount, bonCount, bookingCount);

                    adapter.setData(finalActivity);
                    
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    
                    rvRecent.setVisibility(View.VISIBLE);
                    rvRecent.scheduleLayoutAnimation();

                    setupNextTripCard(v, nextBooking);
                    
                    if (badgeOverdue != null) {
                        badgeOverdue.setVisibility(overdueCount > 0 ? View.VISIBLE : View.GONE);
                    }

                    if (badgeDocuments != null) {
                        badgeDocuments.setVisibility(overdueCount > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (badgeAgenda != null) {
                        badgeAgenda.setVisibility(todayBookingsCount > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (badgeClients != null) {
                        badgeClients.setVisibility(clientsCount == 0 ? View.VISIBLE : View.GONE);
                    }

                    if (cardOverdue != null) {
                        if (overdueCount > 0) {
                            cardOverdue.setVisibility(View.VISIBLE);
                            txtOverdueAlert.setText(getString(R.string.label_overdue_alert, overdueCount));
                            cardOverdue.setOnClickListener(v_overdue -> {
                                Bundle b = new Bundle();
                                b.putInt("start_tab", 0);
                                Navigation.findNavController(v_overdue).navigate(R.id.documentsHubFragment, b);
                            });
                            // Subtle attention pulse for the alert
                            cardOverdue.postDelayed(() -> {
                                if (isAdded()) pulseView(cardOverdue);
                            }, 1000);
                        } else {
                            cardOverdue.setVisibility(View.GONE);
                        }
                    }

                    loadActivitySparklineData();
                });
            }
        });
    }

    private void setupActivitySparkline() {
        if (activitySparkline == null) return;

        activitySparkline.getDescription().setEnabled(false);
        activitySparkline.getLegend().setEnabled(false);
        activitySparkline.setTouchEnabled(false);
        activitySparkline.setDrawGridBackground(false);

        // Hide Axes
        activitySparkline.getXAxis().setEnabled(false);
        activitySparkline.getAxisLeft().setEnabled(false);
        activitySparkline.getAxisRight().setEnabled(false);

        // Standard offsets to keep it visible
        activitySparkline.getAxisLeft().setSpaceTop(20f); 
        activitySparkline.getAxisLeft().setSpaceBottom(20f);

        activitySparkline.setViewPortOffsets(0f, 10f, 0f, 10f);
    }

    private void loadActivitySparklineData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Entry> entries = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -9);

            for (int i = 0; i < 10; i++) {
                int count = db.invoiceDao().getDailyCount(cal.getTime());
                entries.add(new Entry(i, (float) count));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    LineDataSet dataSet = new LineDataSet(entries, "Activity");
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSet.setDrawCircles(false);
                    dataSet.setDrawValues(false);
                    dataSet.setLineWidth(2.5f);
                    
                    // Slightly more visible color
                    int color = ContextCompat.getColor(requireContext(), R.color.primary);
                    dataSet.setColor(android.graphics.Color.argb(120, android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color)));
                    dataSet.setDrawFilled(true);

                    // Slightly more visible gradient
                    GradientDrawable gradient = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{
                                    android.graphics.Color.argb(60, android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color)),
                                    android.graphics.Color.TRANSPARENT
                            }
                    );
                    dataSet.setFillDrawable(gradient);

                    LineData lineData = new LineData(dataSet);
                    activitySparkline.setData(lineData);
                    activitySparkline.animateY(1200);
                    activitySparkline.invalidate();
                });
            }
        });
    }

    private void updateDistributionChart(int invoices, int bons, int bookings) {
        int total = invoices + bons + bookings;
        
        setWeight(segmentInvoices, total > 0 ? (float) invoices / total : 1f);
        setWeight(segmentOrders, total > 0 ? (float) bons / total : 1f);
        setWeight(segmentBookings, total > 0 ? (float) bookings / total : 1f);
        
        // Hide if zero to make the rail look better
        if (segmentInvoices != null) segmentInvoices.setVisibility(invoices > 0 ? View.VISIBLE : View.GONE);
        if (segmentOrders != null) segmentOrders.setVisibility(bons > 0 ? View.VISIBLE : View.GONE);
        if (segmentBookings != null) segmentBookings.setVisibility(bookings > 0 ? View.VISIBLE : View.GONE);
    }

    private void setWeight(View view, float weight) {
        if (view == null) return;
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = Math.max(0.01f, weight); // Avoid 0 weight
        view.setLayoutParams(params);
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
        b.putString("client_name", invoice.clientName);
        b.putString("doc_type", activity.type.name());
        b.putInt("EXTRA_DOC_ID", invoice.id);
        String transitionName = androidx.core.view.ViewCompat.getTransitionName(sharedElement);
        b.putString("transition_name", transitionName);

        androidx.navigation.fragment.FragmentNavigator.Extras extras = new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedElement, transitionName != null ? transitionName : "")
                .build();

        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b, null, extras);
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {
        String title = activity.type == RecentActivity.Type.BOOKING ? "Supprimer la course" : "Supprimer le document";
        String message = activity.type == RecentActivity.Type.BOOKING ? 
                "Voulez-vous vraiment supprimer cette course ?" : 
                "Voulez-vous vraiment supprimer ce document ? Cette action est irréversible.";

        Context context = getContext();
        if (context == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (activity.type == RecentActivity.Type.BOOKING) {
                            db.bookingDao().deleteBooking((Booking) activity.originalObject);
                        } else {
                            Invoice invoice = (Invoice) activity.originalObject;
                            if (invoice.filePath != null) {
                                File file = new File(invoice.filePath);
                                if (file.exists()) file.delete();
                            }
                            db.invoiceDao().deleteInvoice(invoice);
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadHomeData(false);
                                View view = getView();
                                if (view != null) {
                                    com.google.android.material.snackbar.Snackbar.make(view, "Supprimé avec succès", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                                }
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

    @Override
    public void onResume() {
        super.onResume();
        loadHomeData(false);
    }

    private void animateNumber(TextView textView, float target) {
        textView.setAlpha(0f);
        textView.setTranslationY(20f);
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, target);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(String.format(Locale.getDefault(), "%.2f €", value));
            float progress = animation.getAnimatedFraction();
            textView.setAlpha(progress);
            textView.setTranslationY(20f * (1 - progress));
        });
        animator.start();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupClickAnimations(View... views) {
        for (View v : views) {
            v.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                            view.performClick();
                        }
                        break;
                }
                return true;
            });
        }
    }

    private void startPulseAnimations() {
        if (imgServiceDocs != null) pulseViewRepeating(imgServiceDocs, 0);
        if (imgServiceAgenda != null) pulseViewRepeating(imgServiceAgenda, 1000);
        if (imgServiceClients != null) pulseViewRepeating(imgServiceClients, 2000);
        if (imgServiceStats != null) pulseViewRepeating(imgServiceStats, 3000);
        if (imgServiceProfile != null) pulseViewRepeating(imgServiceProfile, 4000);
        if (imgServiceSettings != null) pulseViewRepeating(imgServiceSettings, 5000);

        // Pulsating LIVE badge logic
        View liveBadge = getView() != null ? getView().findViewById(R.id.badge_live_status) : null;
        if (liveBadge != null) {
            android.view.animation.AlphaAnimation blink = new android.view.animation.AlphaAnimation(1.0f, 0.3f);
            blink.setDuration(1200);
            blink.setRepeatMode(android.view.animation.Animation.REVERSE);
            blink.setRepeatCount(android.view.animation.Animation.INFINITE);
            liveBadge.startAnimation(blink);
        }
    }

    private void pulseViewRepeating(View view, long delay) {
        if (view == null) return;
        view.postDelayed(() -> {
            view.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(800)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (view.getContext() == null) return;
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(1200)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction(() -> pulseViewRepeating(view, 5000))
                        .start();
                })
                .start();
        }, delay);
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
