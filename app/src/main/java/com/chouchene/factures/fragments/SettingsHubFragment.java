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

public class SettingsHubFragment extends Fragment {

    public static SettingsHubFragment newInstance(int tabIndex) {
        SettingsHubFragment fragment = new SettingsHubFragment();
        Bundle args = new Bundle();
        args.putInt("target_tab", tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.settingsTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.settingsViewPager);

        viewPager.setAdapter(new SettingsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Général");
                    tab.setIcon(R.drawable.ic_outline_settings);
                    break;
                case 1:
                    tab.setText("Gestion");
                    tab.setIcon(R.drawable.ic_outline_database);
                    break;
                case 2:
                    tab.setText("Entreprise");
                    tab.setIcon(R.drawable.ic_outline_building);
                    break;
            }
        }).attach();

        if (getArguments() != null) {
            int target = getArguments().getInt("target_tab", 0);
            viewPager.setCurrentItem(target, false);
        }
    }

    private static class SettingsPagerAdapter extends FragmentStateAdapter {
        public SettingsPagerAdapter(@NonNull Fragment fragment) { super(fragment); }
        @NonNull @Override public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new GeneralSettingsFragment();
                case 1: return new ManagementSettingsFragment();
                case 2: return new EntrepriseSettingsFragment();
                default: return new GeneralSettingsFragment();
            }
        }
        @Override public int getItemCount() { return 3; }
    }
}