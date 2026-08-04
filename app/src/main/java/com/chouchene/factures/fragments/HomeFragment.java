package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.chouchene.factures.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView cardDocuments = view.findViewById(R.id.card_documents);
        MaterialCardView cardClients = view.findViewById(R.id.card_clients);
        MaterialCardView cardDashboard = view.findViewById(R.id.card_dashboard);
        MaterialCardView cardProfile = view.findViewById(R.id.card_profile);

        BottomNavigationView navView = requireActivity().findViewById(R.id.bottomNavigationView);

        cardDocuments.setOnClickListener(v -> navView.setSelectedItemId(R.id.documentsHubFragment));
        cardClients.setOnClickListener(v -> navView.setSelectedItemId(R.id.clientsFragment));
        cardDashboard.setOnClickListener(v -> navView.setSelectedItemId(R.id.parametresFragment));
        
        cardProfile.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.personalSettingsFragment));
    }
}
