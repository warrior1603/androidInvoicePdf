package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.chouchene.factures.POJO.DailyIncome;
import com.chouchene.factures.POJO.MonthlyIncome;
import com.chouchene.factures.R;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.database.AppDatabase;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RapportsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.activity_rapports, container, false);

        BarChart barChart = myView.findViewById(R.id.barChart);
        TextView chartEmptyState = myView.findViewById(R.id.chart_empty_state);

        InvoiceDao db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().invoiceDao();
        
        // Bar Chart Data
        List<MonthlyIncome> mouthlyIncomeList = db.getMonthlyIncomeTotals();
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (MonthlyIncome mouthlyIncome : mouthlyIncomeList) {
            entries.add(new BarEntry(i++, (float) mouthlyIncome.monthlyTotal));
            labels.add(mouthlyIncome.month);
        }

        // Summary Calculations for TODAY and THIS MONTH
        Date today = new Date();
        float sumDaily = db.getDailyIncome(today);
        float sumMonthly = db.getMonthlyIncome(today);

        TextView totalRevenuesDay = myView.findViewById(R.id.todayRevenue);
        TextView totalRevenusMonth = myView.findViewById(R.id.moisRevenue);

        totalRevenuesDay.setText(String.format(Locale.getDefault(), "+%.2f €", sumDaily));
        totalRevenusMonth.setText(String.format(Locale.getDefault(), "%.2f €", sumMonthly));

        // Get primary color from theme
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        @ColorInt int primaryColor = typedValue.data;

        // Set up the X-axis labels from data
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

        // Set up the BarDataSet and BarData
        BarDataSet dataSet = new BarDataSet(entries, "Revenue");
        dataSet.setColor(primaryColor);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorPrimaryContainer));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true);
        barChart.setScaleEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.animateY(800);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        barChart.getAxisRight().setEnabled(false);

        if (entries.isEmpty()) {
            barChart.setVisibility(View.INVISIBLE);
            chartEmptyState.setVisibility(View.VISIBLE);
        } else {
            barChart.setVisibility(View.VISIBLE);
            chartEmptyState.setVisibility(View.GONE);
            barChart.invalidate();
        }
        
        return myView;
    }

    @ColorInt
    private int resolveColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onResume() {
        super.onResume();
//        // Check if the activity has a default ActionBar
//        if (getActivity() != null) {
//            getActivity().setTitle("   Paramètres de l'application");  // Set the ActionBar title
//        }
//
//        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        // Enable the display of the home icon
//        actionBar.setDisplayShowHomeEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(true);
//        // Change the ActionBar icon
//        actionBar.setLogo(R.drawable.settings_gear_svgrepo_com);
    }

}
