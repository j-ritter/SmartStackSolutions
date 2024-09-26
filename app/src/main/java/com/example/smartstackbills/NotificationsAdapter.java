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
    private List<NotificationItem> notificationsList;
    private OnNotificationClickListener onNotificationClickListener;

    public NotificationsAdapter(Context context, List<NotificationItem> notificationsList, OnNotificationClickListener onNotificationClickListener) {
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
        NotificationItem currentNotification = notificationsList.get(position);
        holder.title.setText(currentNotification.getTitle());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.date.setText(sdf.format(currentNotification.getDate()));
        holder.amount.setText(currentNotification.getAmount());

    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title, date, amount;
        OnNotificationClickListener onNotificationClickListener;

        public NotificationViewHolder(@NonNull View itemView, OnNotificationClickListener onNotificationClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitleNotification);
            date = itemView.findViewById(R.id.tvDateNotification);
            amount = itemView.findViewById(R.id.tvAmountNotification);
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
    public interface OnNotificationClickListener {
        void onNotificationClick(int position);
    }
}