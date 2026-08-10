package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(String period, String status, String type);
    }

    private OnFilterAppliedListener listener;

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup dateGroup = view.findViewById(R.id.filter_date_group);
        ChipGroup statusGroup = view.findViewById(R.id.filter_status_group);
        ChipGroup typeGroup = view.findViewById(R.id.filter_type_group);

        view.findViewById(R.id.btn_apply_filter).setOnClickListener(v -> {
            String period = getSelectedChipText(dateGroup);
            String status = getSelectedChipText(statusGroup);
            String type = getSelectedChipText(typeGroup);

            if (listener != null) {
                listener.onFilterApplied(period, status, type);
            }
            dismiss();
        });

        view.findViewById(R.id.btn_reset_filter).setOnClickListener(v -> {
            dateGroup.clearCheck();
            statusGroup.clearCheck();
            typeGroup.clearCheck();
            if (listener != null) {
                listener.onFilterApplied(null, null, null);
            }
            dismiss();
        });
    }

    private String getSelectedChipText(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id != View.NO_ID) {
            Chip chip = group.findViewById(id);
            return chip.getText().toString();
        }
        return null;
    }
}
