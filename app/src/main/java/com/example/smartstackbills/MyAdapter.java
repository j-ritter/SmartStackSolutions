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

    public MyAdapter(Context context, ArrayList<Bills> billsArrayList) {
        this.context = context;
        this.billsArrayList = billsArrayList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Bills bills = billsArrayList.get(position);

        holder.title.setText(bills.getName());
        holder.amount.setText(bills.getAmount());
        holder.category.setText(bills.getCategory());
        holder.paid.setText(bills.isPaid() ? "Sí" : "No");
        holder.vendor.setText(bills.getVendor());
        holder.comments.setText(bills.getComment());
        holder.purchaseDate.setText(bills.getDate());
    }

    @Override
    public int getItemCount() {
        return billsArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView title, category, vendor, amount, paid, purchaseDate, comments;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitle);
            category = itemView.findViewById(R.id.textviewCategory);
            vendor = itemView.findViewById(R.id.textviewVendor);
            amount = itemView.findViewById(R.id.textviewAmount);
            paid = itemView.findViewById(R.id.textviewPaid);
            purchaseDate = itemView.findViewById(R.id.textviewDate);
            comments = itemView.findViewById(R.id.textviewComments);
        }
    }
}
