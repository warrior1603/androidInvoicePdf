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
        void onEditClick(RecentActivity activity);
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
        int iconBgColor;
        
        switch (activity.type) {
            case BOOKING:
                iconRes = R.drawable.ic_calendar_event_outline;
                iconColor = R.color.icon_agenda;
                iconBgColor = R.color.icon_agenda_bg;
                break;
            case ORDER:
                iconRes = R.drawable.ic_shopping_cart_outline;
                iconColor = R.color.icon_dashboard;
                iconBgColor = R.color.icon_dashboard_bg;
                break;
            case INVOICE:
            default:
                iconRes = R.drawable.ic_receipt_outline;
                iconColor = R.color.icon_documents;
                iconBgColor = R.color.icon_documents_bg;
                break;
        }

        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), iconColor)));
        holder.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), iconBgColor)));

        // Unique transition name for each icon
        androidx.core.view.ViewCompat.setTransitionName(holder.icon, "icon_" + activity.type + "_" + activity.id);
        androidx.core.view.ViewCompat.setTransitionName(holder.itemView, "container_" + activity.type + "_" + activity.id);

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            
            // Add subtle click animation
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
                    
            listener.onItemClick(activity, holder.icon);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (activity.type != RecentActivity.Type.BOOKING) {
                listener.onEditClick(activity);
                return true;
            }
            return false;
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

        // Add Icon to Badge
        int statusIcon = ("Payée".equals(activity.status) || "Completed".equals(activity.status) || "Terminée".equals(activity.status)) ? R.drawable.ic_status_check :
                        ("Annulée".equals(activity.status) || "Cancelled".equals(activity.status)) ? R.drawable.ic_status_x : R.drawable.ic_status_clock;
        
        holder.txtStatus.setCompoundDrawablesWithIntrinsicBounds(statusIcon, 0, 0, 0);
        holder.txtStatus.setCompoundDrawablePadding(8);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(holder.txtStatus, ColorStateList.valueOf(statusColor));

        holder.txtStatus.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            listener.onStatusClick(activity);
        });

        View btnEdit = holder.itemView.findViewById(R.id.btn_edit_item);
        if (btnEdit != null) {
            btnEdit.setVisibility(activity.type == RecentActivity.Type.BOOKING ? View.GONE : View.VISIBLE);
            btnEdit.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                listener.onEditClick(activity);
            });
        }

        holder.txtStatus.setOnLongClickListener(v -> {
            if (activity.type != RecentActivity.Type.BOOKING && !"Payée".equals(activity.status)) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
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
