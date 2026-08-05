package com.chouchene.factures.fragments;

import android.graphics.Color;
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

import com.chouchene.factures.R;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.database.DatabaseClient;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

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
        db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().invoiceDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics_detail, container, false);

        TextView revenueLabel = view.findViewById(R.id.revenueLabel);
        TextView totalRevenueTxt = view.findViewById(R.id.totalRevenue);
        TextView documentCountTxt = view.findViewById(R.id.documentCount);
        TextView chartTitle = view.findViewById(R.id.chartTitle);
        BarChart barChart = view.findViewById(R.id.barChart);
        View chartEmptyState = view.findViewById(R.id.chart_empty_state);

        Executors.newSingleThreadExecutor().execute(() -> {
            Date today = new Date();
            float revenue = 0;
            int count = 0;
            List<BarEntry> chartEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            String labelTop = "";
            String labelChart = "";

            Calendar cal = Calendar.getInstance();

            switch (timeframe) {
                case DAILY:
                    revenue = db.getDailyIncome(today);
                    count = db.getDailyCount(today);
                    labelTop = "REVENU DU JOUR";
                    labelChart = "DERNIERS 7 JOURS";
                    
                    for (int i = 6; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.DAY_OF_YEAR, -i);
                        Date d = cal.getTime();
                        float dayTotal = db.getDailyIncome(d);
                        chartEntries.add(new BarEntry(6 - i, dayTotal));
                        labels.add(new SimpleDateFormat("dd/MM", Locale.getDefault()).format(d));
                    }
                    break;

                case MONTHLY:
                    revenue = db.getMonthlyIncome(today);
                    count = db.getMonthlyCount(today);
                    labelTop = "REVENU DU MOIS";
                    labelChart = "DERNIERS 6 MOIS";
                    
                    for (int i = 5; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.MONTH, -i);
                        Date d = cal.getTime();
                        float monthTotal = db.getMonthlyIncome(d);
                        chartEntries.add(new BarEntry(5 - i, monthTotal));
                        labels.add(new SimpleDateFormat("MM/yy", Locale.getDefault()).format(d));
                    }
                    break;

                case YEARLY:
                    revenue = db.getYearlyIncome(today);
                    count = db.getYearlyCount(today);
                    labelTop = "REVENU DE L'ANNÉE";
                    labelChart = "ÉVOLUTION ANNUELLE";
                    
                    int currentMonth = cal.get(Calendar.MONTH);
                    for (int i = 0; i <= currentMonth; i++) {
                        cal.setTime(new Date());
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.MONTH, i);
                        Date d = cal.getTime();
                        float monthTotal = db.getMonthlyIncome(d);
                        chartEntries.add(new BarEntry(i, monthTotal));
                        labels.add(new SimpleDateFormat("MMM", Locale.getDefault()).format(d));
                    }
                    break;
            }

            final float finalRev = revenue;
            final int finalCount = count;
            final String finalLabelTop = labelTop;
            final String finalLabelChart = labelChart;

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    revenueLabel.setText(finalLabelTop);
                    totalRevenueTxt.setText(String.format(Locale.getDefault(), "%.2f €", finalRev));
                    documentCountTxt.setText(String.valueOf(finalCount));
                    chartTitle.setText(finalLabelChart);
                    setupChart(barChart, chartEntries, labels, chartEmptyState);
                });
            }
        });

        return view;
    }

    private void setupChart(BarChart barChart, List<BarEntry> entries, List<String> labels, View emptyState) {
        // Check if all entries are 0
        boolean hasData = false;
        for (BarEntry e : entries) {
            if (e.getY() > 0) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            barChart.setVisibility(View.INVISIBLE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        @ColorInt int primaryColor = resolveColor(androidx.appcompat.R.attr.colorPrimary);
        @ColorInt int onSurfaceVariant = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setTouchEnabled(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setFitBars(true);
        barChart.setExtraOffsets(10f, 10f, 10f, 10f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(onSurfaceVariant);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setYOffset(10f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.argb(30, 128, 128, 128));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(onSurfaceVariant);
        leftAxis.setXOffset(10f);
        leftAxis.setLabelCount(4, false);
        leftAxis.setAxisMinimum(0f);
        
        barChart.getAxisRight().setEnabled(false);

        BarDataSet dataSet = new BarDataSet(entries, "Revenu");
        dataSet.setColor(primaryColor);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(primaryColor);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        barChart.animateY(1000);

        barChart.invalidate();
    }

    @ColorInt
    private int resolveColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
