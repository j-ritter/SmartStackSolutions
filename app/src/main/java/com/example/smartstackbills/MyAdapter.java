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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private OnBillClickListener onBillClickListener;

    private static final int ITEM_BILL = 0;
    private static final int ITEM_MONTH_HEADER = 1;

    public MyAdapter(Context context, ArrayList<Bills> billsArrayList, OnBillClickListener onBillClickListener) {
        this.context = context;
        this.itemsArrayList = groupBillsByMonth(billsArrayList);
        this.onBillClickListener = onBillClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof String) {
            return ITEM_MONTH_HEADER;
        } else {
            return ITEM_BILL;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_BILL) {
            View v = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
            return new BillViewHolder(v, onBillClickListener);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
            return new MonthHeaderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_BILL) {
            BillViewHolder billHolder = (BillViewHolder) holder;
            Bills bill = (Bills) itemsArrayList.get(position);

            billHolder.title.setText(bill.getName());
            billHolder.amount.setText(bill.getAmount());
            billHolder.category.setText(bill.getCategory());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(bill.getDate());
            billHolder.purchaseDate.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(bill.getDate());
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

    public static class BillViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnBillClickListener onBillClickListener;

        public BillViewHolder(@NonNull View itemView, OnBillClickListener onBillClickListener) {
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

    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView monthHeader;

        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            monthHeader = itemView.findViewById(R.id.textviewMonthHeader);
        }
    }

    public interface OnBillClickListener {
        void onBillClick(int position);
    }

    // Método para actualizar la lista de facturas
    public void updateBills(ArrayList<Bills> newBills) {
        itemsArrayList = groupBillsByMonth(newBills);
        notifyDataSetChanged();
    }

    // Método para agrupar las facturas por mes
    private ArrayList<Object> groupBillsByMonth(ArrayList<Bills> billsArrayList) {
        Map<String, List<Bills>> groupedBills = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (Bills bill : billsArrayList) {
            // Convertir Timestamp a Date y formatear
            String monthYear = sdf.format(bill.getDate().toDate()); // Asegúrate de que getDate() devuelva un Timestamp
            if (!groupedBills.containsKey(monthYear)) {
                groupedBills.put(monthYear, new ArrayList<>());
            }
            groupedBills.get(monthYear).add(bill);
        }

        ArrayList<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<Bills>> entry : groupedBills.entrySet()) {
            items.add(entry.getKey()); // Añade el mes y año como encabezado
            items.addAll(entry.getValue()); // Añade las facturas del mes correspondiente
        }

        return items;
    }

}