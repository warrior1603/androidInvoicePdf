package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Expense;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;
import java.util.concurrent.Executors;

public class AddExpenseBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editDescription, editAmount;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView editCategory;
    private TextView txtTitle;
    private MaterialButton btnSave, btnDelete;
    private OnExpenseAddedListener listener;
    private Expense existingExpense = null;
    private Integer expenseId = null;

    public interface OnExpenseAddedListener {
        void onExpenseAdded();
    }

    public static AddExpenseBottomSheet newInstance(Integer expenseId) {
        AddExpenseBottomSheet fragment = new AddExpenseBottomSheet();
        if (expenseId != null) {
            Bundle args = new Bundle();
            args.putInt("expense_id", expenseId);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void setOnExpenseAddedListener(OnExpenseAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            expenseId = getArguments().getInt("expense_id");
        }
        return inflater.inflate(R.layout.bottom_sheet_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTitle = view.findViewById(R.id.txt_title);
        editDescription = view.findViewById(R.id.edit_description);
        editAmount = view.findViewById(R.id.edit_amount);
        editCategory = view.findViewById(R.id.edit_category);
        btnSave = view.findViewById(R.id.btn_save);
        btnDelete = view.findViewById(R.id.btn_delete);

        AppDatabase db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();

        if (expenseId != null) {
            txtTitle.setText("Modifier la dépense");
            btnDelete.setVisibility(View.VISIBLE);
            Executors.newSingleThreadExecutor().execute(() -> {
                existingExpense = db.expenseDao().getExpenseById(expenseId);
                if (existingExpense != null && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        editDescription.setText(existingExpense.description);
                        editAmount.setText(String.valueOf(existingExpense.amount));
                        editCategory.setText(existingExpense.category, false);
                    });
                }
            });
        }

        btnSave.setOnClickListener(v -> {
            String desc = editDescription.getText().toString().trim();
            String amountStr = editAmount.getText().toString().trim();
            String category = editCategory.getText().toString().trim();

            if (desc.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                if (existingExpense != null) {
                    existingExpense.description = desc;
                    existingExpense.amount = amount;
                    existingExpense.category = category;
                    db.expenseDao().updateExpense(existingExpense);
                } else {
                    Expense expense = new Expense(desc, amount, new Date(), category);
                    db.expenseDao().insertExpense(expense);
                }
                
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (listener != null) listener.onExpenseAdded();
                        dismiss();
                    });
                }
            });
        });

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer")
                    .setMessage("Voulez-vous supprimer cette dépense ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            if (existingExpense != null) {
                                db.expenseDao().deleteExpense(existingExpense);
                                if (getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        if (listener != null) listener.onExpenseAdded();
                                        dismiss();
                                    });
                                }
                            }
                        });
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });
    }
}
