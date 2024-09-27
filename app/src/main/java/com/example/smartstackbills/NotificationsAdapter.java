package com.example.smartstackbills;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private Context context;
    private List<NotificationsActivity.NotificationItem> notificationsList;
    private OnNotificationClickListener onNotificationClickListener;

    public NotificationsAdapter(Context context, List<NotificationsActivity.NotificationItem> notificationsList, OnNotificationClickListener onNotificationClickListener) {
        this.context = context;
        this.notificationsList = notificationsList;
        this.onNotificationClickListener = onNotificationClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.items_notifications, parent, false);
        return new NotificationViewHolder(itemView, onNotificationClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationsActivity.NotificationItem currentNotification = notificationsList.get(position);
        holder.title.setText(currentNotification.getTitle());

        // Convert String (date) to Date and format it
        String dateString = currentNotification.getDate();
        if (dateString != null && !dateString.isEmpty()) {
            String formattedDate = formatDateString(dateString);
            holder.date.setText(formattedDate);
        } else {
            holder.date.setText("No Date Available");
        }

        // Show or hide the orange dot based on the unread status
        if (currentNotification.isUnread()) {
            holder.unreadDot.setVisibility(View.VISIBLE);  // Corrected this line
        } else {
            holder.unreadDot.setVisibility(View.GONE);  // Corrected this line
        }
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title, date, amount;
        View unreadDot;
        OnNotificationClickListener onNotificationClickListener;

        public NotificationViewHolder(@NonNull View itemView, OnNotificationClickListener onNotificationClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            this.onNotificationClickListener = onNotificationClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onNotificationClickListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onNotificationClickListener.onNotificationClick(position);
                }
            }
        }
    }

    // Helper method to format String date to a more readable format
    private String formatDateString(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateString);
            return sdf.format(date);
        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(int position);
    }
}
