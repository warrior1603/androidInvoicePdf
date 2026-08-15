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
        holder.txtSubtitle.setText(activity.subtitle);
        holder.txtAmount.setText(String.format(Locale.getDefault(), "%.2f €", activity.amount));
        
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtDate.setText(fmt.format(activity.date));

        int iconRes;
        int iconColor;
        int iconBgColor;
        
        switch (activity.type) {
            case BOOKING:
                iconRes = R.drawable.ic_typcn_agenda;
                iconColor = R.color.icon_agenda;
                iconBgColor = R.color.icon_agenda_bg;
                break;
            case ORDER:
                iconRes = R.drawable.ic_typcn_cart;
                iconColor = R.color.icon_dashboard;
                iconBgColor = R.color.icon_dashboard_bg;
                break;
            case INVOICE:
            default:
                iconRes = R.drawable.ic_typcn_document;
                iconColor = R.color.icon_documents;
                iconBgColor = R.color.icon_documents_bg;
                break;
        }

        holder.icon.setImageResource(iconRes);
        holder.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), iconColor)));
        
        View iconBg = holder.itemView.findViewById(R.id.icon_bg);
        if (iconBg != null) {
            iconBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), iconColor)));
        }

        // Unique transition name for each icon
        androidx.core.view.ViewCompat.setTransitionName(holder.icon, "icon_" + activity.type + "_" + activity.id);
        androidx.core.view.ViewCompat.setTransitionName(holder.itemView, "container_" + activity.type + "_" + activity.id);

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onItemClick(activity, holder.icon);
        });

        com.chouchene.factures.utils.UIUtils.applyClickScale(holder.itemView);

        holder.itemView.setOnLongClickListener(v -> {
            listener.onEditClick(activity);
            return true;
        });
        
        String displayStatus = activity.status;
        if ("Scheduled".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_scheduled);
        else if ("Completed".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_completed);
        else if ("Cancelled".equals(displayStatus)) displayStatus = holder.itemView.getContext().getString(R.string.status_cancelled);
        
        holder.txtStatus.setText(displayStatus != null ? displayStatus : "En attente");
        int statusColor;
        
        if ("Payée".equals(activity.status) || "Completed".equals(activity.status) || "Terminée".equals(activity.status)) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid);
        } else if ("Annulée".equals(activity.status) || "Cancelled".equals(activity.status)) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled);
        } else {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
        }
        
        holder.txtStatus.setBackground(null);
        holder.txtStatus.setTextColor(statusColor);

        // Add Icon Dot (Studio Precision Suggestion 1)
        int statusIcon = ("Payée".equals(activity.status) || "Completed".equals(activity.status) || "Terminée".equals(activity.status)) ? R.drawable.ic_status_check :
                        ("Annulée".equals(activity.status) || "Cancelled".equals(activity.status)) ? R.drawable.ic_status_x : R.drawable.ic_status_clock;
        
        holder.txtStatus.setCompoundDrawablesWithIntrinsicBounds(statusIcon, 0, 0, 0);
        holder.txtStatus.setCompoundDrawablePadding(12);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(holder.txtStatus, ColorStateList.valueOf(statusColor));

        holder.txtStatus.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            listener.onStatusClick(activity);
        });

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
        TextView txtClientName, txtDate, txtAmount, txtStatus, txtSubtitle;
        ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClientName = itemView.findViewById(R.id.txtClientName);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}
