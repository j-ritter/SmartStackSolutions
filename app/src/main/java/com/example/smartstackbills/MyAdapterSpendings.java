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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyAdapterSpendings extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private OnSpendingClickListener onSpendingClickListener;

    private static final int ITEM_SPENDING = 0;
    private static final int ITEM_MONTH_HEADER = 1;

    public MyAdapterSpendings(Context context, ArrayList<Spendings> spendingsArrayList, OnSpendingClickListener onSpendingClickListener) {
        this.context = context;

        this.itemsArrayList = groupSpendingsByMonth(spendingsArrayList);
        this.onSpendingClickListener = onSpendingClickListener;
    }
    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof String) {
            return ITEM_MONTH_HEADER;
        } else {
            return ITEM_SPENDING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SPENDING) {
            View v = LayoutInflater.from(context).inflate(R.layout.items_spendings, parent, false);
            return new SpendingViewHolder(v, onSpendingClickListener);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
            return new MyAdapterSpendings.MonthHeaderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_SPENDING) {
            SpendingViewHolder spendingHolder = (SpendingViewHolder) holder;
            Spendings spending = (Spendings) itemsArrayList.get(position);

            spendingHolder.title.setText(spending.getName());
            spendingHolder.amount.setText(spending.getAmount());
            spendingHolder.category.setText(spending.getCategory());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(spending.getDate());
            spendingHolder.purchaseDate.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(spending.getDate());
            // Si estás mostrando el mes y el año en otro lugar, usa el formato adecuado
        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
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
        return itemsArrayList.size();
    }

    public static class SpendingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnSpendingClickListener onSpendingClickListener;

        public SpendingViewHolder(@NonNull View itemView, OnSpendingClickListener onSpendingClickListener) {
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
    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView monthHeader;

        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            monthHeader = itemView.findViewById(R.id.textviewMonthHeader);
        }
    }

    public interface OnSpendingClickListener {
        void onSpendingClick(int position);
    }

    // Método para actualizar la lista de facturas
    public void updateSpendings(ArrayList<Spendings> newSpendings) {
        itemsArrayList = groupSpendingsByMonth(newSpendings);
        notifyDataSetChanged();
    }

    // Método para agrupar las facturas por mes
    private ArrayList<Object> groupSpendingsByMonth(ArrayList<Spendings> spendingsArrayList) {
        Map<String, List<Spendings>> groupedSpendings = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (Spendings spending : spendingsArrayList) {
            // Convertir Timestamp a Date y formatear
            String monthYear = sdf.format(spending.getDate().toDate()); // Asegúrate de que getDate() devuelva un Timestamp
            if (!groupedSpendings.containsKey(monthYear)) {
                groupedSpendings.put(monthYear, new ArrayList<>());
            }
            groupedSpendings.get(monthYear).add(spending);
        }

        ArrayList<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<Spendings>> entry : groupedSpendings.entrySet()) {
            items.add(entry.getKey()); // Añade el mes y año como encabezado
            items.addAll(entry.getValue()); // Añade las facturas del mes correspondiente
        }

        return items;
    }
}
