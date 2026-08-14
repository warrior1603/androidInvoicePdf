package com.chouchene.factures.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Booking;
import com.chouchene.factures.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private MaterialButton btnSave, btnDelete, btnBack, btnNext, btnConvertToInvoice;
    private View mapTouchOverlay;
    private WebView webRoutePreview;
    private MaterialSwitch switchCancelled;
    private ViewFlipper viewFlipper;
    private View stepIndicator1, stepIndicator2, stepIndicator3;
    private int currentStep = 0;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            bookingId = getArguments().getInt("booking_id", -1);
        }

        setupInputs(view);
        setupStepper(view);

        if (bookingId != -1) {
            // Edit mode
            btnDelete.setVisibility(View.VISIBLE);
            switchCancelled.setVisibility(View.VISIBLE);
            loadExistingBooking();
        }
    }

    private void setupInputs(View view) {
        switchCancelled = view.findViewById(R.id.switchCancelled);
        
        editClientName = initItemWithAction(view.findViewById(R.id.item_client_name), R.drawable.ic_nav_user_outline, "Nom du client", android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME, this::showClientPicker);
        editPhone = initItem(view.findViewById(R.id.item_client_phone), R.drawable.ic_outline_phone, "Téléphone", android.text.InputType.TYPE_CLASS_PHONE);
        
        editPickup = initItem(view.findViewById(R.id.item_pickup), R.drawable.rounded_location_on_24, "Départ", android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        editDestination = initItem(view.findViewById(R.id.item_destination), R.drawable.rounded_location_on_24, "Arrivée", android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        
        editDate = initItem(view.findViewById(R.id.item_date), R.drawable.rounded_calendar_today_24, "Date", android.text.InputType.TYPE_NULL);
        editTime = initItem(view.findViewById(R.id.item_time), R.drawable.ic_clock_outline, "Heure", android.text.InputType.TYPE_NULL);
        
        editPrice = initItem(view.findViewById(R.id.item_price), R.drawable.rounded_payments_24, "Tarif TTC (€)", android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        btnSave = view.findViewById(R.id.btnSave);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnConvertToInvoice = view.findViewById(R.id.btnConvertToInvoice);
        
        webRoutePreview = view.findViewById(R.id.webRoutePreview);
        mapTouchOverlay = view.findViewById(R.id.mapTouchOverlay);

        initMapPreview();

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        editDate.setText(dateFmt.format(calendar.getTime()));
        editTime.setText(timeFmt.format(calendar.getTime()));

        editDate.setOnClickListener(v -> showDatePicker());
        editTime.setOnClickListener(v -> showTimePicker());

        if (mapTouchOverlay != null) {
            mapTouchOverlay.setOnClickListener(v -> openRouteInMaps());
        }

        TextWatcher routeWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateRoutePreview(); }
        };
        editPickup.addTextChangedListener(routeWatcher);
        editDestination.addTextChangedListener(routeWatcher);
    }

    private com.google.android.material.textfield.TextInputEditText initItem(View itemView, int iconRes, String label, int inputType) {
        android.widget.ImageView icon = itemView.findViewById(R.id.item_icon);
        android.widget.TextView txtLabel = itemView.findViewById(R.id.item_label);
        com.google.android.material.textfield.TextInputEditText input = itemView.findViewById(R.id.item_input);

        icon.setImageResource(iconRes);
        txtLabel.setText(label);
        
        // Use black for labels to match Document Studio
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            txtLabel.setTextColor(typedValue.data);
            txtLabel.setAlpha(0.9f);
        } catch (Exception ignored) {}

        input.setHint(label);
        input.setInputType(inputType);
        return input;
    }

    private com.google.android.material.textfield.TextInputEditText initItemWithAction(View itemView, int iconRes, String label, int inputType, Runnable action) {
        com.google.android.material.textfield.TextInputEditText input = initItem(itemView, iconRes, label, inputType);
        View actionIcon = itemView.findViewById(R.id.item_action_icon);
        if (actionIcon != null) {
            actionIcon.setVisibility(View.VISIBLE);
            actionIcon.setOnClickListener(v -> action.run());
        }
        return input;
    }

    private void initMapPreview() {
        if (webRoutePreview == null) return;
        
        webRoutePreview.getSettings().setJavaScriptEnabled(true);
        webRoutePreview.getSettings().setDomStorageEnabled(true);
        webRoutePreview.getSettings().setDatabaseEnabled(true);
        webRoutePreview.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        webRoutePreview.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        webRoutePreview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        webRoutePreview.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) MesFactures/1.1");
        webRoutePreview.setWebViewClient(new WebViewClient());
        
        String html = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>" +
                "  body, html, #map { height: 100%; margin: 0; padding: 0; background: #F8FAFC; }" +
                "  .leaflet-container { background: #F8FAFC !important; }" +
                "  .leaflet-control-attribution { display: none !important; }" +
                "</style>" +
                "</head><body><div id=\"map\"></div>" +
                "<script>" +
                "var map = L.map('map', {zoomControl: false, attributionControl: false}).setView([46.603354, 1.888334], 5);" + 
                "L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {" +
                "    maxZoom: 19" +
                "}).addTo(map);" +
                "var markers = [];" +
                "var routeLayer = null;" +
                "function updateMarkers(pickup, dest) {" +
                "    markers.forEach(m => map.removeLayer(m)); markers = [];" +
                "    if(routeLayer) map.removeLayer(routeLayer); routeLayer = null;" +
                "    var group = new L.featureGroup();" +
                "    var p1 = pickup ? fetch('https://nominatim.openstreetmap.org/search?format=json&q='+encodeURIComponent(pickup)).then(r=>r.json()) : Promise.resolve([]);" +
                "    var p2 = dest ? fetch('https://nominatim.openstreetmap.org/search?format=json&q='+encodeURIComponent(dest)).then(r=>r.json()) : Promise.resolve([]);" +
                "    Promise.all([p1, p2]).then(results => {" +
                "        var d1 = results[0][0]; var d2 = results[1][0];" +
                "        if(d1) { var m = L.marker([d1.lat, d1.lon]).addTo(map); markers.push(m); group.addLayer(m); }" +
                "        if(d2) { var m = L.marker([d2.lat, d2.lon]).addTo(map); markers.push(m); group.addLayer(m); }" +
                "        if(d1 && d2) {" +
                "            fetch('https://router.project-osrm.org/route/v1/driving/' + d1.lon + ',' + d1.lat + ';' + d2.lon + ',' + d2.lat + '?overview=full&geometries=geojson')" +
                "            .then(r=>r.json()).then(data => {" +
                "                if(data.routes && data.routes[0]) {" +
                "                    routeLayer = L.geoJSON(data.routes[0].geometry, {style: {color: '#4F46E5', weight: 5, opacity: 0.8, lineJoin: 'round'}}).addTo(map);" +
                "                    map.fitBounds(routeLayer.getBounds(), {padding: [40, 40]});" +
                "                }" +
                "            });" +
                "        } else if(markers.length > 0) {" +
                "            map.fitBounds(group.getBounds(), {padding: [30, 30]});" +
                "        }" +
                "    }).catch(e => console.log(e));" +
                "}" +
                "</script></body></html>";
        
        webRoutePreview.loadDataWithBaseURL("https://app.mesfactures.local", html, "text/html", "UTF-8", null);
    }

    private void updateRoutePreview() {
        String from = editPickup.getText().toString().trim();
        String to = editDestination.getText().toString().trim();
        if (from.isEmpty() && to.isEmpty()) return;

        if (webRoutePreview != null) {
            webRoutePreview.evaluateJavascript("updateMarkers('" + from.replace("'", "\\'") + "', '" + to.replace("'", "\\'") + "')", null);
        }
    }

    private void openRouteInMaps() {
        String from = editPickup.getText().toString().trim();
        String to = editDestination.getText().toString().trim();
        if (from.isEmpty() || to.isEmpty()) return;
        Uri uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + Uri.encode(from) + "&destination=" + Uri.encode(to) + "&travelmode=driving");
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) startActivity(intent);
        else startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
    }

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back_booking);
        btnNext = view.findViewById(R.id.btn_next_booking);
        btnSave = view.findViewById(R.id.btnSave);

        stepIndicator1 = view.findViewById(R.id.step_indicator_1);
        stepIndicator2 = view.findViewById(R.id.step_indicator_2);
        stepIndicator3 = view.findViewById(R.id.step_indicator_3);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnSave.setOnClickListener(v -> saveBooking());
        btnDelete.setOnClickListener(v -> confirmDelete());

        updateStepperUI();
    }

    private void goToNextStep() {
        if (currentStep == 0) {
            if (editClientName.getText().toString().trim().isEmpty()) {
                editClientName.setError("Requis"); return;
            }
        } else if (currentStep == 1) {
            if (editPickup.getText().toString().trim().isEmpty()) {
                editPickup.setError("Requis"); return;
            }
            if (editDestination.getText().toString().trim().isEmpty()) {
                editDestination.setError("Requis"); return;
            }
        }
        
        if (currentStep < 2) {
            currentStep++;
            viewFlipper.setInAnimation(requireContext(), R.anim.slide_in_right);
            viewFlipper.setOutAnimation(requireContext(), R.anim.slide_out_left);
            viewFlipper.showNext();
            updateStepperUI();
        }
    }

    private void goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left);
            viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right);
            viewFlipper.showPrevious();
            updateStepperUI();
        }
    }

    private void updateStepperUI() {
        btnBack.setVisibility(currentStep == 0 ? View.GONE : View.VISIBLE);
        btnNext.setVisibility(currentStep == 2 ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        
        if (btnConvertToInvoice != null) {
            btnConvertToInvoice.setVisibility(currentStep == 2 && bookingId != -1 ? View.VISIBLE : View.GONE);
            btnConvertToInvoice.setOnClickListener(v -> convertToInvoice());
        }

        updateIndicator(stepIndicator1, currentStep >= 0);
        updateIndicator(stepIndicator2, currentStep >= 1);
        updateIndicator(stepIndicator3, currentStep >= 2);
        
        if (currentStep == 2) {
            updateSummary();
        }
    }

    private void updateIndicator(View bar, boolean active) {
        bar.setAlpha(active ? 1.0f : 0.2f);
    }

    private void updateSummary() {
        String name = editClientName.getText().toString();
        String pickup = editPickup.getText().toString();
        String dest = editDestination.getText().toString();
        String date = editDate.getText().toString();
        
        String summary = "Course pour: " + name + "\n" +
                        "De: " + pickup + "\n" +
                        "À: " + dest + "\n" +
                        "Le: " + date;
        
        if (getView() != null) {
            TextView summaryView = getView().findViewById(R.id.summary_text_booking);
            if (summaryView != null) summaryView.setText(summary);
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
                    if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                        ((com.chouchene.factures.MainActivity) getActivity()).updateBottomNavBadges();
                    }
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

    private void convertToInvoice() {
        String name = editClientName.getText().toString();
        String pickup = editPickup.getText().toString();
        String dest = editDestination.getText().toString();
        String priceStr = editPrice.getText().toString();
        double price = 0;
        try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}

        String desc = "Transport VTC: " + pickup + " -> " + dest;
        
        dismiss(); // Close booking sheet
        
        CreateInvoiceBottomSheet invoiceSheet = CreateInvoiceBottomSheet.newInstance(name, desc, price);
        invoiceSheet.show(requireActivity().getSupportFragmentManager(), "CONVERT_INVOICE");
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
