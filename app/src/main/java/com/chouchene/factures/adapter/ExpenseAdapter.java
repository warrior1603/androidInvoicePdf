package com.chouchene.factures.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.entity.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private final OnExpenseActionListener listener;

    public interface OnExpenseActionListener {
        void onDeleteClick(Expense expense);
        void onEditClick(Expense expense);
    }

    public ExpenseAdapter(OnExpenseActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Expense> data) {
        this.expenses = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.txtDescription.setText(expense.description);
        holder.txtCategory.setText(expense.category);
        holder.txtAmount.setText(String.format(Locale.getDefault(), "- %.2f €", expense.amount));
        
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.txtDate.setText(fmt.format(expense.date));

        holder.itemView.setOnClickListener(v -> listener.onEditClick(expense));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteClick(expense);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDescription, txtCategory, txtAmount, txtDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}
