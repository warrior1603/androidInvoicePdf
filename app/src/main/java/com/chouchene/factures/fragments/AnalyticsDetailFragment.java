package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import com.chouchene.factures.model.RecentActivity;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import com.google.android.material.button.MaterialButton;

public class AnalyticsDetailFragment extends Fragment implements com.chouchene.factures.adapter.HistoryAdapter.OnHistoryActionListener {

    public enum Timeframe { DAILY, MONTHLY, YEARLY, ALL_TIME }

    private Timeframe timeframe;
    private InvoiceDao db;
    private ExpenseDao expenseDb;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private View mainContent;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressMargin;
    private TextView txtMarginLabel;

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
        Context context = getContext();
        if (context != null) {
            AppDatabase appDb = DatabaseClient.getInstance(context.getApplicationContext()).getAppDatabase();
            db = appDb.invoiceDao();
            expenseDb = appDb.expenseDao();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics_detail, container, false);

        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        mainContent = view.findViewById(R.id.main_content);
        progressMargin = view.findViewById(R.id.progress_margin);
        txtMarginLabel = view.findViewById(R.id.txt_margin_label);
        
        TextView revenueLabel = view.findViewById(R.id.revenueLabel);
        TextView totalRevenueTxt = view.findViewById(R.id.totalRevenue);
        TextView documentCountTxt = view.findViewById(R.id.documentCount);
        TextView totalClientsTxt = view.findViewById(R.id.txt_total_clients_val);
        TextView chartTitle = view.findViewById(R.id.chartTitle);
        TextView growthValTxt = view.findViewById(R.id.txt_growth_val);
        BarChart barChart = view.findViewById(R.id.barChart);
        View chartEmptyState = view.findViewById(R.id.chart_empty_state);
        View cardExpenses = view.findViewById(R.id.cardExpenses);
        MaterialButton btnExportCsv = view.findViewById(R.id.btnExportCsv);

