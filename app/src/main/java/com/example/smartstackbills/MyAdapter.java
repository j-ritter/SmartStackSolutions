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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

            // Set the checkbox state
            billHolder.checkBoxPaid.setOnCheckedChangeListener(null);
            billHolder.checkBoxPaid.setChecked(bill.isPaid());

            // Handle checkbox change
            billHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                bill.setPaid(isChecked);

                // Save the bill to the Spendings collection in Firestore
                saveBillToSpendings(bill);

                // Remove the bill from the current list and notify the adapter
                itemsArrayList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, itemsArrayList.size());
            });

            // Show or hide recurring icon based on the bill's repeat status
            if (!"No".equals(bill.getRepeat())) { // Highlighted change
                billHolder.recurringIcon.setVisibility(View.VISIBLE);  // Show icon for recurring bills
            } else {
                billHolder.recurringIcon.setVisibility(View.GONE);  // Hide icon for non-recurring bills
            }

        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }
    private void saveBillToSpendings(Bills bill) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String billId = bill.getBillId();
        if (billId == null) {
            Log.e("SaveBillToSpendings", "Bill ID is null. Cannot save to spendings.");
            return;  // Exit the method to avoid a crash
        }
        if (userUid != null) {
            // Convert the Bills object to a Spendings object
            Spendings spending = new Spendings();
            spending.setSpendingId(billId);  // Using the bill ID as the spending ID
            spending.setName(bill.getName());
            spending.setAmount(bill.getAmount());
            spending.setCategory(bill.getCategory());
            spending.setSubcategory(bill.getSubcategory());
            spending.setVendor(bill.getVendor());
            spending.setDate(bill.getDate());
            spending.setComment(bill.getComment());
            spending.setAttachment(bill.getAttachment());
            spending.setPaid(true);  // Set it as paid since it's moving to spendings

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("spendings")
                    .document(bill.getBillId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Bill moved to Spendings.", Toast.LENGTH_SHORT).show();
                        // After successfully adding to Spendings, remove it from the Bills collection
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userUid)
                                .collection("bills")
                                .document(bill.getBillId())
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(context, "Bill removed from Bills collection.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Error removing bill from Bills: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error moving bill to Spendings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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

    public Object getItemAtPosition(int position) {
        return itemsArrayList.get(position);
    }

    public static class BillViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnBillClickListener onBillClickListener;
        CheckBox checkBoxPaid;
        ImageView recurringIcon;


        public BillViewHolder(@NonNull View itemView, OnBillClickListener onBillClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleItemsBills);
            category = itemView.findViewById(R.id.textviewCategoryItemsBills);
            amount = itemView.findViewById(R.id.textviewAmountItemsBills);
            checkBoxPaid = itemView.findViewById(R.id.imgCheckBoxItemsBills);
            purchaseDate = itemView.findViewById(R.id.textviewDateItemsBills);
            recurringIcon = itemView.findViewById(R.id.imgRecurringIcon);
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

    // Method to group bills by month with correct order (current -> past -> future)
    private ArrayList<Object> groupBillsByMonth(ArrayList<Bills> billsArrayList) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Calendar currentDate = Calendar.getInstance();

        int currentMonth = currentDate.get(Calendar.MONTH);
        int currentYear = currentDate.get(Calendar.YEAR);

        // Separate bills into past, current, and future groups
        Map<Integer, Map<Integer, List<Bills>>> currentMonthBills = new TreeMap<>();
        Map<Integer, Map<Integer, List<Bills>>> futureMonthBills = new TreeMap<>();
        Map<Integer, Map<Integer, List<Bills>>> pastMonthBills = new TreeMap<>(Comparator.reverseOrder());

        for (Bills bill : billsArrayList) {
            Date billDate = bill.getDate().toDate();
            Calendar billCalendar = Calendar.getInstance();
            billCalendar.setTime(billDate);

            int billMonth = billCalendar.get(Calendar.MONTH);
            int billYear = billCalendar.get(Calendar.YEAR);

            // Categorize bills based on their relation to the current date
            if (billYear == currentYear && billMonth == currentMonth) {
                currentMonthBills.computeIfAbsent(billYear, k -> new TreeMap<>())
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            } else if (billYear > currentYear || (billYear == currentYear && billMonth > currentMonth)) {
                futureMonthBills.computeIfAbsent(billYear, k -> new TreeMap<>())
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            } else {
                pastMonthBills.computeIfAbsent(billYear, k -> new TreeMap<>(Comparator.reverseOrder()))
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            }
        }

        // Sort bills within each month group by date in ascending order
        Comparator<Bills> dateComparator = Comparator.comparing(b -> b.getDate().toDate());
        currentMonthBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator)));
        futureMonthBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator)));
        pastMonthBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator.reversed())));

        ArrayList<Object> items = new ArrayList<>();

        // Add current month bills (if any)
        if (!currentMonthBills.isEmpty()) {
            for (Map.Entry<Integer, Map<Integer, List<Bills>>> entry : currentMonthBills.entrySet()) {
                for (Map.Entry<Integer, List<Bills>> monthEntry : entry.getValue().entrySet()) {
                    String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                    items.add(monthYear);
                    items.addAll(monthEntry.getValue());
                }
            }
        }

        // Add future months in ascending order by year and month
        for (Map.Entry<Integer, Map<Integer, List<Bills>>> entry : futureMonthBills.entrySet()) {
            for (Map.Entry<Integer, List<Bills>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }

        // Add past months in descending order by year and month
        for (Map.Entry<Integer, Map<Integer, List<Bills>>> entry : pastMonthBills.entrySet()) {
            for (Map.Entry<Integer, List<Bills>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }

        return items;
    }

}