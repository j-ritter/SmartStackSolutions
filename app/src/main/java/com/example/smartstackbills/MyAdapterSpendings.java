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

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(spending.getDate());
            spendingHolder.purchaseDate.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(spending.getDate());
            // Si estás mostrando el mes y el año en otro lugar, usa el formato adecuado

            // Set the checkbox state
            spendingHolder.checkBoxPaid.setOnCheckedChangeListener(null); // Clear any previous listener
            spendingHolder.checkBoxPaid.setChecked(spending.isPaid());

            spendingHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                spending.setPaid(isChecked);
                if (!isChecked) {
                    moveSpendingToBills(spending, position);
                }
            });

        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }
    private void moveSpendingToBills(Spendings spending, int position) {
        itemsArrayList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, itemsArrayList.size());

        saveSpendingToBills(spending);
    }

    private void saveSpendingToBills(Spendings spending) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String spendingId = spending.getSpendingId();
        if (spendingId == null) {
            Log.e("SaveSpendingToBills", "Spending ID is null. Cannot save to bills.");
            return;  // Exit the method to avoid a crash
        }if (userUid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("bills")
                    .document(spending.getSpendingId())
                    .set(spending)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Spending moved back to Bills.", Toast.LENGTH_SHORT).show();
                        // After successfully adding back to Bills, remove it from the Spendings collection
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

    // Método para actualizar la lista de facturas
    public void updateSpendings(ArrayList<Spendings> newSpendings) {

        itemsArrayList.clear();

        // Group the new spendings by month and update the items list
        itemsArrayList.addAll(groupSpendingsByMonth(newSpendings));

        // Notify the adapter that the data set has changed
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
