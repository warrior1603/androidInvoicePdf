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

public class RapportsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_rapports, container, false);

        TabLayout tabLayout = view.findViewById(R.id.rapportsTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.rapportsViewPager);

        viewPager.setAdapter(new RapportsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.label_today));
                    tab.setIcon(R.drawable.ic_typcn_time);
                    break;
                case 1:
                    tab.setText(getString(R.string.label_this_month));
                    tab.setIcon(R.drawable.ic_typcn_calendar_outline);
                    break;
                case 2:
                    tab.setText(getString(R.string.label_this_year));
                    tab.setIcon(R.drawable.ic_typcn_chart_line);
                    break;
                case 3:
                    tab.setText("Global");
                    tab.setIcon(R.drawable.ic_typcn_world);
                    break;
            }
        }).attach();

        return view;
    }

    private static class RapportsPagerAdapter extends FragmentStateAdapter {

        public RapportsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return AnalyticsDetailFragment.newInstance(AnalyticsDetailFragment.Timeframe.DAILY);
                case 1:
                    return AnalyticsDetailFragment.newInstance(AnalyticsDetailFragment.Timeframe.MONTHLY);
                case 2:
                    return AnalyticsDetailFragment.newInstance(AnalyticsDetailFragment.Timeframe.YEARLY);
                case 3:
                    return AnalyticsDetailFragment.newInstance(AnalyticsDetailFragment.Timeframe.ALL_TIME);
                default:
                    return AnalyticsDetailFragment.newInstance(AnalyticsDetailFragment.Timeframe.DAILY);
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}