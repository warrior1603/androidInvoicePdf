package com.chouchene.factures.adapter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.utils.AvatarHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final FragmentActivity fragmentActivity;
    private final ArrayList<Client> originalList;
    private final ArrayList<Client> filteredList;
    private OnDataChangedListener onDataChangedListener;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public CustomAdapter(FragmentActivity context, ArrayList<Client> values, int ignoredId) {
        this.fragmentActivity = context;
        this.originalList = new ArrayList<>(values);
        this.filteredList = new ArrayList<>(values);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_with_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Client client = filteredList.get(position);
        holder.textView.setText(client.getClientName());
        holder.textViewCity.setText(client.getVille());

        holder.textViewInitials.setText(AvatarHelper.getInitials(client.getClientName()));
        holder.avatarContainer.setCardBackgroundColor(AvatarHelper.getColorForName(client.getClientName()));

        holder.cardview.setTransitionName("client_card_" + client.getId());

        holder.cardview.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("client_id", client.getId());

            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(holder.cardview, "client_card_transition")
                    .build();

            Navigation.findNavController(v).navigate(R.id.clientDetailFragment, args, null, extras);
        });

        com.chouchene.factures.utils.UIUtils.applyClickScale(holder.cardview);

        holder.buttonCall.setOnClickListener(v -> {
            String phone = client.phone;
            if (phone != null && !phone.isEmpty()) {
                fragmentActivity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
            } else {
                Toast.makeText(fragmentActivity, "Numéro de téléphone non disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void setData(ArrayList<Client> clients) {
        originalList.clear();
        originalList.addAll(clients);
        filteredList.clear();
        filteredList.addAll(clients);
        notifyDataSetChanged();
        if (onDataChangedListener != null) {
            onDataChangedListener.onDataChanged();
        }
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Client obj : originalList) {
                if (obj.getClientName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(obj);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setHighlightId(int id) { }

    public Client getClientAt(int position) {
        return filteredList.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView, textViewCity, textViewInitials;
        MaterialButton buttonCall;
        View cardview;
        MaterialCardView avatarContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewItem);
            textViewCity = itemView.findViewById(R.id.textViewCity);
            textViewInitials = itemView.findViewById(R.id.textViewInitials);
            buttonCall = itemView.findViewById(R.id.buttonCall);
            cardview = itemView.findViewById(R.id.cardView);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
        }
    }
}
