package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class GlobalHistoryFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private LinearLayout emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private List<RecentActivity> allActivities = new ArrayList<>();

    public GlobalHistoryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_global_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();

        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.rounded_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        }

        recyclerView = view.findViewById(R.id.rv_global_history);
        emptyState = view.findViewById(R.id.empty_state);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        ChipGroup chipGroup = view.findViewById(R.id.filter_chip_group);

        adapter = new HistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            filterActivities(checkedIds.get(0));
        });

        loadData();
    }

    private void loadData() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try { Thread.sleep(800); } catch (Exception ignored) {}
            List<Invoice> invoices = db.invoiceDao().getAllInvoices();
            List<Booking> bookings = db.bookingDao().getBookingsBetweenDates(new java.util.Date(0), new java.util.Date(Long.MAX_VALUE));
            
            allActivities.clear();
            for (Invoice i : invoices) allActivities.add(new RecentActivity(i));
            for (Booking b : bookings) allActivities.add(new RecentActivity(b));

            Collections.sort(allActivities, (a1, a2) -> a2.date.compareTo(a1.date));

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    adapter.setData(allActivities);
                    recyclerView.setVisibility(allActivities.isEmpty() ? View.GONE : View.VISIBLE);
                    checkEmptyState();
                });
            }
        });
    }

    private void filterActivities(int chipId) {
        List<RecentActivity> filtered = new ArrayList<>();
        RecentActivity.Type targetType = null;

        if (chipId == R.id.chip_invoices) targetType = RecentActivity.Type.INVOICE;
        else if (chipId == R.id.chip_orders) targetType = RecentActivity.Type.ORDER;
        else if (chipId == R.id.chip_bookings) targetType = RecentActivity.Type.BOOKING;

        if (targetType == null) {
            filtered.addAll(allActivities);
        } else {
            for (RecentActivity a : allActivities) {
                if (a.type == targetType) filtered.add(a);
            }
        }
        adapter.setData(filtered);
        checkEmptyState();
    }

    private void checkEmptyState() {
        emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {}

    @Override
    public void onStatusClick(RecentActivity activity) {}

    @Override
    public void onShareClick(RecentActivity activity) {}

    @Override
    public void onStatusChange(RecentActivity activity, String newStatus) {}
}
