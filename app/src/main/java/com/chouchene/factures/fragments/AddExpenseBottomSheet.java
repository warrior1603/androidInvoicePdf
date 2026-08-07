package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddExpenseBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editDescription, editAmount;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView editCategory;
    private TextView txtTitle;
    private MaterialButton btnSave, btnDelete;
    private OnExpenseAddedListener listener;
    private Expense existingExpense = null;
    private Integer expenseId = null;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        processReceipt(photo);
                    }
                }
            }
    );

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
        MaterialButton btnScan = view.findViewById(R.id.btn_scan_receipt);

        btnScan.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

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

    private void processReceipt(Bitmap bitmap) {
        if (getContext() == null) return;
        Toast.makeText(requireContext(), "Analyse de l'IA en cours...", Toast.LENGTH_SHORT).show();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::parseReceiptText)
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Error", e);
                    if (getContext() != null) Toast.makeText(requireContext(), "Échec de l'analyse", Toast.LENGTH_SHORT).show();
                });
    }

    private void parseReceiptText(Text visionText) {
        String fullText = visionText.getText();
        
        String detectedMerchant = "";
        if (!visionText.getTextBlocks().isEmpty()) {
            detectedMerchant = visionText.getTextBlocks().get(0).getText().split("\n")[0];
        }

        double maxAmount = 0;
        Pattern amountPattern = Pattern.compile("(\\d+[.,]\\d{2})");
        Matcher matcher = amountPattern.matcher(fullText);
        
        List<Double> foundAmounts = new ArrayList<>();
        while (matcher.find()) {
            try {
                String group = matcher.group(1);
                if (group != null) {
                    String val = group.replace(",", ".");
                    foundAmounts.add(Double.parseDouble(val));
                }
            } catch (Exception ignored) {}
        }

        if (!foundAmounts.isEmpty()) {
            for (Double a : foundAmounts) {
                if (a > maxAmount) maxAmount = a;
            }
        }

        if (!detectedMerchant.isEmpty()) editDescription.setText(detectedMerchant);
        if (maxAmount > 0) editAmount.setText(String.format(Locale.getDefault(), "%.2f", maxAmount));

        // 3. Auto-category selection based on keywords
        String textLower = fullText.toLowerCase();
        if (textLower.contains("station") || textLower.contains("gasoil") || textLower.contains("diesel") || textLower.contains("essence") || textLower.contains("totalenergies")) {
            editCategory.setText("Carburant", false);
        } else if (textLower.contains("garage") || textLower.contains("reparation") || textLower.contains("entretien") || textLower.contains("pneu")) {
            editCategory.setText("Entretien", false);
        } else if (textLower.contains("parking") || textLower.contains("peage")) {
            editCategory.setText("Frais de route", false);
        }

        // 4. Try to find a date (simple dd/MM/yyyy or dd.MM.yyyy)
        Pattern datePattern = Pattern.compile("(\\d{2}[/.-]\\d{2}[/.-]\\d{4})");
        Matcher dateMatcher = datePattern.matcher(fullText);
        if (dateMatcher.find()) {
            Log.d("OCR_DATE", "Found date: " + dateMatcher.group(1));
            // We could update the date if we had a date field, but for now we focus on description and amount
        }
        
        if (getContext() != null) Toast.makeText(requireContext(), "Analyse terminée !", Toast.LENGTH_SHORT).show();
    }
}
