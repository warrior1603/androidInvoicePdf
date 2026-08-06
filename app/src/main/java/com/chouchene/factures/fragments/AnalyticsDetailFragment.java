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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.chouchene.factures.R;
import com.chouchene.factures.dao.ExpenseDao;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.RecyclerView;

public class AnalyticsDetailFragment extends Fragment implements com.chouchene.factures.adapter.HistoryAdapter.OnHistoryActionListener {

    public enum Timeframe { DAILY, MONTHLY, YEARLY, ALL_TIME }

    private Timeframe timeframe;
    private InvoiceDao db;
    private ExpenseDao expenseDb;

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
        AppDatabase appDb = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
        db = appDb.invoiceDao();
        expenseDb = appDb.expenseDao();
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
        View cardExpenses = view.findViewById(R.id.cardExpenses);

        cardExpenses.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.expensesFragment));

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
                    float dailyIncome = db.getDailyIncome(today);
                    float dailyExpenses = expenseDb.getDailyExpenses(today);
                    revenue = dailyIncome - dailyExpenses;
                    count = db.getDailyCount(today);
                    labelTop = "BÉNÉFICE DU JOUR";
                    labelChart = "DERNIERS 7 JOURS (Profit)";
                    
                    for (int i = 6; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.DAY_OF_YEAR, -i);
                        Date d = cal.getTime();
                        float income = db.getDailyIncome(d);
                        float exp = expenseDb.getDailyExpenses(d);
                        BarEntry entry = new BarEntry(6 - i, income - exp);
                        entry.setData(new DocumentsViewModel.Filter("DAY", 
                            String.valueOf(d.getTime()),
                            new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(d)));
                        chartEntries.add(entry);
                        labels.add(new SimpleDateFormat("dd/MM", Locale.getDefault()).format(d));
                    }
                    break;

                case MONTHLY:
                    float monthlyIncome = db.getMonthlyIncome(today);
                    float monthlyExpenses = expenseDb.getMonthlyExpenses(today);
                    revenue = monthlyIncome - monthlyExpenses;
                    count = db.getMonthlyCount(today);
                    labelTop = "BÉNÉFICE DU MOIS";
                    labelChart = "DERNIERS 6 MOIS (Bénéfice)";
                    
                    for (int i = 5; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.MONTH, -i);
                        Date d = cal.getTime();
                        float income = db.getMonthlyIncome(d);
                        float exp = expenseDb.getMonthlyExpenses(d);
                        BarEntry entry = new BarEntry(5 - i, income - exp);
                        entry.setData(new DocumentsViewModel.Filter("MONTH", 
                            new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(d),
                            new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d)));
                        chartEntries.add(entry);
                        labels.add(new SimpleDateFormat("MM/yy", Locale.getDefault()).format(d));
                    }
                    break;

                case YEARLY:
                    float yearlyIncome = db.getYearlyIncome(today);
                    float yearlyExpenses = expenseDb.getYearlyExpenses(today);
                    revenue = yearlyIncome - yearlyExpenses;
                    count = db.getYearlyCount(today);
                    labelTop = "BÉNÉFICE DE L'ANNÉE";
                    labelChart = "ÉVOLUTION DU BÉNÉFICE";
                    
                    int currentMonth = cal.get(Calendar.MONTH);
                    for (int i = 0; i <= currentMonth; i++) {
                        cal.setTime(new Date());
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.MONTH, i);
                        Date d = cal.getTime();
                        float income = db.getMonthlyIncome(d);
                        float exp = expenseDb.getMonthlyExpenses(d);
                        BarEntry entry = new BarEntry(i, income - exp);
                        entry.setData(new DocumentsViewModel.Filter("MONTH", 
                            new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(d),
                            new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d)));
                        chartEntries.add(entry);
                        labels.add(new SimpleDateFormat("MMM", Locale.getDefault()).format(d));
                    }
                    break;

                case ALL_TIME:
                    revenue = db.getTotalRevenue();
                    count = db.getTotalCount();
                    labelTop = "REVENU TOTAL";
                    labelChart = "ÉVOLUTION PAR ANNÉE";

                    for (int i = 4; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.YEAR, -i);
                        Date d = cal.getTime();
                        float yearTotal = db.getYearlyIncome(d);
                        BarEntry entry = new BarEntry(4 - i, yearTotal);
                        entry.setData(new DocumentsViewModel.Filter("YEAR",
                                new SimpleDateFormat("yyyy", Locale.getDefault()).format(d),
                                new SimpleDateFormat("yyyy", Locale.getDefault()).format(d)));
                        chartEntries.add(entry);
                        labels.add(new SimpleDateFormat("yyyy", Locale.getDefault()).format(d));
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

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getData() instanceof DocumentsViewModel.Filter && getActivity() != null) {
                    DocumentsViewModel.Filter filter = (DocumentsViewModel.Filter) e.getData();
                    showInvoicesBottomSheet(filter);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        barChart.invalidate();
    }

    private void showInvoicesBottomSheet(DocumentsViewModel.Filter filter) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_analytics_invoices_bottom_sheet, null);
        
        TextView title = view.findViewById(R.id.txt_bottom_sheet_title);
        title.setText("Documents pour " + (filter.label != null ? filter.label : ""));
        
        RecyclerView rv = view.findViewById(R.id.rv_analytics_invoices);
        HistoryAdapter adapter = new HistoryAdapter(this);
        rv.setAdapter(adapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            final List<Invoice> invoices;
            if ("MONTH".equals(filter.type)) {
                invoices = db.getInvoicesByMonth(filter.value);
            } else if ("DAY".equals(filter.type)) {
                List<Invoice> dayInvoices;
                try {
                    long timestamp = Long.parseLong(filter.value);
                    dayInvoices = db.getInvoicesByDay(new Date(timestamp));
                } catch (Exception e) {
                    dayInvoices = new ArrayList<>();
                }
                invoices = dayInvoices;
            } else if ("YEAR".equals(filter.type)) {
                invoices = db.getInvoicesByYear(filter.value);
            } else {
                invoices = db.getLatestInvoices(); // Fallback
            }
            
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setData(invoices);
                    dialog.setContentView(view);
                    dialog.show();
                });
            }
        });
    }

    @Override
    public void onItemClick(Invoice invoice, View sharedElement) {
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("transition_name", androidx.core.view.ViewCompat.getTransitionName(sharedElement));

        androidx.navigation.fragment.FragmentNavigator.Extras extras = new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedElement, androidx.core.view.ViewCompat.getTransitionName(sharedElement))
                .build();

        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b, null, extras);
    }

    @Override
    public void onDeleteClick(Invoice invoice) {}

    @Override
    public void onStatusClick(Invoice invoice) {}

    @Override
    public void onShareClick(Invoice invoice) {}

    @Override
    public void onStatusChange(Invoice invoice, String newStatus) {}

    @ColorInt
    private int resolveColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
