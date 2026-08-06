package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Expense;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;
import java.util.concurrent.Executors;

public class AddExpenseBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editDescription, editAmount;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView editCategory;
    private OnExpenseAddedListener listener;

    public interface OnExpenseAddedListener {
        void onExpenseAdded();
    }

    public void setOnExpenseAddedListener(OnExpenseAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editDescription = view.findViewById(R.id.edit_description);
        editAmount = view.findViewById(R.id.edit_amount);
        editCategory = view.findViewById(R.id.edit_category);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            String desc = editDescription.getText().toString();
            String amountStr = editAmount.getText().toString();
            String category = editCategory.getText().toString();

            if (desc.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(desc, amount, new Date(), category);

            AppDatabase db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
            Executors.newSingleThreadExecutor().execute(() -> {
                db.expenseDao().insertExpense(expense);
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (listener != null) listener.onExpenseAdded();
                        dismiss();
                    });
                }
            });
        });
    }
}
