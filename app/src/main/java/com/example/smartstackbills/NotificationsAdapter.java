import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartstackbills.Notifications;
import com.example.smartstackbills.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_NOTIFICATION = 0;
    private static final int ITEM_MONTH_HEADER = 1;

    private Context context;
    private List<Object> groupedNotificationsList;
    private OnNotificationClickListener listener;

    public NotificationsAdapter(Context context, List<Notifications> notificationsList, OnNotificationClickListener listener) {
        this.context = context;
        this.groupedNotificationsList = groupNotificationsByMonth(notificationsList);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return groupedNotificationsList.get(position) instanceof Notifications ? ITEM_NOTIFICATION : ITEM_MONTH_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_NOTIFICATION) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.items_notifications, parent, false);
            return new NotificationViewHolder(itemView);
        } else {
            View headerView = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
            return new MonthHeaderViewHolder(headerView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_NOTIFICATION) {
            NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;
            Notifications currentNotification = (Notifications) groupedNotificationsList.get(position);

            notificationHolder.title.setText(currentNotification.getTitle());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            notificationHolder.date.setText(sdf.format(currentNotification.getDate().toDate()));
            notificationHolder.amount.setText(currentNotification.getAmount());
            notificationHolder.createdAt.setText(sdf.format(currentNotification.getCreatedAt().toDate()));

            notificationHolder.unreadDot.setVisibility(currentNotification.isUnread() ? View.VISIBLE : View.GONE);

            notificationHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(currentNotification.getNotificationId());
                    currentNotification.setUnread(false);
                    notifyItemChanged(position);
                }
            });

            notificationHolder.deleteIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteNotificationClick(currentNotification.getNotificationId());
                }
            });
        } else {
            MonthHeaderViewHolder headerHolder = (MonthHeaderViewHolder) holder;
            String monthHeader = (String) groupedNotificationsList.get(position);
            headerHolder.monthHeader.setText(monthHeader);
        }
    }

    @Override
    public int getItemCount() {
        return groupedNotificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, amount, createdAt;
        ImageView unreadDot, deleteIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
            createdAt = itemView.findViewById(R.id.tvNotificationTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            deleteIcon = itemView.findViewById(R.id.imgDeleteNotification);
        }
    }

    public static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView monthHeader;

        public MonthHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            monthHeader = itemView.findViewById(R.id.textviewMonthHeader);
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(String notificationId);
        void onDeleteNotificationClick(String notificationId);
    }

    private List<Object> groupNotificationsByMonth(List<Notifications> notificationsList) {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        TreeMap<String, List<Notifications>> groupedMap = new TreeMap<>();

        for (Notifications notification : notificationsList) {
            if (notification.getDate() == null) continue;
            String monthYear = monthYearFormat.format(notification.getDate().toDate());
            groupedMap.computeIfAbsent(monthYear, k -> new ArrayList<>()).add(notification);
        }

        List<Object> groupedNotifications = new ArrayList<>();
        for (Map.Entry<String, List<Notifications>> entry : groupedMap.entrySet()) {
            groupedNotifications.add(entry.getKey()); // Add month header
            groupedNotifications.addAll(entry.getValue()); // Add notifications for that month
        }

        return groupedNotifications;
    }
}
