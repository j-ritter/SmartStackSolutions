package com.ritter.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MyAdapterCalendar extends RecyclerView.Adapter<MyAdapterCalendar.EntryViewHolder> {
    private final Context context;
    private final ArrayList<Object> entries;
    private OnItemClickListener listener;

    public MyAdapterCalendar(Context context, ArrayList<Object> entries) {
        this.context = context;
        this.entries = new ArrayList<>(entries);
    }

    public interface OnItemClickListener { void onItemClick(int position); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.listener = listener; }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_calendar_entry, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Object entry = entries.get(position);
        String title = "";
        String category = "";
        double amount = 0;
        int color;
        int typeText;
        if (entry instanceof Bills) {
            Bills bill = (Bills) entry;
            title = safe(bill.getName());
            category = safe(bill.getCategory());
            amount = bill.getAmount();
            boolean overdue = bill.getDate() != null &&
                    AppDateUtils.INSTANCE.isBeforeToday(bill.getDate().toDate()) && !bill.isPaid();
            color = overdue ? R.color.red : R.color.bill_color;
            typeText = overdue ? R.string.overdue_open_payment : R.string.open_payment;
        } else if (entry instanceof Spendings) {
            Spendings spending = (Spendings) entry;
            title = safe(spending.getName());
            category = safe(spending.getCategory());
            amount = spending.getAmount();
            color = R.color.spending_color;
            typeText = R.string.closed_payment;
        } else {
            Income income = (Income) entry;
            title = safe(income.getName());
            category = safe(income.getCategory());
            amount = income.getAmount();
            color = R.color.income_color;
            typeText = R.string.income;
        }
        holder.type.setText(typeText);
        holder.title.setText(title.isEmpty() ? context.getString(R.string.untitled_entry) : title);
        holder.category.setText(category.isEmpty() ? context.getString(R.string.no_category) : category);
        holder.amount.setText(CurrencyPreferences.format(context, amount));
        holder.amount.setTextColor(ContextCompat.getColor(context, color));
        holder.typeBar.setBackgroundColor(ContextCompat.getColor(context, color));
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(adapterPosition);
            }
        });
    }

    @Override public int getItemCount() { return entries.size(); }
    public Object getItemAtPosition(int position) { return entries.get(position); }
    public void updateEntries(ArrayList<Object> newEntries) {
        ArrayList<Object> snapshot = new ArrayList<>(newEntries);
        entries.clear();
        entries.addAll(snapshot);
        notifyDataSetChanged();
    }
    private String safe(String value) { return value == null ? "" : value.trim(); }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        View typeBar;
        TextView type, title, category, amount;
        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            typeBar = itemView.findViewById(R.id.calendarTypeBar);
            type = itemView.findViewById(R.id.calendarEntryType);
            title = itemView.findViewById(R.id.calendarEntryTitle);
            category = itemView.findViewById(R.id.calendarEntryCategory);
            amount = itemView.findViewById(R.id.calendarEntryAmount);
        }
    }
}
