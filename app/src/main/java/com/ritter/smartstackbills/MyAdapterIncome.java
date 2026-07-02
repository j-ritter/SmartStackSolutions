package com.ritter.smartstackbills;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MyAdapterIncome extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private ArrayList<Income> sourceIncomeArrayList;
    private final Set<Integer> collapsedSections = new HashSet<>();
    private OnIncomeClickListener onIncomeClickListener;

    private static final int ITEM_INCOME = 0;
    private static final int ITEM_MONTH_HEADER = 1;
    private static final int ITEM_SECTION_HEADER = 2;

    public MyAdapterIncome(Context context, ArrayList<Income> incomeArrayList, OnIncomeClickListener onIncomeClickListener) {
        this.context = context;
        this.sourceIncomeArrayList = new ArrayList<>(incomeArrayList);
        this.itemsArrayList = groupIncomeByMonth(incomeArrayList);
        this.onIncomeClickListener = onIncomeClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof String) {
            return ITEM_MONTH_HEADER;
        } else if (itemsArrayList.get(position) instanceof SectionHeader) {
            return ITEM_SECTION_HEADER;
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
        } else if (viewType == ITEM_SECTION_HEADER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_section_header, parent, false);
            return new SectionHeaderViewHolder(v);
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
            incomeHolder.amount.setText(CurrencyPreferences.format(context, income.getAmount(), income.getCurrency()));

            incomeHolder.category.setText(
                    FinancialEntryOptions.displayCategory(context, income.getCategory())
            );

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
        } else if (holder.getItemViewType() == ITEM_MONTH_HEADER) {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            styleMonthHeader(headerHolder);
            headerHolder.monthHeader.setText(monthHeader);
        } else {
            SectionHeaderViewHolder headerHolder = (SectionHeaderViewHolder) holder;
            SectionHeader sectionHeader = (SectionHeader) itemsArrayList.get(position);
            styleSectionHeader(headerHolder, sectionHeader, R.color.income_color, R.color.filter_income_active);
            headerHolder.monthHeader.setText(context.getString(sectionHeader.titleRes));
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
    sourceIncomeArrayList = new ArrayList<>(newIncome);
    itemsArrayList = groupIncomeByMonth(newIncome);

    notifyDataSetChanged();
}

// Método para agrupar las facturas por mes
private static class SectionHeader {
    final int titleRes;

    SectionHeader(int titleRes) {
        this.titleRes = titleRes;
    }
}

public static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {

    TextView monthHeader;
    View chip;
    ImageView arrow;

    public SectionHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        monthHeader = itemView.findViewById(R.id.textviewSectionHeader);
        chip = itemView.findViewById(R.id.sectionHeaderChip);
        arrow = itemView.findViewById(R.id.imageSectionArrow);
    }
}

private void styleMonthHeader(MonthHeaderViewHolder holder) {
    holder.monthHeader.setTextColor(ContextCompat.getColor(context, android.R.color.black));
    holder.monthHeader.setTextSize(16);
    holder.monthHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    holder.monthHeader.setAllCaps(false);
}

private void styleSectionHeader(SectionHeaderViewHolder holder, SectionHeader sectionHeader, int colorRes, int backgroundColorRes) {
    int color = ContextCompat.getColor(context, colorRes);
    int backgroundColor = ContextCompat.getColor(context, backgroundColorRes);
    GradientDrawable chipBackground = new GradientDrawable();
    chipBackground.setColor(backgroundColor);
    chipBackground.setCornerRadius(4f);
    holder.chip.setBackground(chipBackground);
    holder.monthHeader.setTextColor(color);
    holder.arrow.setColorFilter(color);
    holder.arrow.setImageResource(
            collapsedSections.contains(sectionHeader.titleRes)
                    ? R.drawable.ic_arrow_right
                    : R.drawable.ic_arrow_down
    );
    holder.chip.setOnClickListener(v -> {
        if (collapsedSections.contains(sectionHeader.titleRes)) {
            collapsedSections.remove(sectionHeader.titleRes);
        } else {
            collapsedSections.add(sectionHeader.titleRes);
        }
        itemsArrayList = groupIncomeByMonth(sourceIncomeArrayList);
        notifyDataSetChanged();
    });
    holder.monthHeader.setTextSize(16);
    holder.monthHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    holder.monthHeader.setAllCaps(true);
}

private ArrayList<Object> groupIncomeByMonth(ArrayList<Income> incomeArrayList) {
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    Calendar currentDate = Calendar.getInstance();

    int currentMonth = currentDate.get(Calendar.MONTH);
    int currentYear = currentDate.get(Calendar.YEAR);

    Map<Integer, Map<Integer, List<Income>>> currentMonthIncome = new TreeMap<>();
    Map<Integer, Map<Integer, List<Income>>> futureMonthIncome = new TreeMap<>();
    Map<Integer, Map<Integer, List<Income>>> pastMonthIncome = new TreeMap<>(Comparator.reverseOrder());
    List<Income> noDateIncome = new ArrayList<>();

    for (Income income : incomeArrayList) {
        if (income.getDate() == null) {
            noDateIncome.add(income);
            continue;
        }
        Date incomeDate = income.getDate().toDate();
        Calendar incomeCalendar = Calendar.getInstance();
        incomeCalendar.setTime(incomeDate);

        int incomeMonth = incomeCalendar.get(Calendar.MONTH);
        int incomeYear = incomeCalendar.get(Calendar.YEAR);

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

    Comparator<Income> dateComparator = Comparator.comparing(i -> i.getDate().toDate());
    currentMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator.reversed())));
    futureMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator)));
    pastMonthIncome.values().forEach(monthMap -> monthMap.values().forEach(incomes -> incomes.sort(dateComparator.reversed())));
    noDateIncome.sort(Comparator.comparing(i -> i.getName() == null ? "" : i.getName().toLowerCase(Locale.getDefault())));

    ArrayList<Object> items = new ArrayList<>();

    addGroupedSection(items, currentMonthIncome, sdf, R.string.section_this_month_short);
    addGroupedSection(items, futureMonthIncome, sdf, R.string.section_upcoming_income);
    addGroupedSection(items, pastMonthIncome, sdf, R.string.section_past_income);
    if (!noDateIncome.isEmpty()) {
        items.add(new SectionHeader(R.string.section_no_date));
        if (!collapsedSections.contains(R.string.section_no_date)) {
            items.addAll(noDateIncome);
        }
    }

    return items;
}

private void addGroupedSection(
        ArrayList<Object> items,
        Map<Integer, Map<Integer, List<Income>>> groupedIncome,
        SimpleDateFormat sdf,
        int sectionTitleRes
) {
    if (groupedIncome.isEmpty()) {
        return;
    }
    items.add(new SectionHeader(sectionTitleRes));
    if (collapsedSections.contains(sectionTitleRes)) {
        return;
    }
    for (Map.Entry<Integer, Map<Integer, List<Income>>> entry : groupedIncome.entrySet()) {
        for (Map.Entry<Integer, List<Income>> monthEntry : entry.getValue().entrySet()) {
            String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
            items.add(monthYear);
            items.addAll(monthEntry.getValue());
        }
    }
}
}
