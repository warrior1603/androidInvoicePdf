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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DocumentsHubFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Chip filterChip;
    private ChipGroup statusChipGroup;
    private DocumentsViewModel viewModel;

    public DocumentsHubFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(DocumentsViewModel.class);
        return inflater.inflate(R.layout.fragment_documents_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        filterChip = view.findViewById(R.id.filterChip);
        statusChipGroup = view.findViewById(R.id.statusChipGroup);

        viewPager.setAdapter(new DocumentsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.title_invoices));
                    tab.setIcon(R.drawable.rounded_receipt_long_24);
                    break;
                case 1:
                    tab.setText(getString(R.string.title_orders));
                    tab.setIcon(R.drawable.rounded_shopping_cart_24);
                    break;
            }
        }).attach();

        viewModel.getCurrentFilter().observe(getViewLifecycleOwner(), filter -> {
            if (filter != null && filter.label != null) {
                filterChip.setVisibility(View.VISIBLE);
                filterChip.setText("Filtré par: " + filter.label);
            } else {
                filterChip.setVisibility(View.GONE);
            }
            
            // Sync chips if filter changed from outside (e.g. cleared)
            if (filter == null || filter.status == null) {
                statusChipGroup.check(R.id.chipAll);
            }
        });

        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setStatusFilter(null);
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPaid) viewModel.setStatusFilter("Payée");
            else if (id == R.id.chipPending) viewModel.setStatusFilter("En attente");
            else if (id == R.id.chipCancelled) viewModel.setStatusFilter("Annulée");
            else viewModel.setStatusFilter(null);
        });

        filterChip.setOnCloseIconClickListener(v -> viewModel.clearTimeFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private static class DocumentsPagerAdapter extends FragmentStateAdapter {

        public DocumentsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 1) {
                return new BonDeCommandeFragment();
            }
            return new InvoiceGenrationFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}