package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
            incomeHolder.amount.setText(String.format(Locale.getDefault(), "%.2f", income.getAmount()));

            incomeHolder.category.setText(income.getCategory());

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(income.getDate());
            incomeHolder.dateOfIncome.setText(formattedDate);

            // Show or hide recurring icon based on the income's repeat field
            if (!"No".equals(income.getRepeat())) {
                incomeHolder.recurringIcon.setVisibility(View.VISIBLE);
            } else {
                incomeHolder.recurringIcon.setVisibility(View.GONE);
            }

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

    public Object getItemAtPosition(int position) {
        return itemsArrayList.get(position);
    }

public static class IncomeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    TextView title, category, amount, dateOfIncome;
    OnIncomeClickListener onIncomeClickListener;
    ImageView recurringIcon;

    public IncomeViewHolder(@NonNull View itemView, OnIncomeClickListener onIncomeClickListener) {
        super(itemView);
        title = itemView.findViewById(R.id.textviewTitleItemsIncome);
        category = itemView.findViewById(R.id.textviewCategoryItemsIncome);
        amount = itemView.findViewById(R.id.textviewAmountItemsIncome);
        dateOfIncome = itemView.findViewById(R.id.textviewDateItemsIncome);
        recurringIcon = itemView.findViewById(R.id.imgRecurringIconIncome);
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
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    Calendar currentDate = Calendar.getInstance();

    int currentMonth = currentDate.get(Calendar.MONTH);
    int currentYear = currentDate.get(Calendar.YEAR);

    // Separate income items into past, current, and future groups
    Map<Integer, Map<Integer, List<Income>>> currentMonthIncome = new TreeMap<>();
    Map<Integer, Map<Integer, List<Income>>> futureMonthIncome = new TreeMap<>();
    Map<Integer, Map<Integer, List<Income>>> pastMonthIncome = new TreeMap<>(Comparator.reverseOrder());

    for (Income income : incomeArrayList) {
        Date incomeDate = income.getDate().toDate();
        Calendar incomeCalendar = Calendar.getInstance();
        incomeCalendar.setTime(incomeDate);

        int incomeMonth = incomeCalendar.get(Calendar.MONTH);
        int incomeYear = incomeCalendar.get(Calendar.YEAR);

        // Categorize income items based on their relation to the current date
        if (incomeYear == currentYear && incomeMonth == currentMonth) {
            currentMonthIncome.computeIfAbsent(incomeYear, k -> new TreeMap<>())
                    .computeIfAbsent(incomeMonth, k -> new ArrayList<>())
                    .add(income);
        } else if (incomeYear > currentYear || (incomeYear == currentYear && incomeMonth > currentMonth)) {
            futureMonthIncome.computeIfAbsent(incomeYear, k -> new TreeMap<>())
                    .computeIfAbsent(incomeMonth, k -> new ArrayList<>())
                    .add(income);
        } else {
            pastMonthIncome.computeIfAbsent(incomeYear, k -> new TreeMap<>(Comparator.reverseOrder()))
                    .computeIfAbsent(incomeMonth, k -> new ArrayList<>())
                    .add(income);
        }
    }

    // Sort income within each month group by date in ascending order
    Comparator<Income> dateComparator = Comparator.comparing(i -> i.getDate().toDate());
    currentMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator)));
    futureMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator)));
    pastMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator.reversed())));

    ArrayList<Object> items = new ArrayList<>();

    // Add current month income (if any)
    if (!currentMonthIncome.isEmpty()) {
        for (Map.Entry<Integer, Map<Integer, List<Income>>> entry : currentMonthIncome.entrySet()) {
            for (Map.Entry<Integer, List<Income>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }
    }

    // Add future months in ascending order by year and month
    for (Map.Entry<Integer, Map<Integer, List<Income>>> entry : futureMonthIncome.entrySet()) {
        for (Map.Entry<Integer, List<Income>> monthEntry : entry.getValue().entrySet()) {
            String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
            items.add(monthYear);
            items.addAll(monthEntry.getValue());
        }
    }

    // Add past months in descending order by year and month
    for (Map.Entry<Integer, Map<Integer, List<Income>>> entry : pastMonthIncome.entrySet()) {
        for (Map.Entry<Integer, List<Income>> monthEntry : entry.getValue().entrySet()) {
            String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
            items.add(monthYear);
            items.addAll(monthEntry.getValue());
        }
    }

    return items;
}
}
