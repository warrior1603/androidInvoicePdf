package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintResultCallbackShim;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import androidx.preference.PreferenceManager;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.content.SharedPreferences;

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
        
        View cardExpenses = view.findViewById(R.id.cardExpenses);
        View btnExportCsv = view.findViewById(R.id.btnExportCsv);
        View btnGenerateReportPdf = view.findViewById(R.id.btnGenerateReportPdf);

        cardExpenses.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.expensesFragment));
        btnExportCsv.setOnClickListener(v -> exportToCSV());
        btnGenerateReportPdf.setOnClickListener(v -> generateMonthlyReport());

        loadAnalyticsData(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnalyticsData(false);
    }

    private void loadAnalyticsData(boolean showShimmer) {
        if (getView() == null) return;
        
        TextView revenueLabel = getView().findViewById(R.id.revenueLabel);
        TextView totalRevenueTxt = getView().findViewById(R.id.totalRevenue);
        TextView documentCountTxt = getView().findViewById(R.id.documentCount);
        TextView totalClientsTxt = getView().findViewById(R.id.txt_total_clients_val);
        TextView chartTitle = getView().findViewById(R.id.chartTitle);
        TextView growthValTxt = getView().findViewById(R.id.txt_growth_val);
        BarChart barChart = getView().findViewById(R.id.barChart);
        View chartEmptyState = getView().findViewById(R.id.chart_empty_state);
        View btnExportCsv = getView().findViewById(R.id.btnExportCsv);

        if (showShimmer && shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            mainContent.setVisibility(View.GONE);
            btnExportCsv.setVisibility(View.GONE);
        }

        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
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
                    labelTop = "Bénéfice du jour";
                    labelChart = "Derniers 7 jours (Profit)";

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
                    labelTop = "Bénéfice du mois";
                    labelChart = "Derniers 6 mois (Bénéfice)";

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
                    labelTop = "Bénéfice de l'année";
                    labelChart = "Évolution du bénéfice";

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
                    labelTop = "Revenu total";
                    labelChart = "Évolution par année";

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
            final int finalClientCount = clientCount;
            final List<BarEntry> finalChartEntries = chartEntries;
            final List<String> finalLabels = labels;

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
                    btnExportCsv.setVisibility(View.VISIBLE);
                }
                revenueLabel.setText(finalLabelTop);
                totalRevenueTxt.setText(String.format(Locale.getDefault(), "%.2f €", finalRev));
                documentCountTxt.setText(String.valueOf(finalCount));
                totalClientsTxt.setText(String.valueOf(finalClientCount));
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

                setupChart(barChart, finalChartEntries, finalLabels, chartEmptyState);

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
        
        // Add subtle gradient to bars if supported
        dataSet.setGradientColor(primaryColor, Color.argb(100, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.4f);
        barChart.setData(barData);
        barChart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic);

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

    @Override
    public void onEditClick(RecentActivity activity) {
        if (activity.type == RecentActivity.Type.BOOKING) {
            AddBookingBottomSheet sheet = AddBookingBottomSheet.newInstance(activity.id);
            sheet.setOnBookingAddedListener(() -> loadAnalyticsData(false));
            sheet.show(getChildFragmentManager(), "edit_booking");
            return;
        }
        android.content.Intent intent = new android.content.Intent(requireContext(), com.chouchene.factures.DocumentStudioActivity.class);
        intent.putExtra("EXTRA_MODE", "EDIT");
        intent.putExtra("EXTRA_TYPE", activity.type == RecentActivity.Type.ORDER ? "BON" : "INVOICE");
        if (activity.originalObject instanceof com.chouchene.factures.entity.Invoice) {
            intent.putExtra("EXTRA_DOC_ID", ((com.chouchene.factures.entity.Invoice) activity.originalObject).id);
        }
        startActivity(intent);
    }

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
                csv.append(invoice.date != null ? sdf.format(invoice.date) : "").append(";")
                   .append(invoice.id).append(";")
                   .append(invoice.clientName != null ? invoice.clientName.replace(";", ",") : "").append(";")
                   .append(String.format(Locale.getDefault(), "%.2f", invoice.amount)).append(";")
                   .append(invoice.status != null ? invoice.status : "").append(";")
                   .append(invoice.type != null ? invoice.type : "").append("\n");
            }

            try {
                // 1. Create the temporary file for sharing
                File cachePath = new File(context.getCacheDir(), "exports");
                if (!cachePath.exists()) cachePath.mkdirs();
                String fileName = "export_invoices_" + System.currentTimeMillis() + ".csv";
                File tempFile = new File(cachePath, fileName);
                FileWriter writer = new FileWriter(tempFile);
                writer.write(csv.toString());
                writer.close();

                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    
                    // 2. Show choice dialog
                    String[] options = {getString(R.string.option_download), getString(R.string.option_share)};
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.action_export_csv)
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) {
                                    saveToDownloads(csv.toString(), fileName);
                                } else {
                                    shareFile(tempFile);
                                }
                            })
                            .show();
                });

            } catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(context, R.string.msg_csv_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveToDownloads(String content, String fileName) {
        Context context = getContext();
        if (context == null) return;

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (java.io.OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            os.write(content.getBytes());
                        }
                    }
                    Toast.makeText(context, R.string.msg_csv_saved, Toast.LENGTH_SHORT).show();
                }
            } else {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(content.getBytes());
                }
                Toast.makeText(context, R.string.msg_csv_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(context, R.string.msg_csv_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Context context = getContext();
        if (context == null) return;
        
        Uri contentUri = FileProvider.getUriForFile(context, "com.chouchene.factures.provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.action_export_csv)));
    }

    private void generateMonthlyReport() {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Invoice> invoices = db.getAllInvoices();
            Date now = new Date();
            String currentMonthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(now);
            String displayMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now);

            float totalIncome = 0;
            float totalExpenses = expenseDb.getMonthlyExpenses(now);
            StringBuilder tableRows = new StringBuilder();
            SimpleDateFormat tableSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            for (Invoice i : invoices) {
                String iMonth = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(i.date);
                if (currentMonthYear.equals(iMonth)) {
                    totalIncome += i.amount;
                    tableRows.append("<tr>")
                            .append("<td>").append(tableSdf.format(i.date)).append("</td>")
                            .append("<td>").append(i.clientName).append("</td>")
                            .append("<td>").append(i.type).append("</td>")
                            .append("<td>").append(String.format(Locale.getDefault(), "%.2f €", i.amount)).append("</td>")
                            .append("<td>").append(i.status).append("</td>")
                            .append("</tr>");
                }
            }

            float netProfit = totalIncome - totalExpenses;
            
            try {
                String html = loadHtmlFromAssets("monthly_report_template.html");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String userName = prefs.getString("User", "Utilisateur");

                html = html.replace("{{reportMonth}}", displayMonth)
                           .replace("{{totalIncome}}", String.format(Locale.getDefault(), "%.2f", totalIncome))
                           .replace("{{totalExpenses}}", String.format(Locale.getDefault(), "%.2f", totalExpenses))
                           .replace("{{netProfit}}", String.format(Locale.getDefault(), "%.2f", netProfit))
                           .replace("{{tableContent}}", tableRows.toString())
                           .replace("{{nomEmetteur}}", userName);

                File cachePath = new File(context.getCacheDir(), "reports");
                if (!cachePath.exists()) cachePath.mkdirs();
                File reportFile = new File(cachePath, "Bilan_" + currentMonthYear + ".pdf");
                final String finalHtml = html;

                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    
                    String[] options = {getString(R.string.option_download), getString(R.string.option_share)};
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                            .setTitle("Bilan Mensuel")
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) {
                                    savePdfToDownloads(finalHtml, reportFile.getName());
                                } else {
                                    createPdfFromHtml(finalHtml, reportFile);
                                }
                            })
                            .show();
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void savePdfToDownloads(String html, String fileName) {
        File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        File outFile = new File(downloadsDir, fileName);
        createPdfFromHtml(html, outFile, true);
    }

    private void createPdfFromHtml(String html, File outFile) {
        createPdfFromHtml(html, outFile, false);
    }

    private void createPdfFromHtml(String html, File outFile, boolean isSilentDownload) {
        WebView webView = new WebView(requireContext());
        webView.layout(0, 0, 1024, 1448);
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();

                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("BilanMensuel");
                    adapter.onLayout(null, attributes, null, new PrintResultCallbackShim.LayoutResultCallbackShim() {
                        @Override
                        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                            try {
                                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, null, new PrintResultCallbackShim.WriteResultCallbackShim() {
                                    @Override
                                    public void onWriteFinished(PageRange[] pages) {
                                        try {
                                            pfd.close();
                                            if (isSilentDownload) {
                                                if (getActivity() != null) {
                                                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Bilan enregistré dans Téléchargements", Toast.LENGTH_SHORT).show());
                                                }
                                            } else {
                                                shareReport(outFile);
                                            }
                                        } catch (IOException e) {
                                            Log.e("PDF", "Error closing PFD", e);
                                        }
                                    }
                                    @Override
                                    public void onWriteFailed(CharSequence error) {}
                                });
                            } catch (Exception e) {
                                Log.e("PDF", "Error", e);
                            }
                        }
                    }, null);
                }, 1000);
            }
        });
    }

    private void shareReport(File file) {
        Context context = getContext();
        if (context == null) return;
        Uri contentUri = FileProvider.getUriForFile(context, "com.chouchene.factures.provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Partager le bilan mensuel"));
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    @ColorInt
    private int resolveColor(Context context, int attr) {
        if (context == null) return Color.TRANSPARENT;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
