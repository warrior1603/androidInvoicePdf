package com.chouchene.factures.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.AgendaAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Booking;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class AgendaFragment extends Fragment implements AgendaAdapter.OnBookingActionListener {

    private RecyclerView rvBookings;
    private AgendaAdapter adapter;
    private AppDatabase db;
    private LinearLayout emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private TabLayout tabLayout;
    private ChipGroup statusChipGroup;
    private Date selectedDate;
    private boolean isMonthlyView = false;
    private String currentStatusFilter = null;

    public AgendaFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        
        selectedDate = new Date();
        if (getArguments() != null && getArguments().containsKey("selected_date")) {
            selectedDate = new Date(getArguments().getLong("selected_date"));
        }

        rvBookings = view.findViewById(R.id.rvBookings);
        emptyState = view.findViewById(R.id.emptyState);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        tabLayout = view.findViewById(R.id.tabLayout);
        statusChipGroup = view.findViewById(R.id.statusChipGroup);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabAddBooking);
        
        adapter = new AgendaAdapter(this);
        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBookings.setAdapter(adapter);

        setupTabs();
        setupChips();

        fab.setOnClickListener(v -> {
            AddBookingBottomSheet bottomSheet = AddBookingBottomSheet.newInstance(null);
            bottomSheet.setOnBookingAddedListener(this::loadBookings);
            bottomSheet.show(getChildFragmentManager(), "ADD_BOOKING");
        });

        loadBookings();
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Jour").setIcon(R.drawable.ic_tab_day_outline));
        tabLayout.addTab(tabLayout.newTab().setText("Mois").setIcon(R.drawable.ic_tab_month_outline));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isMonthlyView = tab.getPosition() == 1;
                loadBookings();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupChips() {
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentStatusFilter = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipScheduled) currentStatusFilter = "Scheduled";
                else if (id == R.id.chipCompleted) currentStatusFilter = "Completed";
                else currentStatusFilter = null;
            }
            loadBookings();
        });
    }

    private void loadBookings() {
        if (isMonthlyView) {
            loadBookingsForMonth(selectedDate);
        } else {
            loadBookingsForDate(selectedDate);
        }
    }

    private void loadBookingsForDate(Date date) {
        showLoading();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date start = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        Date end = cal.getTime();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Booking> bookings = db.bookingDao().getBookingsBetweenDates(start, end);
            filterAndDisplay(bookings, false);
        });
    }

    private void loadBookingsForMonth(Date date) {
        showLoading();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date start = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        Date end = cal.getTime();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Booking> bookings = db.bookingDao().getBookingsBetweenDates(start, end);
            filterAndDisplay(bookings, true);
        });
    }

    private void filterAndDisplay(List<Booking> bookings, boolean isMonth) {
        List<Booking> filtered = new ArrayList<>();
        Date now = new Date();

        for (Booking b : bookings) {
            String effectiveStatus;
            if ("Cancelled".equals(b.status)) {
                effectiveStatus = "Cancelled";
            } else if (b.dateTime.before(now)) {
                effectiveStatus = "Completed";
            } else {
                effectiveStatus = "Scheduled";
            }

            if (currentStatusFilter == null || currentStatusFilter.equals(effectiveStatus)) {
                filtered.add(b);
            }
        }

        final int totalCount = bookings.size();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter.setData(filtered, isMonthlyView, selectedDate);
                if (isMonth) adapter.updateMonthStats(totalCount);
                
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
                rvBookings.setVisibility(View.VISIBLE);
                rvBookings.scheduleLayoutAnimation();
                emptyState.setVisibility(filtered.isEmpty() && !isMonthlyView ? View.VISIBLE : View.GONE);
            });
        }
    }

    private void showLoading() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            rvBookings.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCallClient(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    @Override
    public void onBookingClick(Booking booking) {
        AddBookingBottomSheet bottomSheet = AddBookingBottomSheet.newInstance(booking.id);
        bottomSheet.setOnBookingAddedListener(this::loadBookings);
        bottomSheet.show(getChildFragmentManager(), "EDIT_BOOKING");
    }

    @Override
    public void onDateChanged(Date date) {
        this.selectedDate = date;
        loadBookings();
    }
}
