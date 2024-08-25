package com.example.smartstackbills;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

            // Convert Timestamp to String
            String formattedDate = formatTimestamp(spending.getDate());
            spendingHolder.purchaseDate.setText(formattedDate);

            String monthYear = formatMonthYear(spending.getDate());

            spendingHolder.checkBoxPaid.setOnCheckedChangeListener(null); // Clear any previous listener
            spendingHolder.checkBoxPaid.setChecked(spending.isPaid());

            spendingHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                spending.setPaid(isChecked);
                if (!isChecked) {
                    saveSpendingToBills(spending);
                    itemsArrayList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, itemsArrayList.size());
                } else {
                    saveSpendingToFirestore(spending);
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

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(userUid)
                    .collection("bills")
                    .document(spending.getSpendingId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Spending moved back to Bills.", Toast.LENGTH_SHORT).show();

                        db.collection("users")
                                .document(userUid)
                                .collection("spendings")
                                .document(spendingId)
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

    // Format Timestamp to String
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

    public static class SpendingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnSpendingClickListener onSpendingClickListener;
        CheckBox checkBoxPaid;

        public SpendingViewHolder(@NonNull View itemView, OnSpendingClickListener onSpendingClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitleItemsSpendings);
            category = itemView.findViewById(R.id.textviewCategoryItemsSpendings);
            amount = itemView.findViewById(R.id.textviewAmountItemsSpendings);
            purchaseDate = itemView.findViewById(R.id.textviewDateItemsSpendings);
            checkBoxPaid = itemView.findViewById(R.id.imgCheckBoxItemsSpendings);
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
        itemsArrayList.clear();
        itemsArrayList.addAll(groupSpendingsByMonth(newSpendings));
        notifyDataSetChanged();
    }

    private ArrayList<Object> groupSpendingsByMonth(ArrayList<Spendings> spendingsArrayList) {
        Map<String, List<Spendings>> groupedSpendings = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (Spendings spending : spendingsArrayList) {
            String monthYear = sdf.format(spending.getDate().toDate());
            if (!groupedSpendings.containsKey(monthYear)) {
                groupedSpendings.put(monthYear, new ArrayList<>());
            }
            groupedSpendings.get(monthYear).add(spending);
        }

        ArrayList<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<Spendings>> entry : groupedSpendings.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }

        return items;
    }
    private void saveSpendingToFirestore(Spendings spending) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userUid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("spendings")
                    .document(spending.getSpendingId())
                    .set(spending)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Spending updated successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error updating spending: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
