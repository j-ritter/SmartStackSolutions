package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notifications> notificationsList;
    private OnNotificationClickListener listener; // Listener for handling clicks

    // Constructor with listener parameter
    public NotificationsAdapter(Context context, List<Notifications> notificationsList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationsList = notificationsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.items_notifications, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notifications currentNotification = notificationsList.get(position);
        holder.title.setText(currentNotification.getTitle());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.date.setText(sdf.format(currentNotification.getDate().toDate()));
        holder.amount.setText(currentNotification.getAmount());

        // Set click listener to handle notification click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(currentNotification.getNotificationId()); // Pass notificationId
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, amount;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
        }
    }

    // Interface for click handling
    public interface OnNotificationClickListener {
        void onNotificationClick(String notificationId); // Pass only the ID for marking as read
    }
}
