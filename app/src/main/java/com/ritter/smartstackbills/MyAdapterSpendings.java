package com.ritter.smartstackbills;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

public class MyAdapterSpendings extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private ArrayList<Spendings> sourceSpendingsArrayList;
    private final Set<Integer> collapsedSections = new HashSet<>();
    private OnSpendingClickListener onSpendingClickListener;

    private static final int ITEM_SPENDING = 0;
    private static final int ITEM_MONTH_HEADER = 1;
    private static final int ITEM_SECTION_HEADER = 2;

    public MyAdapterSpendings(Context context, ArrayList<Spendings> spendingsArrayList, OnSpendingClickListener onSpendingClickListener) {
        this.context = context;
        this.sourceSpendingsArrayList = new ArrayList<>(spendingsArrayList);
        this.itemsArrayList = groupSpendingsByMonth(spendingsArrayList);
        this.onSpendingClickListener = onSpendingClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof Spendings) {
            return ITEM_SPENDING;
        } else if (itemsArrayList.get(position) instanceof SectionHeader) {
            return ITEM_SECTION_HEADER;
        }
        return ITEM_MONTH_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SPENDING) {
            View v = LayoutInflater.from(context).inflate(R.layout.items_spendings, parent, false);
            return new SpendingViewHolder(v, onSpendingClickListener);
        } else if (viewType == ITEM_SECTION_HEADER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_section_header, parent, false);
            return new SectionHeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
            return new MonthHeaderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_SPENDING) {
            SpendingViewHolder spendingHolder = (SpendingViewHolder) holder;
            Spendings spending = (Spendings) itemsArrayList.get(position);

            spendingHolder.title.setText(spending.getName());
            spendingHolder.amount.setText(CurrencyPreferences.format(context, spending.getAmount(), spending.getCurrency()));

            spendingHolder.category.setText(
                    FinancialEntryOptions.displayCategory(context, spending.getCategory())
            );

            // Convert Timestamp to String
            String formattedDate = formatTimestamp(spending.getDate());
            spendingHolder.purchaseDate.setText(formattedDate);

            String monthYear = formatMonthYear(spending.getDate());

            // Set the checkbox state
            spendingHolder.checkBoxPaid.setOnCheckedChangeListener(null); // Clear any previous listener
            spendingHolder.checkBoxPaid.setChecked(spending.isPaid());

            // Show or hide recurring icon based on the spending's recurring status
            if (spending.isRecurring()) {  // Show the recurring icon if the spending was recurring
                spendingHolder.recurringIcon.setVisibility(View.VISIBLE);
            } else {
                spendingHolder.recurringIcon.setVisibility(View.GONE);
            }

            spendingHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {  // Se ejecutará solo cuando se quite el check
                    spendingHolder.checkBoxPaid.setEnabled(false);
                    saveSpendingToBills(spending, position, spendingHolder);
                }
            });

        } else if (holder.getItemViewType() == ITEM_MONTH_HEADER) {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            styleMonthHeader(headerHolder);
            headerHolder.monthHeader.setText(monthHeader);
        } else {
            SectionHeaderViewHolder headerHolder = (SectionHeaderViewHolder) holder;
            SectionHeader sectionHeader = (SectionHeader) itemsArrayList.get(position);
            styleSectionHeader(headerHolder, sectionHeader, R.color.spending_color, R.color.filter_closed_active);
            headerHolder.monthHeader.setText(context.getString(sectionHeader.titleRes));
        }
    }
    private void saveSpendingToBills(Spendings spending, int adapterPosition, SpendingViewHolder holder) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            holder.checkBoxPaid.setEnabled(true);
            notifyDataSetChanged();
            return;
        }
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String spendingId = spending.getSpendingId();

        if (spendingId == null) {
            Log.e("SaveSpendingToBills", "Spending ID is null. Cannot save to bills.");
            holder.checkBoxPaid.setEnabled(true);
            notifyDataSetChanged();
            return;  // Exit the method to avoid a crash
        }

        if (userUid != null) {

            // Convert the spending back to a bill
            Bills bill = new Bills();
            bill.setBillId(spendingId);
            bill.setName(spending.getName());
            bill.setAmount(spending.getAmount());
            bill.setCurrency(spending.getCurrency());
            bill.setCategory(spending.getCategory());
            bill.setSubcategory(spending.getSubcategory());
            bill.setVendor(spending.getVendor());
            bill.setDate(spending.getDate());
            bill.setComment(spending.getComment());
            bill.setAttachment(spending.getAttachment());
            bill.setPaid(false);
            bill.setRepeat(spending.getRepeat() == null ? "No" : spending.getRepeat());
            bill.setParentBillId(
                    spending.getParentBillId() == null
                            ? spending.getSpendingId()
                            : spending.getParentBillId()
            );

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference billRef = db.collection("users")
                    .document(userUid).collection("bills").document(spendingId);
            com.google.firebase.firestore.DocumentReference spendingRef = db.collection("users")
                    .document(userUid).collection("spendings").document(spendingId);

            db.runBatch(batch -> {
                        batch.set(billRef, bill);
                        batch.delete(spendingRef);
                    })
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, R.string.payment_moved_to_open, Toast.LENGTH_SHORT).show();
                        PaymentNotificationScheduler.INSTANCE.scheduleBill(context, userUid, bill, false);
                        // Let the Firestore listener rebuild the grouped, filtered list.
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        holder.checkBoxPaid.setEnabled(true);
                        Toast.makeText(context, context.getString(R.string.payment_move_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    });
        }
    }

    @Override
    public int getItemCount() {
        return itemsArrayList.size();
    }

    public Object getItemAtPosition(int position) {
        return itemsArrayList.get(position);
    }

    public static class SpendingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        CheckBox checkBoxPaid;
        ImageView recurringIcon;
        OnSpendingClickListener onSpendingClickListener;

        public SpendingViewHolder(@NonNull View itemView, OnSpendingClickListener onSpendingClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleItemsSpendings);
            category = itemView.findViewById(R.id.textviewCategoryItemsSpendings);
            amount = itemView.findViewById(R.id.textviewAmountItemsSpendings);
            purchaseDate = itemView.findViewById(R.id.textviewDateItemsSpendings);
            checkBoxPaid = itemView.findViewById(R.id.imgCheckBoxItemsSpendings);
            recurringIcon = itemView.findViewById(R.id.imgRecurringIconSpendings);
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

    public void updateSpendings(ArrayList<Spendings> newSpendings) {
        sourceSpendingsArrayList = new ArrayList<>(newSpendings);
        itemsArrayList = groupSpendingsByMonth(newSpendings);
        notifyDataSetChanged();
    }

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
            itemsArrayList = groupSpendingsByMonth(sourceSpendingsArrayList);
            notifyDataSetChanged();
        });
        holder.monthHeader.setTextSize(16);
        holder.monthHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        holder.monthHeader.setAllCaps(true);
    }

    private ArrayList<Object> groupSpendingsByMonth(ArrayList<Spendings> spendingsArrayList) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Calendar currentDate = Calendar.getInstance();

        int currentMonth = currentDate.get(Calendar.MONTH);
        int currentYear = currentDate.get(Calendar.YEAR);

        Map<Integer, Map<Integer, List<Spendings>>> currentMonthSpendings = new TreeMap<>();
        Map<Integer, Map<Integer, List<Spendings>>> pastMonthSpendings = new TreeMap<>(Comparator.reverseOrder());
        Map<Integer, Map<Integer, List<Spendings>>> futureMonthSpendings = new TreeMap<>();
        List<Spendings> noDateSpendings = new ArrayList<>();

        for (Spendings spending : spendingsArrayList) {
            if (spending.getDate() == null) {
                noDateSpendings.add(spending);
                continue;
            }
            Date spendingDate = spending.getDate().toDate();
            Calendar spendingCalendar = Calendar.getInstance();
            spendingCalendar.setTime(spendingDate);

            int spendingMonth = spendingCalendar.get(Calendar.MONTH);
            int spendingYear = spendingCalendar.get(Calendar.YEAR);

            // Categorize spendings based on their relation to the current date
            if (spendingYear == currentYear && spendingMonth == currentMonth) {
                currentMonthSpendings.computeIfAbsent(spendingYear, k -> new TreeMap<>())
                        .computeIfAbsent(spendingMonth, k -> new ArrayList<>())
                        .add(spending);
            } else if (spendingYear > currentYear || (spendingYear == currentYear && spendingMonth > currentMonth)) {
                futureMonthSpendings.computeIfAbsent(spendingYear, k -> new TreeMap<>())
                        .computeIfAbsent(spendingMonth, k -> new ArrayList<>())
                        .add(spending);
            } else {
                pastMonthSpendings.computeIfAbsent(spendingYear, k -> new TreeMap<>(Comparator.reverseOrder()))
                        .computeIfAbsent(spendingMonth, k -> new ArrayList<>())
                        .add(spending);
            }
        }

        Comparator<Spendings> dateComparator = Comparator.comparing(s -> s.getDate().toDate());
        currentMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator.reversed())));
        futureMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator)));
        pastMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator.reversed())));
        noDateSpendings.sort(Comparator.comparing(s -> s.getName() == null ? "" : s.getName().toLowerCase(Locale.getDefault())));

        ArrayList<Object> items = new ArrayList<>();

        addGroupedSection(items, currentMonthSpendings, sdf, R.string.section_this_month_short);
        addGroupedSection(items, pastMonthSpendings, sdf, R.string.section_past_closed_payments);
        addGroupedSection(items, futureMonthSpendings, sdf, R.string.section_future_closed_payments);
        if (!noDateSpendings.isEmpty()) {
            items.add(new SectionHeader(R.string.section_no_date));
            if (!collapsedSections.contains(R.string.section_no_date)) {
                items.addAll(noDateSpendings);
            }
        }

        return items;
    }

    private void addGroupedSection(
            ArrayList<Object> items,
            Map<Integer, Map<Integer, List<Spendings>>> groupedSpendings,
            SimpleDateFormat sdf,
            int sectionTitleRes
    ) {
        if (groupedSpendings.isEmpty()) {
            return;
        }
        items.add(new SectionHeader(sectionTitleRes));
        if (collapsedSections.contains(sectionTitleRes)) {
            return;
        }
        for (Map.Entry<Integer, Map<Integer, List<Spendings>>> entry : groupedSpendings.entrySet()) {
            for (Map.Entry<Integer, List<Spendings>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        return "-"; // Default value if timestamp is null
    }
    // Format Timestamp to month and year
    private String formatMonthYear(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

}
