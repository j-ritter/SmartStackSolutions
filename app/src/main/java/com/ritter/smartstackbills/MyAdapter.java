package com.ritter.smartstackbills;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import kotlin.Unit;

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Object> itemsArrayList;
    private ArrayList<Bills> sourceBillsArrayList;
    private final Set<Integer> collapsedSections = new HashSet<>();
    private OnBillClickListener onBillClickListener;

    private static final int ITEM_BILL = 0;
    private static final int ITEM_MONTH_HEADER = 1;
    private static final int ITEM_SECTION_HEADER = 2;

    public MyAdapter(Context context, ArrayList<Bills> billsArrayList, OnBillClickListener onBillClickListener) {
        this.context = context;
        this.sourceBillsArrayList = new ArrayList<>(billsArrayList);
        this.itemsArrayList = groupBillsByMonth(billsArrayList);
        this.onBillClickListener = onBillClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsArrayList.get(position) instanceof String) {
            return ITEM_MONTH_HEADER;
        } else if (itemsArrayList.get(position) instanceof SectionHeader) {
            return ITEM_SECTION_HEADER;
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
        if (holder.getItemViewType() == ITEM_BILL) {
            BillViewHolder billHolder = (BillViewHolder) holder;
            Bills bill = (Bills) itemsArrayList.get(position);

            billHolder.title.setText(isBlank(bill.getName()) ? context.getString(R.string.untitled_payment) : bill.getName());
            billHolder.amount.setText(CurrencyPreferences.format(context, bill.getAmount(), bill.getCurrency()));
            boolean overdue = bill.getDate() != null
                    && AppDateUtils.INSTANCE.isBeforeToday(bill.getDate().toDate())
                    && !bill.isPaid();
            billHolder.amount.setTextColor(ContextCompat.getColor(
                    context,
                    overdue ? R.color.red : R.color.bill_color
            ));
            billHolder.category.setText(FinancialEntryOptions.displayCategory(context, bill.getCategory()));

            // Convierte Timestamp a String
            String formattedDate = formatTimestamp(bill.getDate());
            billHolder.purchaseDate.setText(formattedDate);

            // Si necesitas mostrar mes y año, usa:
            String monthYear = formatMonthYear(bill.getDate());

            // Set the checkbox state
            billHolder.checkBoxPaid.setOnCheckedChangeListener(null);
            billHolder.checkBoxPaid.setEnabled(true);
            billHolder.checkBoxPaid.setChecked(bill.isPaid());

            // Handle checkbox change
            billHolder.checkBoxPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    return;
                }

                billHolder.checkBoxPaid.setEnabled(false);
                saveBillToSpendings(bill, getCurrentBillPosition(bill));
            });

            // Show or hide recurring icon based on the bill's repeat status
            if (!"No".equals(bill.getRepeat())) { // Highlighted change
                billHolder.recurringIcon.setVisibility(View.VISIBLE);  // Show icon for recurring bills
            } else {
                billHolder.recurringIcon.setVisibility(View.GONE);  // Hide icon for non-recurring bills
            }

        } else if (holder.getItemViewType() == ITEM_MONTH_HEADER) {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) itemsArrayList.get(position);
            styleMonthHeader(headerHolder);
            headerHolder.monthHeader.setText(monthHeader);
        } else {
            SectionHeaderViewHolder headerHolder = (SectionHeaderViewHolder) holder;
            SectionHeader sectionHeader = (SectionHeader) itemsArrayList.get(position);
            styleSectionHeader(headerHolder, sectionHeader, R.color.bill_color, R.color.filter_open_active);
            headerHolder.monthHeader.setText(context.getString(sectionHeader.titleRes));
        }
    }
    private int getCurrentBillPosition(Bills bill) {
        return itemsArrayList.indexOf(bill);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void saveBillToSpendings(Bills bill, int adapterPosition) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            bill.setPaid(false);
            Toast.makeText(context, R.string.payment_login_required, Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            return;
        }

        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String billId = bill.getBillId();
        if (billId == null) {
            bill.setPaid(false);
            Toast.makeText(context, R.string.payment_id_missing, Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            return;  // Exit the method to avoid a crash
        }
        if (userUid != null) {
            UsageLimits.checkClosedPaymentCreation(context, userUid, 1L, (allowed, messageRes) -> {
                if (allowed) {
                    moveBillToSpendings(bill, adapterPosition, userUid);
                } else {
                    billHolderRollback(adapterPosition);
                    Toast.makeText(
                            context,
                            messageRes != null ? context.getString(messageRes) : context.getString(R.string.usage_limit_check_failed),
                            Toast.LENGTH_LONG
                    ).show();
                }
                return Unit.INSTANCE;
            });
        }
    }

    private void moveBillToSpendings(Bills bill, int adapterPosition, String userUid) {
        // Convert the Bills object to a Spendings object
        Spendings spending = new Spendings();
        spending.setSpendingId(bill.getBillId());  // Using the bill ID as the spending ID
        spending.setName(bill.getName());
        spending.setAmount(bill.getAmount());
        spending.setCurrency(bill.getCurrency());
        spending.setCategory(bill.getCategory());
        spending.setSubcategory(bill.getSubcategory());
        spending.setVendor(bill.getVendor());
        spending.setDate(bill.getDate());
        spending.setComment(bill.getComment());
        spending.setAttachment(bill.getAttachment());
        spending.setPaid(true);  // Set it as paid since it's moving to spendings
        spending.setRepeat(bill.getRepeat());
        spending.setRecurring(!"No".equals(bill.getRepeat()));
        spending.setBillId(bill.getBillId());
        spending.setParentBillId(bill.getParentBillId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.DocumentReference spendingRef = db.collection("users")
                .document(userUid).collection("spendings").document(bill.getBillId());
        com.google.firebase.firestore.DocumentReference billRef = db.collection("users")
                .document(userUid).collection("bills").document(bill.getBillId());

        db.runBatch(batch -> {
                    batch.set(spendingRef, spending);
                    batch.delete(billRef);
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, R.string.payment_moved_to_closed, Toast.LENGTH_SHORT).show();
                    PaymentNotificationScheduler.INSTANCE.cancelBill(context, bill.getBillId());
                    // Do not mutate this grouped adapter list directly. The Firestore listener
                    // in MyBills rebuilds the filtered source list after the bill is deleted.
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    billHolderRollback(adapterPosition);
                    Toast.makeText(context, context.getString(R.string.payment_move_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void billHolderRollback(int adapterPosition) {
        if (adapterPosition >= 0 && adapterPosition < itemsArrayList.size()) {
            notifyItemChanged(adapterPosition);
        } else {
            notifyDataSetChanged();
        }
    }

    // Método para formatear el Timestamp a String
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    // Método para formatear el Timestamp a mes y año
    private String formatMonthYear(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
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
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                onBillClickListener.onBillClick(position);
            }
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
        sourceBillsArrayList = new ArrayList<>(newBills);
        itemsArrayList = groupBillsByMonth(newBills);
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
            itemsArrayList = groupBillsByMonth(sourceBillsArrayList);
            notifyDataSetChanged();
        });
        holder.monthHeader.setTextSize(16);
        holder.monthHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        holder.monthHeader.setAllCaps(true);
    }

    // Method to group open payments by urgency: overdue -> this month -> upcoming -> no date.
    private ArrayList<Object> groupBillsByMonth(ArrayList<Bills> billsArrayList) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        Map<Integer, Map<Integer, List<Bills>>> overdueBills = new TreeMap<>();
        Map<Integer, Map<Integer, List<Bills>>> currentMonthBills = new TreeMap<>();
        Map<Integer, Map<Integer, List<Bills>>> upcomingBills = new TreeMap<>();
        List<Bills> noDateBills = new ArrayList<>();

        for (Bills bill : billsArrayList) {
            if (bill.getDate() == null) {
                noDateBills.add(bill);
                continue;
            }
            Date billDate = bill.getDate().toDate();
            Calendar billCalendar = Calendar.getInstance();
            billCalendar.setTime(billDate);
            billCalendar.set(Calendar.HOUR_OF_DAY, 0);
            billCalendar.set(Calendar.MINUTE, 0);
            billCalendar.set(Calendar.SECOND, 0);
            billCalendar.set(Calendar.MILLISECOND, 0);

            int billMonth = billCalendar.get(Calendar.MONTH);
            int billYear = billCalendar.get(Calendar.YEAR);

            if (billCalendar.before(today)) {
                overdueBills.computeIfAbsent(billYear, k -> new TreeMap<>())
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            } else if (billYear == currentYear && billMonth == currentMonth) {
                currentMonthBills.computeIfAbsent(billYear, k -> new TreeMap<>())
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            } else {
                upcomingBills.computeIfAbsent(billYear, k -> new TreeMap<>())
                        .computeIfAbsent(billMonth, k -> new ArrayList<>())
                        .add(bill);
            }
        }

        Comparator<Bills> dateComparator = Comparator.comparing(b -> b.getDate().toDate());
        overdueBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator)));
        currentMonthBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator)));
        upcomingBills.values().forEach(monthMap -> monthMap.values().forEach(bills -> bills.sort(dateComparator)));
        noDateBills.sort(Comparator.comparing(b -> b.getName() == null ? "" : b.getName().toLowerCase(Locale.getDefault())));

        ArrayList<Object> items = new ArrayList<>();

        addGroupedSection(items, overdueBills, sdf, R.string.section_overdue_open_payments);
        addGroupedSection(items, currentMonthBills, sdf, R.string.section_this_month_open_payments);
        addGroupedSection(items, upcomingBills, sdf, R.string.section_upcoming_open_payments);
        if (!noDateBills.isEmpty()) {
            items.add(new SectionHeader(R.string.section_no_date));
            if (!collapsedSections.contains(R.string.section_no_date)) {
                items.addAll(noDateBills);
            }
        }

        return items;
    }

    private void addGroupedSection(
            ArrayList<Object> items,
            Map<Integer, Map<Integer, List<Bills>>> groupedBills,
            SimpleDateFormat sdf,
            int sectionTitleRes
    ) {
        if (groupedBills.isEmpty()) {
            return;
        }
        items.add(new SectionHeader(sectionTitleRes));
        if (collapsedSections.contains(sectionTitleRes)) {
            return;
        }
        for (Map.Entry<Integer, Map<Integer, List<Bills>>> entry : groupedBills.entrySet()) {
            for (Map.Entry<Integer, List<Bills>> monthEntry : entry.getValue().entrySet()) {
                String monthYear = sdf.format(new GregorianCalendar(entry.getKey(), monthEntry.getKey(), 1).getTime());
                items.add(monthYear);
                items.addAll(monthEntry.getValue());
            }
        }
    }

}
