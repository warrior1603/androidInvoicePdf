package com.chouchene.factures.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Booking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgendaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER_CALENDAR = 0;
    private static final int TYPE_HEADER_STATS = 1;
    private static final int TYPE_ITEM = 2;

    private List<Booking> bookings = new ArrayList<>();
    private final OnBookingActionListener listener;
    private boolean isMonthlyView = false;
    private Date selectedDate = new Date();
    private String monthStatsText = "0 courses";

    public interface OnBookingActionListener {
        void onCallClient(String phone);
        void onBookingClick(Booking booking);
        void onDateChanged(Date date);
    }

    public AgendaAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Booking> data, boolean isMonthlyView, Date selectedDate) {
        this.bookings = data;
        this.isMonthlyView = isMonthlyView;
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    public void updateMonthStats(int count) {
        this.monthStatsText = count + " courses";
        if (isMonthlyView) notifyItemChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return isMonthlyView ? TYPE_HEADER_STATS : TYPE_HEADER_CALENDAR;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER_CALENDAR) {
            return new CalendarHeaderViewHolder(inflater.inflate(R.layout.header_agenda_calendar, parent, false));
        } else if (viewType == TYPE_HEADER_STATS) {
            return new StatsHeaderViewHolder(inflater.inflate(R.layout.header_agenda_stats, parent, false));
        }
        return new ItemViewHolder(inflater.inflate(R.layout.item_booking, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CalendarHeaderViewHolder) {
            CalendarHeaderViewHolder h = (CalendarHeaderViewHolder) holder;
            h.calendarView.setDate(selectedDate.getTime(), false, true);
            h.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 0, 0, 0);
                listener.onDateChanged(cal.getTime());
            });
        } else if (holder instanceof StatsHeaderViewHolder) {
            StatsHeaderViewHolder h = (StatsHeaderViewHolder) holder;
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            h.txtMonthTitle.setText(sdf.format(selectedDate).toUpperCase());
            h.txtMonthStats.setText(monthStatsText);
        } else if (holder instanceof ItemViewHolder) {
            Booking booking = bookings.get(position - 1);
            ItemViewHolder h = (ItemViewHolder) holder;
            
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
            
            h.txtTime.setText(timeFmt.format(booking.dateTime));
            h.txtDate.setText(dateFmt.format(booking.dateTime));
            h.txtClientName.setText(booking.clientName);
            h.txtRoute.setText(booking.pickupLocation + " → " + booking.destinationLocation);
            h.txtAmount.setText(String.format(Locale.getDefault(), "%.2f €", booking.estimatedPrice));
            
            setupStatusBadge(h, booking);

            h.btnCall.setOnClickListener(v -> {
                if (booking.clientPhone != null && !booking.clientPhone.isEmpty()) {
                    listener.onCallClient(booking.clientPhone);
                }
            });

            h.itemView.setOnClickListener(v -> listener.onBookingClick(booking));
        }
    }

    private void setupStatusBadge(ItemViewHolder holder, Booking booking) {
        String status = booking.status;
        Date now = new Date();
        
        int bgRes;
        int textColor;
        String label;

        if ("Cancelled".equals(status)) {
            bgRes = R.drawable.bg_status_cancelled;
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled);
            label = "Annulée";
        } else if (booking.dateTime.before(now)) {
            bgRes = R.drawable.bg_status_paid;
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid);
            label = "Terminée";
        } else {
            bgRes = R.drawable.bg_status_pending;
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
            label = "À venir";
        }

        holder.txtStatus.setText(label);
        holder.txtStatus.setBackgroundResource(bgRes);
        holder.txtStatus.setTextColor(textColor);

        // Add Icon to Badge
        int iconRes = "Completed".equals(status) ? R.drawable.ic_status_check :
                     "Cancelled".equals(status) ? R.drawable.ic_status_x : R.drawable.ic_status_clock;
        
        holder.txtStatus.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        holder.txtStatus.setCompoundDrawablePadding(8);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(holder.txtStatus, android.content.res.ColorStateList.valueOf(textColor));
    }

    @Override
    public int getItemCount() {
        return bookings.size() + 1;
    }

    static class CalendarHeaderViewHolder extends RecyclerView.ViewHolder {
        CalendarView calendarView;
        CalendarHeaderViewHolder(View v) {
            super(v);
            calendarView = v.findViewById(R.id.calendarView);
        }
    }

    static class StatsHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtMonthTitle, txtMonthStats;
        StatsHeaderViewHolder(View v) {
            super(v);
            txtMonthTitle = v.findViewById(R.id.txtMonthTitle);
            txtMonthStats = v.findViewById(R.id.txtMonthStats);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime, txtDate, txtClientName, txtRoute, txtAmount, txtStatus;
        ImageButton btnCall;
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnCall = itemView.findViewById(R.id.btnCall);
        }
    }
}
