package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
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
    private TextView txtTitle;
    private MaterialButton btnSave, btnDelete;
    private MaterialSwitch switchCancelled;
    private Calendar calendar = Calendar.getInstance();
    private OnBookingAddedListener listener;
    private Booking existingBooking;
    private int bookingId = -1;

    public interface OnBookingAddedListener {
        void onBookingAdded();
    }

    public static AddBookingBottomSheet newInstance(Integer bookingId) {
        AddBookingBottomSheet fragment = new AddBookingBottomSheet();
        if (bookingId != null) {
            Bundle args = new Bundle();
            args.putInt("booking_id", bookingId);
            fragment.setArguments(args);
        }
        return fragment;
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

        if (getArguments() != null) {
            bookingId = getArguments().getInt("booking_id", -1);
        }

        txtTitle = view.findViewById(R.id.txtSheetTitle);
        switchCancelled = view.findViewById(R.id.switchCancelled);
        editClientName = view.findViewById(R.id.editClientName);
        editPhone = view.findViewById(R.id.editPhone);
        editPickup = view.findViewById(R.id.editPickup);
        editDestination = view.findViewById(R.id.editDestination);
        editDate = view.findViewById(R.id.editDate);
        editTime = view.findViewById(R.id.editTime);
        editPrice = view.findViewById(R.id.editPrice);
        btnSave = view.findViewById(R.id.btnSave);
        btnDelete = view.findViewById(R.id.btnDelete);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        editDate.setText(dateFmt.format(calendar.getTime()));
        editTime.setText(timeFmt.format(calendar.getTime()));

        com.google.android.material.textfield.TextInputLayout clientInput = view.findViewById(R.id.client_input_layout);
        if (clientInput != null) {
            clientInput.setEndIconOnClickListener(v -> showClientPicker());
        }

        editDate.setOnClickListener(v -> showDatePicker());
        editTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveBooking());
        btnDelete.setOnClickListener(v -> confirmDelete());

        if (bookingId != -1) {
            switchCancelled.setVisibility(View.VISIBLE);
            loadExistingBooking();
        }
    }

    private void showClientPicker() {
        ClientPickerBottomSheet picker = new ClientPickerBottomSheet();
        picker.setOnClientSelectedListener(client -> {
            editClientName.setText(client.clientName);
            editPhone.setText(client.phone);
        });
        picker.show(getChildFragmentManager(), "CLIENT_PICKER");
    }

    private void loadExistingBooking() {
        AppDatabase db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        Executors.newSingleThreadExecutor().execute(() -> {
            Booking booking = db.bookingDao().getBookingById(bookingId);
            if (booking != null) {
                populateFields(booking);
            }
        });
    }

    private void populateFields(Booking booking) {
        existingBooking = booking;
        calendar.setTime(booking.dateTime);
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (txtTitle != null) txtTitle.setText(R.string.title_edit_booking);
                if (btnSave != null) btnSave.setText(R.string.action_update);
                if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
                
                editClientName.setText(booking.clientName);
                editPhone.setText(booking.clientPhone);
                editPickup.setText(booking.pickupLocation);
                editDestination.setText(booking.destinationLocation);
                editPrice.setText(String.valueOf(booking.estimatedPrice));
                
                switchCancelled.setChecked("Cancelled".equals(booking.status));

                SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
                editDate.setText(dateFmt.format(booking.dateTime));
                editTime.setText(timeFmt.format(booking.dateTime));
            });
        }
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

        String status = switchCancelled.isChecked() ? "Cancelled" : "Scheduled";

        AppDatabase db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
        
        final Booking booking;
        if (bookingId != -1 && existingBooking != null) {
            booking = existingBooking;
            booking.clientName = name;
            booking.clientPhone = phone;
            booking.pickupLocation = pickup;
            booking.destinationLocation = dest;
            booking.dateTime = calendar.getTime();
            booking.estimatedPrice = price;
            booking.status = status;
        } else {
            booking = new Booking(name, phone, pickup, dest, calendar.getTime(), status, price);
        }
        
        Executors.newSingleThreadExecutor().execute(() -> {
            if (bookingId != -1) {
                db.bookingDao().updateBooking(booking);
            } else {
                long id = db.bookingDao().insertBooking(booking);
                booking.id = (int) id;
            }
            
            // Re-schedule notification only if not cancelled
            if (!"Cancelled".equals(booking.status)) {
                NotificationHelper.scheduleBookingReminder(requireContext(), booking);
            }
            
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onBookingAdded();
                    dismiss();
                });
            }
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_delete)
                .setMessage(R.string.msg_confirm_delete_booking)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteBooking())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteBooking() {
        if (existingBooking == null) return;
        AppDatabase db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        Executors.newSingleThreadExecutor().execute(() -> {
            db.bookingDao().deleteBooking(existingBooking);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onBookingAdded();
                    dismiss();
                });
            }
        });
    }
}
