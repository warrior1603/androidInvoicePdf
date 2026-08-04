package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.chouchene.factures.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ClientsHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clients_hub, container, false);

        TabLayout tabLayout = view.findViewById(R.id.clientsTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.clientsViewPager);

        viewPager.setAdapter(new ClientsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.label_all_clients);
            } else {
                tab.setText(R.string.label_recent_clients);
            }
        }).attach();

        // If we are navigating here to highlight a specific client, switch to "All Clients" tab
        if (getArguments() != null && getArguments().containsKey("highlight_client_id")) {
            viewPager.setCurrentItem(0, false);
        }

        return view;
    }

    private static class ClientsPagerAdapter extends FragmentStateAdapter {

        public ClientsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return ClientListFragment.newInstance(ClientListFragment.Mode.ALL);
            } else {
                return ClientListFragment.newInstance(ClientListFragment.Mode.RECENT);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
