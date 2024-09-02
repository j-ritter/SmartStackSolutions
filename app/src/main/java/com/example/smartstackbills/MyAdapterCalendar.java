package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MyAdapterCalendar extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_BILL = 0;
    private static final int ITEM_INCOME = 1;

    private Context context;
    private ArrayList<Object> calendarEntries;
    private OnItemClickListener listener;

    public MyAdapterCalendar(Context context, ArrayList<Object> calendarEntries) {
        this.context = context;
        this.calendarEntries = calendarEntries;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (calendarEntries.get(position) instanceof Bills) {
            return ITEM_BILL;
        } else {
            return ITEM_INCOME;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_BILL) {
            View view = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
            return new BillViewHolder(view, listener);  // Pass listener here
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.items_income, parent, false);
            return new IncomeViewHolder(view, listener);  // Pass listener here
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_BILL) {
            Bills bill = (Bills) calendarEntries.get(position);
            BillViewHolder billViewHolder = (BillViewHolder) holder;
            billViewHolder.title.setText(bill.getName());
            billViewHolder.amount.setText(bill.getAmount());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(bill.getDate());
            billViewHolder.purchaseDate.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(bill.getDate());

        } else {
            Income income = (Income) calendarEntries.get(position);
            IncomeViewHolder incomeViewHolder = (IncomeViewHolder) holder;
            incomeViewHolder.title.setText(income.getName());
            incomeViewHolder.amount.setText(income.getAmount());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(income.getDate());
            incomeViewHolder.dateOfIncome.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(income.getDate());
        }
    }
    // Método para formatear el Timestamp a String
    private String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    // Método para formatear el Timestamp a mes y año
    private String formatMonthYear(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    @Override
    public int getItemCount() {
        return calendarEntries.size();
    }

    public Object getItemAtPosition(int position) {
        return calendarEntries.get(position);
    }

    public void updateEntries(ArrayList<Object> newEntries) {
        calendarEntries.clear();
        calendarEntries.addAll(newEntries);
        notifyDataSetChanged();
    }

    // ViewHolder for Bills
    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView title, amount, purchaseDate;

        public BillViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleItemsBills);
            amount = itemView.findViewById(R.id.textviewAmountItemsBills);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    // ViewHolder for Income
    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView title, amount, dateOfIncome;

        public IncomeViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleItemsIncome);
            amount = itemView.findViewById(R.id.textviewAmountItemsIncome);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}
