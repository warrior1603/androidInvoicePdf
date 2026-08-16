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
import com.chouchene.factures.fragments.DocumentsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
    private LineChart lineChart;
    private BarChart barChart;
    private TextView txtGoalCurrent, txtGoalTarget;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressRevenueGoal;

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
        lineChart = view.findViewById(R.id.lineChart);
        barChart = view.findViewById(R.id.barChart);
        txtGoalCurrent = view.findViewById(R.id.txt_goal_current);
        txtGoalTarget = view.findViewById(R.id.txt_goal_target);
        progressRevenueGoal = view.findViewById(R.id.progress_revenue_goal);
        
        View cardExpenses = view.findViewById(R.id.cardExpenses);
        View btnExportCsv = view.findViewById(R.id.btnExportCsv);
        View btnGenerateReportPdf = view.findViewById(R.id.btnGenerateReportPdf);

        cardExpenses.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.expensesFragment));
        btnExportCsv.setOnClickListener(v -> exportToCSV());
        btnGenerateReportPdf.setOnClickListener(v -> generateMonthlyReport());

        com.chouchene.factures.utils.UIUtils.applyClickScale(cardExpenses);
        com.chouchene.factures.utils.UIUtils.applyClickScale(btnExportCsv);
        com.chouchene.factures.utils.UIUtils.applyClickScale(btnGenerateReportPdf);

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
        TextView growthValTxt = getView().findViewById(R.id.txt_growth_val);
        TextView briefingTxt = getView().findViewById(R.id.txt_dashboard_briefing);
        TextView docTrendTxt = getView().findViewById(R.id.txt_doc_count_trend);
        TextView clientTrendTxt = getView().findViewById(R.id.txt_client_count_trend);
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

            Calendar cal = Calendar.getInstance();

            switch (timeframe) {
                case DAILY:
                    float dailyIncome = db.getDailyIncome(today);
                    float dailyExpenses = expenseDb.getDailyExpenses(today);
                    revenue = dailyIncome - dailyExpenses;
                    count = db.getDailyCount(today);
                    labelTop = "Bénéfice du jour";

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
            final int finalClientCount = clientCount;
            final List<BarEntry> finalChartEntries = chartEntries;
            final List<String> finalLabels = labels;

            // Trend Calculations for Bento
            String docTrend = "Stable";
            String clientTrend = "Stable";
            
            // Simple logic for doc trend (current vs previous if available)
            // For now, let's just use the revenue growth logic as a placeholder for briefing
            String briefingText = "Performance stable";
            if (revenue > prevRevenue && prevRevenue > 0) {
                briefingText = "VOTRE MEILLEUR RÉSULTAT SUR CETTE PÉRIODE";
            } else if (revenue < prevRevenue) {
                briefingText = "VIGILANCE : RÉSULTAT EN BAISSE";
            } else {
                briefingText = "ANALYSE DE PERFORMANCE";
            }

            final String finalBriefing = briefingText;

            float totalIncome = 0;
            if (timeframe == Timeframe.DAILY) totalIncome = db.getDailyIncome(today);
            else if (timeframe == Timeframe.MONTHLY) totalIncome = db.getMonthlyIncome(today);
            else if (timeframe == Timeframe.YEARLY) totalIncome = db.getYearlyIncome(today);
            else totalIncome = db.getTotalRevenue();

            final float incomeForMargin = totalIncome;
            final float monthlyProfitGoal = db.getMonthlyIncome(today) - expenseDb.getMonthlyExpenses(today);

            activity.runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                
                if (shimmerContainer != null) {
                    shimmerContainer.stopShimmer();
                    shimmerContainer.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                    btnExportCsv.setVisibility(View.VISIBLE);
                }
                revenueLabel.setText(finalLabelTop.toUpperCase());
                briefingTxt.setText(finalBriefing);
                animateNumber(totalRevenueTxt, finalRev);
                documentCountTxt.setText(String.valueOf(finalCount));

                if (txtGoalCurrent != null) txtGoalCurrent.setText(String.format(Locale.getDefault(), "%.2f €", monthlyProfitGoal));
                if (progressRevenueGoal != null) {
                    float target = 5000f; // Default monthly target
                    int progress = (int) ((monthlyProfitGoal / target) * 100);
                    progressRevenueGoal.setProgress(Math.min(100, Math.max(0, progress)));
                }

                totalClientsTxt.setText(String.valueOf(finalClientCount));

                // Trends in Bento
                docTrendTxt.setText(finalCount > 0 ? "+ " + finalCount + " docs" : "Aucun doc");
                clientTrendTxt.setText(finalClientCount > 0 ? "Total de " + finalClientCount : "Nouveau");

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
                    growthValTxt.setTextColor(ContextCompat.getColor(context, R.color.status_paid));
                }

                setupChart(finalChartEntries, finalLabels, chartEmptyState);

                // Update Margin
                if (incomeForMargin > 0) {
                    int marginPercent = (int) ((finalRev / incomeForMargin) * 100);
                    if (marginPercent < 0) marginPercent = 0;
                    progressMargin.setProgress(marginPercent, true);
                    txtMarginLabel.setText(marginPercent + "%");
                } else {
                    progressMargin.setProgress(0, true);
                    txtMarginLabel.setText("0%");
                }
            });
        });
    }

    private void setupChart(List<BarEntry> entries, List<String> labels, View emptyState) {
        boolean hasData = false;
        for (BarEntry e : entries) {
            if (e.getY() > 0) { hasData = true; break; }
        }

        if (!hasData) {
            lineChart.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        
        if (timeframe == Timeframe.YEARLY || timeframe == Timeframe.ALL_TIME) {
            lineChart.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.GONE);
            setupLineChart(entries, labels);
        } else {
            lineChart.setVisibility(View.GONE);
            barChart.setVisibility(View.VISIBLE);
            setupBarChart(entries, labels);
        }
    }

    private void setupLineChart(List<BarEntry> barEntries, List<String> labels) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (BarEntry be : barEntries) {
            entries.add(new Entry(be.getX(), be.getY(), be.getData()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Performance");
        int primaryColor = resolveColor(getContext(), androidx.appcompat.R.attr.colorPrimary);

        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(primaryColor);
        dataSet.setDrawCircleHole(false);
        dataSet.setLineWidth(3f);
        dataSet.setColor(primaryColor);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(primaryColor);
        
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.revenue_gradient);
        if (drawable != null) dataSet.setFillDrawable(drawable);
        else dataSet.setFillColor(primaryColor);

        LineData data = new LineData(dataSet);
        data.setDrawValues(false);
        
        lineChart.setData(data);
        styleChartBase(lineChart, labels);
        lineChart.animateY(1000);

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getData() instanceof DocumentsViewModel.Filter) {
                    showInvoicesBottomSheet((DocumentsViewModel.Filter) e.getData());
                }
            }
            @Override public void onNothingSelected() {}
        });

        lineChart.invalidate();
    }

    private void setupBarChart(List<BarEntry> entries, List<String> labels) {
        int primaryColor = resolveColor(getContext(), androidx.appcompat.R.attr.colorPrimary);
        BarDataSet dataSet = new BarDataSet(entries, "Revenu");
        dataSet.setColor(primaryColor);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(primaryColor);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        styleChartBase(barChart, labels);
        barChart.animateY(1000);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getData() instanceof DocumentsViewModel.Filter) {
                    showInvoicesBottomSheet((DocumentsViewModel.Filter) e.getData());
                }
            }
            @Override public void onNothingSelected() {}
        });

        barChart.invalidate();
    }

    private void styleChartBase(BarLineChartBase<?> chart, List<String> labels) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);

        // Add Marker (Precision Suggestion 1)
        CustomMarkerView marker = new CustomMarkerView(requireContext(), R.layout.layout_chart_marker);
        chart.setMarker(marker);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(labels.size());
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(resolveColor(getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
        xAxis.setTextSize(10f);
        xAxis.setYOffset(10f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.argb(20, 0, 0, 0));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setLabelCount(4, false);
        leftAxis.setTextColor(resolveColor(getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
        leftAxis.setTextSize(10f);
        leftAxis.setXOffset(10f);
        leftAxis.setAxisMinimum(0f);

        float avg = 0;
        if (chart.getData() != null) {
            avg = chart.getData().getYMax() / 2; // Simple half-way benchmark for demo
            // Better: calculate real average from entries
        }
        
        com.github.mikephil.charting.components.LimitLine ll = new com.github.mikephil.charting.components.LimitLine(avg, "");
        ll.setLineColor(Color.LTGRAY);
        ll.setLineWidth(1f);
        ll.enableDashedLine(10f, 10f, 0f);
        ll.setLabelPosition(com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextSize(8f);
        
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll);

        chart.getAxisRight().setEnabled(false);
    }

    public class CustomMarkerView extends com.github.mikephil.charting.components.MarkerView {
        private TextView tvContent;
        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            tvContent = findViewById(R.id.txt_marker_val);
        }
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            tvContent.setText(String.format(Locale.getDefault(), "%.2f €", e.getY()));
            super.refreshContent(e, highlight);
        }
        @Override
        public com.github.mikephil.charting.utils.MPPointF getOffset() {
            return new com.github.mikephil.charting.utils.MPPointF(-(getWidth() / 2), -getHeight());
        }
    }

    private void showInvoicesBottomSheet(DocumentsViewModel.Filter filter) {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = getLayoutInflater().inflate(R.layout.layout_analytics_invoices_bottom_sheet, null);
        
        TextView title = view.findViewById(R.id.txt_bottom_sheet_title);
        title.setText(filter.label != null ? filter.label : "Détails");
        
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

    private void animateNumber(TextView textView, float target) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, target);
        animator.setDuration(1000);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(String.format(Locale.getDefault(), "%.2f €", value));
        });
        animator.start();
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
                File cachePath = new File(context.getCacheDir(), "exports");
                if (!cachePath.exists()) cachePath.mkdirs();
                String fileName = "export_invoices_" + System.currentTimeMillis() + ".csv";
                File tempFile = new File(cachePath, fileName);
                FileWriter writer = new FileWriter(tempFile);
                writer.write(csv.toString());
                writer.close();

                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    
                    BottomSheetDialog dialog = new BottomSheetDialog(context);
                    View sheetView = getLayoutInflater().inflate(R.layout.layout_export_options_sheet, null);
                    
                    ((TextView) sheetView.findViewById(R.id.txt_sheet_title)).setText("Exporter en CSV");
                    
                    sheetView.findViewById(R.id.btn_download).setOnClickListener(v -> {
                        dialog.dismiss();
                        saveToDownloads(csv.toString(), fileName);
                    });
                    
                    sheetView.findViewById(R.id.btn_share).setOnClickListener(v -> {
                        dialog.dismiss();
                        shareFile(tempFile);
                    });
                    
                    dialog.setContentView(sheetView);
                    dialog.show();
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
                        if (os != null) os.write(content.getBytes());
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
                    
                    BottomSheetDialog dialog = new BottomSheetDialog(context);
                    View sheetView = getLayoutInflater().inflate(R.layout.layout_export_options_sheet, null);
                    
                    ((TextView) sheetView.findViewById(R.id.txt_sheet_title)).setText("Bilan Mensuel");
                    
                    sheetView.findViewById(R.id.btn_download).setOnClickListener(v -> {
                        dialog.dismiss();
                        savePdfToDownloads(finalHtml, reportFile.getName());
                    });
                    
                    sheetView.findViewById(R.id.btn_share).setOnClickListener(v -> {
                        dialog.dismiss();
                        createPdfFromHtml(finalHtml, reportFile);
                    });
                    
                    dialog.setContentView(sheetView);
                    dialog.show();
                });
            } catch (IOException e) { e.printStackTrace(); }
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
                                                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Bilan enregistré dans Téléchargements", Toast.LENGTH_SHORT).show());
                                            } else shareReport(outFile);
                                        } catch (IOException e) { Log.e("PDF", "Error closing PFD", e); }
                                    }
                                    @Override public void onWriteFailed(CharSequence error) {}
                                });
                            } catch (Exception e) { Log.e("PDF", "Error", e); }
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
