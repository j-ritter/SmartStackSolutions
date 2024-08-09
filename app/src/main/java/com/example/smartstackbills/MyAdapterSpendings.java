package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapterSpendings extends RecyclerView.Adapter<MyAdapterSpendings.MyViewHolder> {

    private Context context;
    private ArrayList<Spendings> spendingsArrayList;
    private OnSpendingClickListener onSpendingClickListener;

    public MyAdapterSpendings(Context context, ArrayList<Spendings> spendingsArrayList, OnSpendingClickListener onSpendingClickListener) {
        this.context = context;
        this.spendingsArrayList = spendingsArrayList;
        this.onSpendingClickListener = onSpendingClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
        return new MyViewHolder(v, onSpendingClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Spendings spendings = spendingsArrayList.get(position);

        holder.title.setText(spendings.getName());
        holder.amount.setText(spendings.getAmount());
        holder.category.setText(spendings.getCategory());
        holder.purchaseDate.setText(spendings.getDate());
    }

    @Override
    public int getItemCount() {
        return spendingsArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnSpendingClickListener onSpendingClickListener;

        public MyViewHolder(@NonNull View itemView, OnSpendingClickListener onSpendingClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleSpendings);
            category = itemView.findViewById(R.id.textviewCategorySpendings);
            amount = itemView.findViewById(R.id.textviewAmountSpendings);
            purchaseDate = itemView.findViewById(R.id.textviewDateSpendings);
            this.onSpendingClickListener = onSpendingClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onSpendingClickListener.onSpendingClick(getAdapterPosition());
        }
    }

    public interface OnSpendingClickListener {
        void onSpendingClick(int position);
    }

    // Method to update the list of spendings
    public void updateSpendings(ArrayList<Spendings> newSpendings) {
        spendingsArrayList.clear();
        spendingsArrayList.addAll(newSpendings);
        notifyDataSetChanged();
    }
}
