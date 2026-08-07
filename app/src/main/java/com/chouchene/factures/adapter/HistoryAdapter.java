package com.chouchene.factures.adapter;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Invoice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Invoice> invoices = new ArrayList<>();
    private final OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onItemClick(Invoice invoice, View sharedElement);
        void onDeleteClick(Invoice invoice);
        void onStatusClick(Invoice invoice);
        void onShareClick(Invoice invoice);
        void onStatusChange(Invoice invoice, String newStatus);
    }

    public HistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Invoice> data) {
        this.invoices = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);
        holder.txtClientName.setText(invoice.clientName);
        holder.txtAmount.setText(String.format(Locale.getDefault(), "%.2f €", invoice.amount));
        
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtDate.setText(fmt.format(invoice.date));

        if ("Facture".equals(invoice.type)) {
            holder.icon.setImageResource(R.drawable.rounded_receipt_long_24);
            holder.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.icon_documents)));
        } else {
            holder.icon.setImageResource(R.drawable.rounded_shopping_cart_24);
            holder.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.icon_dashboard)));
        }

        androidx.core.view.ViewCompat.setTransitionName(holder.itemView, "invoice_" + invoice.id);

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onItemClick(invoice, holder.itemView);
        });
        
        holder.txtStatus.setText(invoice.status != null ? invoice.status : "En attente");
        int statusColor;
        int statusBg;
        
        if ("Payée".equals(invoice.status)) {
            statusBg = R.drawable.bg_status_paid;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid);
        } else if ("Annulée".equals(invoice.status)) {
            statusBg = R.drawable.bg_status_cancelled;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled);
        } else {
            statusBg = R.drawable.bg_status_pending;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
        }
        
        holder.txtStatus.setBackgroundResource(statusBg);
        holder.txtStatus.setTextColor(statusColor);

        holder.txtStatus.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onStatusClick(invoice);
        });

        holder.txtStatus.setOnLongClickListener(v -> {
            if (!"Payée".equals(invoice.status)) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                listener.onStatusChange(invoice, "Payée");
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    public Invoice getInvoiceAt(int position) {
        return invoices.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtClientName, txtDate, txtAmount, txtStatus;
        ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}
