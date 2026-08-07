package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Booking;
import com.chouchene.factures.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddBookingBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editClientName, editPhone, editPickup, editDestination, editDate, editTime, editPrice;
    private Calendar calendar = Calendar.getInstance();
    private OnBookingAddedListener listener;

    public interface OnBookingAddedListener {
        void onBookingAdded();
    }

    public void setOnBookingAddedListener(OnBookingAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editClientName = view.findViewById(R.id.editClientName);
        editPhone = view.findViewById(R.id.editPhone);
        editPickup = view.findViewById(R.id.editPickup);
        editDestination = view.findViewById(R.id.editDestination);
        editDate = view.findViewById(R.id.editDate);
        editTime = view.findViewById(R.id.editTime);
        editPrice = view.findViewById(R.id.editPrice);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        editDate.setText(dateFmt.format(calendar.getTime()));
        editTime.setText(timeFmt.format(calendar.getTime()));

        editDate.setOnClickListener(v -> showDatePicker());
        editTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveBooking());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Choisir la date")
                .setSelection(calendar.getTimeInMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar selected = Calendar.getInstance();
            selected.setTimeInMillis(selection);
            calendar.set(Calendar.YEAR, selected.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, selected.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH));
            editDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
        });
        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Choisir l'heure")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, picker.getHour());
            calendar.set(Calendar.MINUTE, picker.getMinute());
            editTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime()));
        });
        picker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    private void saveBooking() {
        String name = editClientName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String pickup = editPickup.getText().toString().trim();
        String dest = editDestination.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();

        if (name.isEmpty()) { editClientName.setError("Requis"); return; }
        if (pickup.isEmpty()) { editPickup.setError("Requis"); return; }
        if (dest.isEmpty()) { editDestination.setError("Requis"); return; }

        double price = 0;
        try {
            if (!priceStr.isEmpty()) price = Double.parseDouble(priceStr);
        } catch (Exception ignored) {}

        Booking booking = new Booking(name, phone, pickup, dest, calendar.getTime(), "", price);
        
        AppDatabase db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
        Executors.newSingleThreadExecutor().execute(() -> {
            long id = db.bookingDao().insertBooking(booking);
            booking.id = (int) id;
            // Schedule notification
            NotificationHelper.scheduleBookingReminder(requireContext(), booking);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onBookingAdded();
                    dismiss();
                });
            }
        });
    }
}
