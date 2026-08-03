package com.chouchene.factures.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Invoice> invoices = new ArrayList<>();
    private final OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onItemClick(Invoice invoice);
        void onDeleteClick(Invoice invoice);
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
            holder.icon.setImageResource(R.drawable.baseline_receipt_long_24);
        } else {
            holder.icon.setImageResource(R.drawable.baseline_shopping_cart_24);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(invoice));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(invoice));
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtClientName, txtDate, txtAmount;
        ImageView icon;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            icon = itemView.findViewById(R.id.icon);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
