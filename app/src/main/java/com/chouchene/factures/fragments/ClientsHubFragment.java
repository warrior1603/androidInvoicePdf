package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.chouchene.factures.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ClientsHubFragment extends Fragment {

    private ClientsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(ClientsViewModel.class);
        return inflater.inflate(R.layout.fragment_clients_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.clientsTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.clientsViewPager);

        ClientsPagerAdapter adapter = new ClientsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.label_all_clients);
                tab.setIcon(R.drawable.ic_typcn_clients);
            } else {
                tab.setText(R.string.label_recent_clients);
                tab.setIcon(R.drawable.ic_typcn_time);
            }
        }).attach();

        updateClientCount(tabLayout);
        handleNewArguments(viewPager);
    }

    private void updateClientCount(TabLayout tabLayout) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            int count = com.chouchene.factures.database.DatabaseClient.getInstance(requireContext())
                    .getAppDatabase().clientDao().getAllClients().size();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    TabLayout.Tab tab = tabLayout.getTabAt(0);
                    if (tab != null) {
                        tab.setText(getString(R.string.label_all_clients) + " (" + count + ")");
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            ViewPager2 viewPager = getView().findViewById(R.id.clientsViewPager);
            if (viewPager != null) {
                handleNewArguments(viewPager);
            }
            TabLayout tabLayout = getView().findViewById(R.id.clientsTabLayout);
            if (tabLayout != null) {
                updateClientCount(tabLayout);
            }
        }
    }

    private void handleNewArguments(ViewPager2 viewPager) {
        if (getArguments() != null && getArguments().containsKey("highlight_client_id")) {
            int id = getArguments().getInt("highlight_client_id", -1);
            if (id != -1) {
                viewPager.setCurrentItem(0, false);
                viewModel.setHighlightClientId(id);
                getArguments().remove("highlight_client_id");
            }
        }
    }

    private static class ClientsPagerAdapter extends FragmentStateAdapter {
        private final Bundle arguments;

        public ClientsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
            this.arguments = fragment.getArguments();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            int highlightId = (arguments != null) ? arguments.getInt("highlight_client_id", -1) : -1;
            if (position == 0) {
                return ClientListFragment.newInstance(ClientListFragment.Mode.ALL, highlightId);
            } else {
                return ClientListFragment.newInstance(ClientListFragment.Mode.RECENT, -1);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
