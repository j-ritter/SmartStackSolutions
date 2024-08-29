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

public class MyAdapterIncome extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private OnIncomeClickListener onIncomeClickListener;

    private static final int ITEM_INCOME = 0;
    private static final int ITEM_MONTH_HEADER = 1;

    public MyAdapterIncome(Context context, ArrayList<Income> incomeArrayList, OnIncomeClickListener onIncomeClickListener) {
        this.context = context;
        this.itemsArrayList = groupIncomeByMonth(incomeArrayList);
        this.onIncomeClickListener = onIncomeClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof String) {
            return ITEM_MONTH_HEADER;
        } else {
            return ITEM_INCOME;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_INCOME) {
            View v = LayoutInflater.from(context).inflate(R.layout.items_income, parent, false);
            return new IncomeViewHolder(v, onIncomeClickListener);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
            return new MyAdapterIncome.MonthHeaderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_INCOME) {
            IncomeViewHolder incomeHolder = (IncomeViewHolder) holder;
            Income income = (Income) itemsArrayList.get(position);

            incomeHolder.title.setText(income.getName());
            incomeHolder.amount.setText(income.getAmount());
            incomeHolder.category.setText(income.getCategory());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(income.getDate());
            incomeHolder.dateOfIncome.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(income.getDate());
            // Si estás mostrando el mes y el año en otro lugar, usa el formato adecuado
        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }
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



public static class IncomeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    TextView title, category, amount, dateOfIncome;
    OnIncomeClickListener onIncomeClickListener;

    public IncomeViewHolder(@NonNull View itemView, OnIncomeClickListener onIncomeClickListener) {
        super(itemView);
        title = itemView.findViewById(R.id.textviewTitleItemsIncome);
        category = itemView.findViewById(R.id.textviewCategoryItemsIncome);
        amount = itemView.findViewById(R.id.textviewAmountItemsIncome);
        dateOfIncome = itemView.findViewById(R.id.textviewDateItemsIncome);
        this.onIncomeClickListener = onIncomeClickListener;
        itemView.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        onIncomeClickListener.onIncomeClick(getAdapterPosition());
    }
}

public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {

    TextView monthHeader;

    public MonthHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        monthHeader = itemView.findViewById(R.id.textviewMonthHeader);
    }
}

public interface OnIncomeClickListener {
    void onIncomeClick(int position);
}
// Method to update the list of income items
public void updateIncome(ArrayList<Income> newIncome) {
    itemsArrayList = groupIncomeByMonth(newIncome);

    notifyDataSetChanged();
}

// Método para agrupar las facturas por mes
private ArrayList<Object> groupIncomeByMonth(ArrayList<Income> incomeArrayList) {
    Map<String, List<Income>> groupedIncome = new HashMap<>();
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    for (Income income : incomeArrayList) {
        // Convertir Timestamp a Date y formatear
        String monthYear = sdf.format(income.getDate().toDate()); // Asegúrate de que getDate() devuelva un Timestamp
        if (!groupedIncome.containsKey(monthYear)) {
            groupedIncome.put(monthYear, new ArrayList<>());
        }
        groupedIncome.get(monthYear).add(income);
    }

    ArrayList<Object> items = new ArrayList<>();
    for (Map.Entry<String, List<Income>> entry : groupedIncome.entrySet()) {
        items.add(entry.getKey()); // Añade el mes y año como encabezado
        items.addAll(entry.getValue()); // Añade las facturas del mes correspondiente
    }

    return items;
}
// Método para formatear el Timestamp a String



}
