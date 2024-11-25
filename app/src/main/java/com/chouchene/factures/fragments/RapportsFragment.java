package com.chouchene.factures.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.media3.common.util.Util;
import androidx.room.Room;

import com.chouchene.factures.POJO.DailyIncome;
import com.chouchene.factures.POJO.MonthlyIncome;
import com.chouchene.factures.R;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.database.AppDatabase;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class RapportsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Rapports");
        View myView = inflater.inflate(R.layout.activity_rapports, container, false);
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.graph_svgrepo_com);

        BarChart barChart = myView.findViewById(R.id.barChart);

        // Example monthly prices
        List<BarEntry> entries = new ArrayList<>();
        // Sample data loop for daily income
        InvoiceDao db = Room.databaseBuilder(getContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().invoiceDao();
        List<MonthlyIncome> mouthlyIncomeList = db.getMonthlyIncomeTotals();
        int i = 0;
        float sumMounthly = 0;
        for (MonthlyIncome mouthlyIncome : mouthlyIncomeList) {
            entries.add(new BarEntry(i++, (float) mouthlyIncome.monthlyTotal));
            sumMounthly += mouthlyIncome.monthlyTotal;
        }

        float sumDaily = 0;
        List<DailyIncome> dailyIncomeList = db.getDailyIncomeTotals();
        for (DailyIncome dailyIncome : dailyIncomeList) {
            sumDaily += dailyIncome.dailyTotal;

        }

        TextView totalRevenuesDay = myView.findViewById(R.id.todayRevenue);
        TextView totalRevenusMonth = myView.findViewById(R.id.moisRevenue);

        totalRevenuesDay.setText(String.valueOf(sumDaily));
        totalRevenusMonth.setText(String.valueOf(sumMounthly));

        // Set up the X-axis labels
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        IndexAxisValueFormatter xAxisFormatter = new IndexAxisValueFormatter(months);
        xAxisFormatter.setValues(months);

// Set up the BarDataSet and BarData
        BarDataSet dataSet = new BarDataSet(entries, "Revenu par mois");
        dataSet.setColor(new ColorDrawable(Color.parseColor("#3784ff")).getColor());
        dataSet.setValueTextColor(Color.WHITE);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate(); // re
        return myView;
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
