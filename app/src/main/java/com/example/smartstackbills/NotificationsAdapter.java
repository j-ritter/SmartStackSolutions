package com.example.smartstackbills;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        // Format and bind the createdAt timestamp (null check added)
        Date createdAt = currentNotification.getCreatedAt();
        if (createdAt != null) {
            String createdAtString = formatTimestamp(createdAt);
            holder.notificationTime.setText(createdAtString); // Bind the time to the TextView
        } else {
            holder.notificationTime.setText("No Time Available");
        }

        // Show or hide the orange dot based on the unread status
        if (currentNotification.isUnread()) {
            holder.unreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.unreadDot.setVisibility(View.GONE);
        }

        // Set click listener for deleting the notification
        holder.imgDeleteNotification.setOnClickListener(view -> {
            deleteNotification(currentNotification, position);
        });
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MyBills.class);
            intent.putExtra("BILL_ID", currentNotification.getBillId());  // Pass the billId
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title, date, amount, notificationTime; // Added notificationTime TextView
        View unreadDot;
        ImageView imgDeleteNotification;
        OnNotificationClickListener onNotificationClickListener;

        public NotificationViewHolder(@NonNull View itemView, OnNotificationClickListener onNotificationClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
            notificationTime = itemView.findViewById(R.id.tvNotificationTime); // Initialize notificationTime TextView
            unreadDot = itemView.findViewById(R.id.unreadDot);
            imgDeleteNotification = itemView.findViewById(R.id.imgDeleteNotification);  // Initialize delete icon

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

    // Helper method to format the timestamp into a readable format
    private String formatTimestamp(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd/MM/yyyy", Locale.getDefault());
        return sdf.format(timestamp);
    }

    // Method to delete the notification
    private void deleteNotification(NotificationsActivity.NotificationItem notification, int position) {
        notificationsList.remove(position);
        notifyItemRemoved(position);
        removeNotificationFromSharedPreferences(notification);
    }

    // Method to remove notification from SharedPreferences
    private void removeNotificationFromSharedPreferences(NotificationsActivity.NotificationItem notification) {
        SharedPreferences sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE);
        String json = sharedPref.getString("notificationsList", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<NotificationsActivity.NotificationItem>>() {}.getType();
            ArrayList<NotificationsActivity.NotificationItem> currentList = gson.fromJson(json, type);
            currentList.removeIf(item -> item.getTitle().equals(notification.getTitle()) && item.getDate().equals(notification.getDate()));
            SharedPreferences.Editor editor = sharedPref.edit();
            String updatedJson = gson.toJson(currentList);
            editor.putString("notificationsList", updatedJson);
            editor.apply();
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(int position);
        void onDeleteClick(int position);
    }
}
