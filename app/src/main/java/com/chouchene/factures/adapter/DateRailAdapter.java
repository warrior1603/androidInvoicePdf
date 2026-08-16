package com.chouchene.factures.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chouchene.factures.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateRailAdapter extends RecyclerView.Adapter<DateRailAdapter.ViewHolder> {

    private final List<Date> dates = new ArrayList<>();
    private Date selectedDate;
    private final OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    public DateRailAdapter(OnDateSelectedListener listener) {
        this.listener = listener;
        generateDates();
    }

    private void generateDates() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -15); // Start 15 days ago
        for (int i = 0; i < 45; i++) { // Show 45 days range
            dates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        selectedDate = new Date(); // Default today
    }

    public void setSelectedDate(Date date) {
        this.selectedDate = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agenda_date_rail, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Date date = dates.get(position);
        SimpleDateFormat dayNumFmt = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat dayNameFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        holder.txtDayNum.setText(dayNumFmt.format(date));
        holder.txtDayName.setText(dayNameFmt.format(date).toUpperCase());

        boolean isSelected = isSameDay(date, selectedDate);
        holder.aura.setAlpha(isSelected ? 0.08f : 0f);
        holder.txtDayNum.setTextColor(isSelected ? holder.itemView.getContext().getColor(R.color.primary) : Color.parseColor("#1E293B"));
        holder.txtDayName.setAlpha(isSelected ? 0.8f : 0.4f);

        holder.itemView.setOnClickListener(v -> {
            selectedDate = date;
            notifyDataSetChanged();
            listener.onDateSelected(date);
        });
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance(); cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance(); cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemCount() { return dates.size(); }

    public int getPositionForDate(Date date) {
        for (int i = 0; i < dates.size(); i++) {
            if (isSameDay(dates.get(i), date)) return i;
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDayNum, txtDayName;
        View aura, dot;
        ViewHolder(View v) {
            super(v);
            txtDayNum = v.findViewById(R.id.txt_day_num);
            txtDayName = v.findViewById(R.id.txt_day_name);
            aura = v.findViewById(R.id.date_aura);
            dot = v.findViewById(R.id.dot_has_events);
        }
    }
}
