package com.example.smartstackbills;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

            // Convert Timestamp to String
            String formattedDate = formatTimestamp(spending.getDate());
            spendingHolder.purchaseDate.setText(formattedDate);

            String monthYear = formatMonthYear(spending.getDate());

            // Set the checkbox state
            spendingHolder.checkBoxPaid.setOnCheckedChangeListener(null); // Clear any previous listener
            spendingHolder.checkBoxPaid.setChecked(spending.isPaid());

            // Show or hide recurring icon based on the spending's recurring status
            if (spending.isRecurring()) {  // NEW: Show the recurring icon if the spending was recurring
                spendingHolder.recurringIcon.setVisibility(View.VISIBLE);
            } else {
                spendingHolder.recurringIcon.setVisibility(View.GONE);
            }

            spendingHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {  // Se ejecutará solo cuando se quite el check
                    spending.setPaid(false);

                    saveSpendingToBills(spending);
                    itemsArrayList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, itemsArrayList.size());
                }
            });

        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }
    private void saveSpendingToBills(Spendings spending) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String spendingId = spending.getSpendingId();

        if (spendingId == null) {
            Log.e("SaveSpendingToBills", "Spending ID is null. Cannot save to bills.");
            return;  // Exit the method to avoid a crash
        }

        if (userUid != null) {

            // Convert the spending back to a bill
            Bills bill = new Bills();
            bill.setBillId(spendingId);
            bill.setName(spending.getName());
            bill.setAmount(spending.getAmount());
            bill.setCategory(spending.getCategory());
            bill.setSubcategory(spending.getSubcategory());
            bill.setVendor(spending.getVendor());
            bill.setDate(spending.getDate());
            bill.setComment(spending.getComment());
            bill.setAttachment(spending.getAttachment());
            bill.setPaid(false);

            // Check if repeat info is available and set it, defaulting to "No" if not available
            String repeatValue = spending.isRecurring() ? "Yes" : "No";
            bill.setRepeat(repeatValue);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("bills")
                    .document(spending.getSpendingId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Spending moved back to Bills.", Toast.LENGTH_SHORT).show();
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userUid)
                                .collection("spendings")
                                .document(spending.getSpendingId())
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(context, "Spending removed from Spendings collection.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Error removing spending from Spendings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error moving spending back to Bills: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    // Format Timestamp to month and year
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

    public static class SpendingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnSpendingClickListener onSpendingClickListener;
        CheckBox checkBoxPaid;
        ImageView recurringIcon;

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

        itemsArrayList = groupSpendingsByMonth(newSpendings);
        notifyDataSetChanged();
    }

    private ArrayList<Object> groupSpendingsByMonth(ArrayList<Spendings> spendingsArrayList) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Calendar currentDate = Calendar.getInstance();

        int currentMonth = currentDate.get(Calendar.MONTH);
        int currentYear = currentDate.get(Calendar.YEAR);

        // Separate spendings into past, current, and future groups
        Map<Integer, Map<Integer, List<Spendings>>> currentMonthSpendings = new TreeMap<>();
        Map<Integer, Map<Integer, List<Spendings>>> futureMonthSpendings = new TreeMap<>();
        Map<Integer, Map<Integer, List<Spendings>>> pastMonthSpendings = new TreeMap<>(Comparator.reverseOrder());

        for (Spendings spending : spendingsArrayList) {
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

        // Sort spendings within each month group by date in ascending order
        Comparator<Spendings> dateComparator = Comparator.comparing(s -> s.getDate().toDate());
        currentMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator)));
        futureMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator)));
        pastMonthSpendings.values().forEach(monthMap -> monthMap.values().forEach(spendings -> spendings.sort(dateComparator.reversed())));

        ArrayList<Object> items = new ArrayList<>();

        // Add current month spendings (if any)
        if (!currentMonthSpendings.isEmpty()) {
            for (Map.Entry<Integer, Map<Integer, List<Spendings>>> entry : currentMonthSpendings.entrySet()) {
                for (Map.Entry<Integer, List<Spendings>> monthEntry : entry.getValue().entrySet()) {
                    String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                    items.add(monthYear);
                    items.addAll(monthEntry.getValue());
                }
            }
        }

        // Add future months in ascending order by year and month
        for (Map.Entry<Integer, Map<Integer, List<Spendings>>> entry : futureMonthSpendings.entrySet()) {
            for (Map.Entry<Integer, List<Spendings>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }

        // Add past months in descending order by year and month
        for (Map.Entry<Integer, Map<Integer, List<Spendings>>> entry : pastMonthSpendings.entrySet()) {
            for (Map.Entry<Integer, List<Spendings>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }

        return items;
    }

    private void saveSpendingToFirestore(Spendings spending) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userUid != null) {
            Map<String, Object> spendingData = new HashMap<>();
            spendingData.put("name", spending.getName());
            spendingData.put("amount", spending.getAmount());
            spendingData.put("date", spending.getDate());
            spendingData.put("category", spending.getCategory());
            spendingData.put("subcategory", spending.getSubcategory());
            spendingData.put("vendor", spending.getVendor());
            spendingData.put("comment", spending.getComment());
            spendingData.put("attachment", spending.getAttachment());
            spendingData.put("paid", spending.isPaid());

            // Save spending without extra fields
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("spendings")
                    .document(spending.getSpendingId())
                    .set(spendingData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Spending updated successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error updating spending: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

}
