package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.ExpenseAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Expense;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.concurrent.Executors;

public class ExpensesFragment extends Fragment implements ExpenseAdapter.OnExpenseActionListener {

    private AppDatabase db;
    private ExpenseAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;

    public ExpensesFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expenses_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.empty_state);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);

        adapter = new ExpenseAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadExpenses();

        fab.setOnClickListener(v -> {
            AddExpenseBottomSheet bottomSheet = new AddExpenseBottomSheet();
            bottomSheet.setOnExpenseAddedListener(this::loadExpenses);
            bottomSheet.show(getChildFragmentManager(), "ADD_EXPENSE");
        });
    }

    private void loadExpenses() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Expense> expenses = db.expenseDao().getAllExpenses();
            float total = 0;
            for (Expense e : expenses) total += e.amount;
            final float finalTotal = total;

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    android.widget.TextView txtTotal = getView().findViewById(R.id.txt_total_expenses_header);
                    if (txtTotal != null) animateNumber(txtTotal, finalTotal);

                    adapter.setData(expenses);
                    recyclerView.scheduleLayoutAnimation();
                    checkEmptyState();
                });
            }
        });
    }

    private void checkEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDeleteClick(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer cette dépense ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.expenseDao().deleteExpense(expense);
                        loadExpenses();
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void animateNumber(android.widget.TextView textView, float target) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, target);
        animator.setDuration(1000);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(String.format(java.util.Locale.getDefault(), "%.2f €", value));
        });
        animator.start();
    }

    @Override
    public void onEditClick(Expense expense) {
        AddExpenseBottomSheet bottomSheet = AddExpenseBottomSheet.newInstance(expense.id);
        bottomSheet.setOnExpenseAddedListener(this::loadExpenses);
        bottomSheet.show(getChildFragmentManager(), "EDIT_EXPENSE");
    }
}
