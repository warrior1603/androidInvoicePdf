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

public class DocumentsHubFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    public DocumentsHubFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documents_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        DocumentsPagerAdapter adapter = new DocumentsPagerAdapter(this);
        viewPager.setAdapter(adapter);

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