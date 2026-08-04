package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalyticsDetailFragment extends Fragment {

    public enum Timeframe { DAILY, MONTHLY, YEARLY }

    private Timeframe timeframe;
    private InvoiceDao db;

    public static AnalyticsDetailFragment newInstance(Timeframe timeframe) {
        AnalyticsDetailFragment fragment = new AnalyticsDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("timeframe", timeframe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            timeframe = (Timeframe) getArguments().getSerializable("timeframe");
        }
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().invoiceDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics_detail, container, false);

        TextView totalRevenueTxt = view.findViewById(R.id.totalRevenue);
        TextView documentCountTxt = view.findViewById(R.id.documentCount);
        BarChart barChart = view.findViewById(R.id.barChart);
        TextView chartEmptyState = view.findViewById(R.id.chart_empty_state);

        Date today = new Date();
        float revenue = 0;
        int count = 0;
        List<BarEntry> chartEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        switch (timeframe) {
            case DAILY:
                revenue = db.getDailyIncome(today);
                count = db.getDailyCount(today);
                List<DailyIncome> dailyIncomes = db.getDailyIncomeTotals();
                int i = 0;
                for (DailyIncome di : dailyIncomes) {
                    chartEntries.add(new BarEntry(i++, (float) di.dailyTotal));
                    labels.add(di.date.toString()); // Simplify for now
                }
                break;
            case MONTHLY:
                revenue = db.getMonthlyIncome(today);
                count = db.getMonthlyCount(today);
                List<MonthlyIncome> monthlyIncomes = db.getMonthlyIncomeTotals();
                int j = 0;
                for (MonthlyIncome mi : monthlyIncomes) {
                    chartEntries.add(new BarEntry(j++, (float) mi.monthlyTotal));
                    labels.add(mi.month);
                }
                break;
            case YEARLY:
                revenue = db.getYearlyIncome(today);
                count = db.getYearlyCount(today);
                // Yearly chart could group by month for the current year
                List<MonthlyIncome> yearlyByMonth = db.getMonthlyIncomeTotals();
                int k = 0;
                for (MonthlyIncome mi : yearlyByMonth) {
                    chartEntries.add(new BarEntry(k++, (float) mi.monthlyTotal));
                    labels.add(mi.month);
                }
                break;
        }

        totalRevenueTxt.setText(String.format(Locale.getDefault(), "%.2f €", revenue));
        documentCountTxt.setText(String.valueOf(count));

        setupChart(barChart, chartEntries, labels, chartEmptyState);

        return view;
    }

    private void setupChart(BarChart barChart, List<BarEntry> entries, List<String> labels, View emptyState) {
        if (entries.isEmpty()) {
            barChart.setVisibility(View.INVISIBLE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        @ColorInt int primaryColor = resolveColor(androidx.appcompat.R.attr.colorPrimary);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

        BarDataSet dataSet = new BarDataSet(entries, "Revenu");
        dataSet.setColor(primaryColor);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(800);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }

    @ColorInt
    private int resolveColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}