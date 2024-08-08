package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<Bills> billsArrayList;
    private OnBillClickListener onBillClickListener;

    public MyAdapter(Context context, ArrayList<Bills> billsArrayList, OnBillClickListener onBillClickListener) {
        this.context = context;
        this.billsArrayList = billsArrayList;
        this.onBillClickListener = onBillClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
        return new MyViewHolder(v, onBillClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Bills bills = billsArrayList.get(position);

        holder.title.setText(bills.getName());
        holder.amount.setText(bills.getAmount());
        holder.category.setText(bills.getCategory());
        holder.purchaseDate.setText(bills.getDate());
    }

    @Override
    public int getItemCount() {
        return billsArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnBillClickListener onBillClickListener;

        public MyViewHolder(@NonNull View itemView, OnBillClickListener onBillClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitle);
            category = itemView.findViewById(R.id.textviewCategory);
            amount = itemView.findViewById(R.id.textviewAmount);
            purchaseDate = itemView.findViewById(R.id.textviewDate);
            this.onBillClickListener = onBillClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onBillClickListener.onBillClick(getAdapterPosition());
        }
    }
    public interface OnBillClickListener {
        void onBillClick(int position);
    }

    // Método para actualizar la lista de facturas
    public void updateBills(ArrayList<Bills> newBills) {
        billsArrayList.clear();
        billsArrayList.addAll(newBills);
        notifyDataSetChanged();
    }


}