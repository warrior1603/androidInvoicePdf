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
import com.chouchene.factures.model.RecentActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<RecentActivity> activities = new ArrayList<>();
    private final OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onItemClick(RecentActivity activity, View sharedElement);
        void onDeleteClick(RecentActivity activity);
        void onStatusClick(RecentActivity activity);
        void onShareClick(RecentActivity activity);
        void onStatusChange(RecentActivity activity, String newStatus);
    }

    public HistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<RecentActivity> data) {
        this.activities = data;
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
        RecentActivity activity = activities.get(position);
        holder.txtClientName.setText(activity.title);
        holder.txtAmount.setText(String.format(Locale.getDefault(), "%.2f €", activity.amount));
        
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtDate.setText(fmt.format(activity.date));

        int iconRes;
        int iconColor;
        
        switch (activity.type) {
            case BOOKING:
                iconRes = R.drawable.rounded_calendar_today_24;
                iconColor = R.color.icon_agenda;
                break;
            case ORDER:
                iconRes = R.drawable.rounded_shopping_cart_24;
                iconColor = R.color.icon_dashboard;
                break;
            case INVOICE:
            default:
                iconRes = R.drawable.rounded_receipt_long_24;
                iconColor = R.color.icon_documents;
                break;
        }

        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), iconColor)));

        androidx.core.view.ViewCompat.setTransitionName(holder.itemView, "activity_" + position);

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onItemClick(activity, holder.itemView);
        });
        
        String displayStatus = activity.status;
        if ("Scheduled".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_scheduled);
        else if ("Completed".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_completed);
        else if ("Cancelled".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_cancelled);
        
        holder.txtStatus.setText(displayStatus != null ? displayStatus : "En attente");
        int statusColor;
        int statusBg;
        
        if ("Payée".equals(activity.status) || "Completed".equals(activity.status) || "Terminée".equals(activity.status)) {
            statusBg = R.drawable.bg_status_paid;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid);
        } else if ("Annulée".equals(activity.status) || "Cancelled".equals(activity.status)) {
            statusBg = R.drawable.bg_status_cancelled;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled);
        } else {
            statusBg = R.drawable.bg_status_pending;
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
        }
        
        holder.txtStatus.setBackgroundResource(statusBg);
        holder.txtStatus.setTextColor(statusColor);

        holder.txtStatus.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            listener.onStatusClick(activity);
        });

        holder.txtStatus.setOnLongClickListener(v -> {
            if (activity.type != RecentActivity.Type.BOOKING && !"Payée".equals(activity.status)) {
                com.chouchene.factures.utils.AnimationUtils.popView(v);
                listener.onStatusChange(activity, "Payée");
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public RecentActivity getActivityAt(int position) {
        return activities.get(position);
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
