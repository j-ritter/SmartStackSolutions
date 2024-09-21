package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapterIncome extends RecyclerView.Adapter<MyAdapterIncome.MyViewHolder> {

    private Context context;
    private ArrayList<Income> incomeArrayList;
    private OnIncomeClickListener onIncomeClickListener;

    public MyAdapterIncome(Context context, ArrayList<Income> incomeArrayList, OnIncomeClickListener onIncomeClickListener) {
        this.context = context;
        this.incomeArrayList = incomeArrayList;
        this.onIncomeClickListener = onIncomeClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.items_income, parent, false);
        return new MyViewHolder(v, onIncomeClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Income income = incomeArrayList.get(position);

        holder.title.setText(income.getName());
        holder.amount.setText(income.getAmount());
        holder.category.setText(income.getCategory());
        holder.dateOfIncome.setText(income.getDate());
    }

    @Override
    public int getItemCount() {
        return incomeArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, dateOfIncome;
        OnIncomeClickListener onIncomeClickListener;

        public MyViewHolder(@NonNull View itemView, OnIncomeClickListener onIncomeClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleIncome);
            category = itemView.findViewById(R.id.textviewCategoryIncome);
            amount = itemView.findViewById(R.id.textviewAmountIncome);
            dateOfIncome = itemView.findViewById(R.id.textviewDateIncome);
            this.onIncomeClickListener = onIncomeClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onIncomeClickListener.onIncomeClick(getAdapterPosition());
        }
    }

    public interface OnIncomeClickListener {
        void onIncomeClick(int position);
    }

    // Method to update the list of income items
    public void updateIncome(ArrayList<Income> newIncome) {
        incomeArrayList.clear();
        incomeArrayList.addAll(newIncome);
        notifyDataSetChanged();
    }
}