package com.example.smartstackbills;

import android.content.Context;
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
            // Set the checkbox state
            billHolder.checkBoxPaid.setChecked(bill.isPaid());

            // Handle checkbox change
            billHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                bill.setPaid(isChecked);
                if (isChecked) {
                    // Save the bill to the Spendings collection in Firestore
                    saveBillToSpendings(bill);

                    // Remove the bill from the current list and notify the adapter
                    itemsArrayList.remove(position);
                    notifyItemRemoved(position);
                } else {
                    // If unchecked, do not move to Spendings, just update Firestore
                    saveBillToFirestore(bill);  // Save the change to Firestore
                }
            });

        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }
    private void saveBillToSpendings(Bills bill) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userUid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("spendings")
                    .document(bill.getBillId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Bill moved to Spendings successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error moving bill to Spendings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            // Remove the bill from the Bills collection
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("bills")
                    .document(bill.getBillId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Bill removed from Bills collection", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error removing bill from Bills collection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    public static class BillViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, category, amount, purchaseDate;
        OnBillClickListener onBillClickListener;
        CheckBox checkBoxPaid;

        public BillViewHolder(@NonNull View itemView, OnBillClickListener onBillClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textviewTitle);
            category = itemView.findViewById(R.id.textviewCategory);
            amount = itemView.findViewById(R.id.textviewAmount);
            purchaseDate = itemView.findViewById(R.id.textviewDate);
            checkBoxPaid = itemView.findViewById(R.id.imgCheckBoxItems);
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
    private void saveBillToFirestore(Bills bill) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userUid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userUid)
                    .collection("bills")
                    .document(bill.getBillId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Bill updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error updating bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }


}