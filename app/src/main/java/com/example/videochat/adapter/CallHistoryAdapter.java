package com.example.videochat.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videochat.R;
import com.example.videochat.dto.CallStatus;

import java.util.ArrayList;
import java.util.List;

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {

  private final List<CallHistoryItem> items = new ArrayList<>();
  private final OnCallItemClickListener listener;

  public interface OnCallItemClickListener {
    void onCallItemClick(CallHistoryItem item);
  }

  public CallHistoryAdapter(OnCallItemClickListener listener) {
    this.listener = listener;
  }

  public void updateItems(List<CallHistoryItem> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_call_history, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    CallHistoryItem item = items.get(position);
    holder.bind(item);
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) listener.onCallItemClick(item);
    });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    private final TextView nameView;
    private final TextView timeView;
    private final TextView durationView;
    private final ImageView statusIconView;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      nameView = itemView.findViewById(R.id.caller_name);
      timeView = itemView.findViewById(R.id.call_time);
      durationView = itemView.findViewById(R.id.call_duration);
      statusIconView = itemView.findViewById(R.id.call_status_icon);
    }

    void bind(CallHistoryItem item) {
      nameView.setText(item.getCounterpartName());
      timeView.setText(item.getFormattedStartTime());
      durationView.setText(item.getFormattedDuration());
      statusIconView.setImageResource(item.getStatusIconRes());

      if (item.getStatus() == CallStatus.REJECTED || item.getStatus() == CallStatus.CANCELLED) {
        durationView.setTextColor(Color.RED);
      } else {
        durationView.setTextColor(Color.GRAY);
      }
    }
  }
}