package com.chouchene.factures.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

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

public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.ItemViewHolder> {

    private List<Booking> bookings = new ArrayList<>();
    private final OnBookingActionListener listener;
    private Date selectedDate = new Date();

    public interface OnBookingActionListener {
        void onCallClient(String phone);
        void onOpenGps(String address);
        void onBookingClick(Booking booking, View sharedElement);
        void onDateChanged(Date date);
    }

    public AgendaAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Booking> data, boolean isMonthlyView, Date selectedDate) {
        this.bookings = data;
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder h, int position) {
        Booking booking = bookings.get(position);
        
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        
        ((TextView) h.txtTime).setText(timeFmt.format(booking.dateTime));
        ((TextView) h.txtDate).setText(dateFmt.format(booking.dateTime));
        ((TextView) h.txtClientName).setText(booking.clientName);
        ((TextView) h.txtRoute).setText(booking.pickupLocation + " → " + booking.destinationLocation);
        ((TextView) h.txtAmount).setText(String.format(Locale.getDefault(), "%.2f €", booking.estimatedPrice));
        
        setupStatusBadge(h, booking);

        // Unique transition name for booking
        androidx.core.view.ViewCompat.setTransitionName(h.itemView, "booking_container_" + booking.id);

        h.itemView.setOnClickListener(view -> listener.onBookingClick(booking, h.itemView));

        View btnGps = h.itemView.findViewById(R.id.btn_list_gps);
        if (btnGps != null) {
            btnGps.setVisibility(!"Cancelled".equals(booking.status) ? View.VISIBLE : View.GONE);
            btnGps.setOnClickListener(v -> listener.onOpenGps(booking.destinationLocation));
        }

        com.chouchene.factures.utils.UIUtils.applyClickScale(h.itemView);
        if (btnGps != null) com.chouchene.factures.utils.UIUtils.applyClickScale(btnGps);
        
        // Highlight logic for current/next booking
        Date now = new Date();
        boolean isActive = !booking.status.equals("Cancelled") && 
                          booking.dateTime.after(new Date(now.getTime() - 1800000)) && // Within last 30 mins
                          booking.dateTime.before(new Date(now.getTime() + 1800000)); // or next 30 mins
        
        if (h.nodeDot != null) h.nodeDot.setAlpha(isActive ? 1.0f : 0.6f);
        h.itemView.setBackgroundColor(isActive ? ContextCompat.getColor(h.itemView.getContext(), R.color.icon_agenda_bg) : android.graphics.Color.TRANSPARENT);
    }

    private void setupStatusBadge(ItemViewHolder holder, Booking booking) {
        String status = booking.status;
        Date now = new Date();
        
        int tintColor;
        String label;

        if ("Cancelled".equals(status)) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled);
            label = "ANNULÉE";
        } else if (booking.dateTime.before(now)) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid);
            label = "TERMINÉE";
        } else {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
            label = "À VENIR";
        }

        TextView txtStatus = (TextView) holder.txtStatus;
        txtStatus.setText(label);
        txtStatus.setBackground(null);
        txtStatus.setTextColor(tintColor);

        // Add Icon Dot
        int iconRes = "Cancelled".equals(status) ? R.drawable.ic_status_x :
                     booking.dateTime.before(now) ? R.drawable.ic_status_check : R.drawable.ic_status_clock;
        
        txtStatus.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        txtStatus.setCompoundDrawablePadding(12);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(txtStatus, android.content.res.ColorStateList.valueOf(tintColor));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public View txtTime, txtDate, txtClientName, txtRoute, txtAmount, txtStatus, nodeDot;
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            nodeDot = itemView.findViewById(R.id.nodeDot);
        }
    }
}
