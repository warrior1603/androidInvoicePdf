package com.chouchene.factures.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Booking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.ViewHolder> {

    private List<Booking> bookings = new ArrayList<>();
    private final OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onCallClient(String phone);
        void onBookingClick(Booking booking);
    }

    public AgendaAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Booking> data) {
        this.bookings = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        
        holder.txtTime.setText(timeFmt.format(booking.dateTime));
        holder.txtDate.setText(dateFmt.format(booking.dateTime));
        holder.txtClientName.setText(booking.clientName);
        holder.txtPickup.setText(booking.pickupLocation);
        holder.txtDestination.setText(booking.destinationLocation);
        holder.txtAmount.setText(String.format(Locale.getDefault(), "%.2f €", booking.estimatedPrice));
        
        holder.btnCall.setOnClickListener(v -> {
            if (booking.clientPhone != null && !booking.clientPhone.isEmpty()) {
                listener.onCallClient(booking.clientPhone);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onBookingClick(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime, txtDate, txtClientName, txtPickup, txtDestination, txtAmount;
        ImageButton btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtPickup = itemView.findViewById(R.id.txtPickup);
            txtDestination = itemView.findViewById(R.id.txtDestination);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            btnCall = itemView.findViewById(R.id.btnCall);
        }
    }
}
