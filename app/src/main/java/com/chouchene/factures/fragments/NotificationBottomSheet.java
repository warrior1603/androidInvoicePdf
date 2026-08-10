package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.model.AppNotification;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class NotificationBottomSheet extends BottomSheetDialogFragment {

    private List<AppNotification> notifications;

    public NotificationBottomSheet() {
        // Required empty public constructor
    }

    public void setNotifications(List<AppNotification> notifications) {
        this.notifications = notifications;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_notifications);
        View emptyState = view.findViewById(R.id.empty_notifications);

        if (notifications == null || notifications.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setAdapter(new NotificationAdapter(notifications));
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<AppNotification> items;

        public NotificationAdapter(List<AppNotification> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppNotification item = items.get(position);
            holder.title.setText(item.title);
            holder.desc.setText(item.description);
            holder.dot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);

            int iconRes = R.drawable.rounded_info_24;
            int colorRes = R.color.icon_agenda;
            
            if (item.type == AppNotification.Type.ALERT) {
                iconRes = R.drawable.rounded_history_24;
                colorRes = R.color.status_cancelled;
            } else if (item.type == AppNotification.Type.SUCCESS) {
                iconRes = R.drawable.rounded_receipt_long_24;
                colorRes = R.color.status_paid;
            }

            holder.icon.setImageResource(iconRes);
            holder.icon.setColorFilter(ContextCompat.getColor(requireContext(), colorRes));
            holder.iconContainer.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorRes));
            holder.iconContainer.setAlpha(0.15f);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc;
            ImageView icon;
            View dot;
            com.google.android.material.card.MaterialCardView iconContainer;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.notif_title);
                desc = v.findViewById(R.id.notif_desc);
                icon = v.findViewById(R.id.notif_icon);
                dot = v.findViewById(R.id.notif_unread_dot);
                iconContainer = v.findViewById(R.id.notif_icon_container);
            }
        }
    }
}