        cardExpenses.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.expensesFragment));
        btnExportCsv.setOnClickListener(v -> exportToCSV());

        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            mainContent.setVisibility(View.GONE);
        }

        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return view;

        Executors.newSingleThreadExecutor().execute(() -> {
            try { Thread.sleep(700); } catch (Exception ignored) {}
            Date today = new Date();
            float revenue = 0;
            float prevRevenue = 0;
            int count = 0;
            int clientCount = DatabaseClient.getInstance(context).getAppDatabase().clientDao().getAllClients().size();
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

                    cal.setTime(today);
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    prevRevenue = db.getDailyIncome(cal.getTime()) - expenseDb.getDailyExpenses(cal.getTime());
                    
                    for (int i = 6; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.DAY_OF_YEAR, -i);
                        Date d = cal.getTime();
                        float income = db.getDailyIncome(d);
                        float exp = expenseDb.getDailyExpenses(d);
                        BarEntry entry = new BarEntry(6 - i, income - exp);
                        entry.setData(new DocumentsViewModel.Filter("DAY", 
                            String.valueOf(d.getTime()),
                            new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(d), null));
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

                    cal.setTime(today);
                    cal.add(Calendar.MONTH, -1);
                    prevRevenue = db.getMonthlyIncome(cal.getTime()) - expenseDb.getMonthlyExpenses(cal.getTime());
                    
                    for (int i = 5; i >= 0; i--) {
                        cal.setTime(new Date());
                        cal.add(Calendar.MONTH, -i);
                        Date d = cal.getTime();
                        float income = db.getMonthlyIncome(d);
                        float exp = expenseDb.getMonthlyExpenses(d);
                        BarEntry entry = new BarEntry(5 - i, income - exp);
                        entry.setData(new DocumentsViewModel.Filter("MONTH", 
                            new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(d),
                            new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d), null));
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

                    cal.setTime(today);
                    cal.add(Calendar.YEAR, -1);
                    prevRevenue = db.getYearlyIncome(cal.getTime()) - expenseDb.getYearlyExpenses(cal.getTime());
                    
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
                            new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d), null));
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
                                new SimpleDateFormat("yyyy", Locale.getDefault()).format(d), null));
                        chartEntries.add(entry);
                        labels.add(new SimpleDateFormat("yyyy", Locale.getDefault()).format(d));
                    }
                    break;
            }

            final float finalRev = revenue;
            final float finalPrevRev = prevRevenue;
            final int finalCount = count;
            final String finalLabelTop = labelTop;
            final String finalLabelChart = labelChart;

            float totalIncome = 0;
            if (timeframe == Timeframe.DAILY) totalIncome = db.getDailyIncome(today);
            else if (timeframe == Timeframe.MONTHLY) totalIncome = db.getMonthlyIncome(today);
            else if (timeframe == Timeframe.YEARLY) totalIncome = db.getYearlyIncome(today);
            else totalIncome = db.getTotalRevenue();

            final float incomeForMargin = totalIncome;

            activity.runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                
                if (shimmerContainer != null) {
                    shimmerContainer.stopShimmer();
                    shimmerContainer.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                }
                revenueLabel.setText(finalLabelTop);
                totalRevenueTxt.setText(String.format(Locale.getDefault(), "%.2f €", finalRev));
                documentCountTxt.setText(String.valueOf(finalCount));
                totalClientsTxt.setText(String.valueOf(clientCount));
                chartTitle.setText(finalLabelChart);

                // Update Growth
                if (finalPrevRev > 0) {
                    float growth = ((finalRev - finalPrevRev) / finalPrevRev) * 100;
                    growthValTxt.setText(String.format(Locale.getDefault(), "%+.1f%%", growth));
                    growthValTxt.setTextColor(growth >= 0 ? 
                        ContextCompat.getColor(context, R.color.status_paid) : 
                        ContextCompat.getColor(context, R.color.status_cancelled));
                } else if (finalRev > 0) {
                    growthValTxt.setText("+100%");
                    growthValTxt.setTextColor(ContextCompat.getColor(context, R.color.status_paid));
                } else {
                    growthValTxt.setText("0%");
                    growthValTxt.setTextColor(resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant));
                }

                setupChart(barChart, chartEntries, labels, chartEmptyState);

                // Update Margin
                if (incomeForMargin > 0) {
                    int marginPercent = (int) ((finalRev / incomeForMargin) * 100);
                    if (marginPercent < 0) marginPercent = 0;
                    progressMargin.setProgress(marginPercent);
                    txtMarginLabel.setText("Marge réelle: " + marginPercent + "%");
                } else {
                    progressMargin.setProgress(0);
                    txtMarginLabel.setText("Marge réelle: 0%");
                }
            });
        });

        return view;
    }

    private void setupChart(BarChart barChart, List<BarEntry> entries, List<String> labels, View emptyState) {
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

        Context context = barChart.getContext();
        @ColorInt int primaryColor = resolveColor(context, androidx.appcompat.R.attr.colorPrimary);
        @ColorInt int onSurfaceVariant = resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);

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
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(context);
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
            
            List<RecentActivity> finalActivities = new ArrayList<>();
            for (Invoice i : invoices) finalActivities.add(new RecentActivity(i));

            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.setData(finalActivities);
                rv.scheduleLayoutAnimation();
                dialog.setContentView(view);
                dialog.show();
            });
        });
    }

    @Override
    public void onItemClick(RecentActivity activity, View sharedElement) {
        Invoice invoice = (Invoice) activity.originalObject;
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("transition_name", androidx.core.view.ViewCompat.getTransitionName(sharedElement));

        androidx.navigation.fragment.FragmentNavigator.Extras extras = new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedElement, androidx.core.view.ViewCompat.getTransitionName(sharedElement))
                .build();

        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.webViewPdfFragment, b, null, extras);
        }
    }

    @Override
    public void onDeleteClick(RecentActivity activity) {}

    @Override
    public void onStatusClick(RecentActivity activity) {}

    @Override
    public void onShareClick(RecentActivity activity) {}

    @Override
    public void onStatusChange(RecentActivity activity, String newStatus) {}

    private void exportToCSV() {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Invoice> invoices = db.getAllInvoices();
            StringBuilder csv = new StringBuilder();
            csv.append("Date;Numero;Client;Montant TTC;Statut;Type\n");
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            
            for (Invoice invoice : invoices) {
                csv.append(sdf.format(invoice.date)).append(";")
                   .append(invoice.id).append(";")
                   .append(invoice.clientName != null ? invoice.clientName.replace(";", ",") : "").append(";")
                   .append(String.format(Locale.getDefault(), "%.2f", invoice.amount)).append(";")
                   .append(invoice.status != null ? invoice.status : "").append(";")
                   .append(invoice.type != null ? invoice.type : "").append("\n");
            }

            try {
                File cachePath = new File(context.getCacheDir(), "exports");
                if (!cachePath.exists()) cachePath.mkdirs();
                File csvFile = new File(cachePath, "export_invoices_" + System.currentTimeMillis() + ".csv");
                FileWriter writer = new FileWriter(csvFile);
                writer.write(csv.toString());
                writer.close();

                Uri contentUri = FileProvider.getUriForFile(context, "com.chouchene.factures.provider", csvFile);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    startActivity(Intent.createChooser(intent, context.getString(R.string.action_export_csv)));
                });

            } catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(context, "Erreur lors de l'exportation", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @ColorInt
    private int resolveColor(Context context, int attr) {
        if (context == null) return Color.TRANSPARENT;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
