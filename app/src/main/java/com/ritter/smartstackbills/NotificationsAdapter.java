package com.ritter.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_NOTIFICATION = 0;
    private static final int ITEM_WEEK_HEADER = 1;

    private Context context;
    private List<Object> groupedNotificationsList;
    private OnNotificationClickListener listener;

    public NotificationsAdapter(Context context, List<Notifications> notificationsList, OnNotificationClickListener listener) {
        this.context = context;
        this.groupedNotificationsList = groupNotificationsByWeek(notificationsList);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return groupedNotificationsList.get(position) instanceof Notifications ? ITEM_NOTIFICATION : ITEM_WEEK_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_NOTIFICATION) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.items_notifications, parent, false);
            return new NotificationViewHolder(itemView);
        } else {
            View headerView = LayoutInflater.from(context).inflate(R.layout.item_week_header, parent, false);
            return new WeekHeaderViewHolder(headerView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ITEM_NOTIFICATION) {
            NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;
            Notifications currentNotification = (Notifications) groupedNotificationsList.get(position);

            notificationHolder.title.setText(currentNotification.getTitle());
            notificationHolder.message.setText(
                    currentNotification.getMessage() != null
                            ? currentNotification.getMessage()
                            : currentNotification.getPaymentName()
            );
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            notificationHolder.date.setText(
                    currentNotification.getDate() != null
                            ? context.getString(
                                    R.string.notification_due_date_short,
                                    sdf.format(currentNotification.getDate().toDate())
                            )
                            : ""
            );

            notificationHolder.createdAt.setText(
                    currentNotification.getCreatedAt() != null
                            ? new SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(currentNotification.getCreatedAt().toDate())
                            : ""
            );

            notificationHolder.unreadDot.setVisibility(currentNotification.isUnread() ? View.VISIBLE : View.GONE);
            String type = currentNotification.getType();
            if (PaymentNotificationScheduler.EVENT_ADVANCE.equals(type)) {
                notificationHolder.iconBackground.setBackgroundResource(R.drawable.circle_notification_blue);
            } else if (PaymentNotificationScheduler.EVENT_DUE_TODAY.equals(type)) {
                notificationHolder.iconBackground.setBackgroundResource(R.drawable.circle_notification_orange);
            } else {
                notificationHolder.iconBackground.setBackgroundResource(R.drawable.circle_notification_red);
            }

            notificationHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(
                            currentNotification.getNotificationId(),
                            currentNotification.getBillId() != null
                                    ? currentNotification.getBillId()
                                    : currentNotification.getNotificationId()
                    );
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
            WeekHeaderViewHolder headerHolder = (WeekHeaderViewHolder) holder;
            String weekHeader = (String) groupedNotificationsList.get(position);
            headerHolder.weekHeader.setText(weekHeader);
        }
    }

    @Override
    public int getItemCount() {
        return groupedNotificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, date, amount, createdAt;
        ImageView unreadDot, deleteIcon;
        FrameLayout iconBackground;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            message = itemView.findViewById(R.id.tvMessageNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
            createdAt = itemView.findViewById(R.id.tvNotificationTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            deleteIcon = itemView.findViewById(R.id.imgDeleteNotification);
            iconBackground = itemView.findViewById(R.id.notificationIconBackground);
        }
    }

    public static class WeekHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView weekHeader;

        public WeekHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            weekHeader = itemView.findViewById(R.id.textviewWeekHeader);
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(String notificationId, String billId);
        void onDeleteNotificationClick(String notificationId);
    }

    private List<Object> groupNotificationsByWeek(List<Notifications> notificationsList) {
        Map<String, List<Notifications>> groupedMap = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("'CW' w, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (Notifications notification : notificationsList) {
            if (notification.getCreatedAt() == null) continue;

            calendar.setTime(notification.getCreatedAt().toDate());
            String weekKey = sdf.format(calendar.getTime());

            groupedMap.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(notification);
        }

        List<Object> groupedNotifications = new ArrayList<>();
        for (Map.Entry<String, List<Notifications>> entry : groupedMap.entrySet()) {
            groupedNotifications.add(entry.getKey()); // Add week header
            groupedNotifications.addAll(entry.getValue()); // Add notifications for that week
        }

        return groupedNotifications;
    }
}
