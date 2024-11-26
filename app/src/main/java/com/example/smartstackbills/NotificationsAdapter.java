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

    public NotificationsAdapter(Context context, List<Notifications> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
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
}
