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
        
        String city = client.getVille();
        if (city != null && !city.isEmpty()) {
            holder.textViewCity.setText("LOC: " + city.toUpperCase());
        } else {
            holder.textViewCity.setText("LOC: N/A");
        }

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

        holder.buttonEmail.setOnClickListener(v -> {
            String email = client.email;
            if (email != null && !email.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                fragmentActivity.startActivity(Intent.createChooser(intent, "Envoyer un e-mail..."));
            } else {
                Toast.makeText(fragmentActivity, "Adresse e-mail non disponible", Toast.LENGTH_SHORT).show();
            }
        });

        holder.buttonAddInvoice.setOnClickListener(v -> {
            Intent intent = new Intent(fragmentActivity, com.chouchene.factures.DocumentStudioActivity.class);
            intent.putExtra("EXTRA_CLIENT_NAME", client.getClientName());
            intent.putExtra("EXTRA_CLIENT_EMAIL", client.email);
            intent.putExtra("EXTRA_CLIENT_PHONE", client.phone);
            intent.putExtra("EXTRA_CLIENT_STREET", client.getStreet());
            intent.putExtra("EXTRA_CLIENT_ZIP", client.getCodePostale());
            intent.putExtra("EXTRA_CLIENT_CITY", client.getVille());
            fragmentActivity.startActivity(intent);
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
        MaterialButton buttonCall, buttonEmail, buttonAddInvoice;
        View cardview;
        MaterialCardView avatarContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewItem);
            textViewCity = itemView.findViewById(R.id.textViewCity);
            textViewInitials = itemView.findViewById(R.id.textViewInitials);
            buttonCall = itemView.findViewById(R.id.buttonCall);
            buttonEmail = itemView.findViewById(R.id.buttonEmail);
            buttonAddInvoice = itemView.findViewById(R.id.buttonAddInvoice);
            cardview = itemView.findViewById(R.id.cardView);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
        }
    }
}
