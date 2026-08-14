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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.model.AppNotification;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class NotificationBottomSheet extends BottomSheetDialogFragment {

    private List<AppNotification> notifications;
    private NotificationAdapter adapter;
    private RecyclerView rv;
    private View emptyState, btnClearAll;

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

        rv = view.findViewById(R.id.rv_notifications);
        emptyState = view.findViewById(R.id.empty_notifications);
        btnClearAll = view.findViewById(R.id.btn_clear_all);

        btnClearAll.setOnClickListener(v -> {
            if (notifications != null) {
                notifications.clear();
                updateUI();
            }
        });

        updateUI();
    }

    private void updateUI() {
        if (notifications == null || notifications.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.VISIBLE);
            
            if (adapter == null) {
                adapter = new NotificationAdapter(notifications);
                rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                rv.setAdapter(adapter);
                setupSwipeToDismiss();
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setupSwipeToDismiss() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAbsoluteAdapterPosition();
                notifications.remove(position);
                adapter.notifyItemRemoved(position);
                if (notifications.isEmpty()) {
                    updateUI();
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rv);
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

            int colorRes = R.color.icon_agenda;
            if (item.type == AppNotification.Type.ALERT) {
                colorRes = R.color.status_cancelled;
            } else if (item.type == AppNotification.Type.SUCCESS) {
                colorRes = R.color.status_paid;
            }

            holder.icon.setImageResource(item.iconRes != 0 ? item.iconRes : R.drawable.rounded_info_24);
            holder.icon.setColorFilter(ContextCompat.getColor(requireContext(), colorRes));
            
            // Set background color with 15% alpha without affecting the icon visibility
            int baseColor = ContextCompat.getColor(requireContext(), colorRes);
            int alphaColor = androidx.core.graphics.ColorUtils.setAlphaComponent(baseColor, 38); // 38 is ~15% of 255
            holder.iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(alphaColor));

            com.chouchene.factures.utils.UIUtils.applyClickScale(holder.itemView);

            holder.itemView.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                item.isRead = true;
                notifyItemChanged(position);
                
                if (item.targetFragmentId != 0) {
                    dismiss();
                    // Navigate to the target fragment
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(item.targetFragmentId);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc;
            ImageView icon;
            View dot;
            View iconContainer;
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
