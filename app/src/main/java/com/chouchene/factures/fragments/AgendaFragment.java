package com.chouchene.factures.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.AgendaAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Booking;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class AgendaFragment extends Fragment implements AgendaAdapter.OnBookingActionListener {

    private RecyclerView rvBookings;
    private AgendaAdapter adapter;
    private AppDatabase db;
    private LinearLayout emptyState;
    private CalendarView calendarView;
    private Date selectedDate;

    public AgendaFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        
        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null && getArguments() != null && getArguments().containsKey("selected_date")) {
            toolbar.setNavigationIcon(R.drawable.rounded_history_24); // Show back icon if came from search
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        }

        selectedDate = new Date();
        if (getArguments() != null && getArguments().containsKey("selected_date")) {
            selectedDate = new Date(getArguments().getLong("selected_date"));
        }

        rvBookings = view.findViewById(R.id.rvBookings);
        emptyState = view.findViewById(R.id.emptyState);
        calendarView = view.findViewById(R.id.calendarView);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabAddBooking);
        
        calendarView.setDate(selectedDate.getTime());

        adapter = new AgendaAdapter(this);
        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBookings.setAdapter(adapter);

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate = cal.getTime();
            loadBookingsForDate(selectedDate);
        });

        fab.setOnClickListener(v -> {
            AddBookingBottomSheet bottomSheet = new AddBookingBottomSheet();
            bottomSheet.setOnBookingAddedListener(() -> loadBookingsForDate(selectedDate));
            bottomSheet.show(getChildFragmentManager(), "ADD_BOOKING");
        });

        loadBookingsForDate(selectedDate);
    }

    private void loadBookingsForDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date start = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date end = cal.getTime();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Booking> bookings = db.bookingDao().getBookingsBetweenDates(start, end);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setData(bookings);
                    emptyState.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    @Override
    public void onCallClient(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    @Override
    public void onBookingClick(Booking booking) {
        // Show details or edit
    }
}
