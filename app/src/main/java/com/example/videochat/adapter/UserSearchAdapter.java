package com.example.videochat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.videochat.R;
import com.example.videochat.dto.UserSearchResultDto;
import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

  private List<UserSearchResultDto> users = new ArrayList<>();

  public void setUsers(List<UserSearchResultDto> users) {
    this.users = users != null ? users : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user_search, parent, false);
    return new UserViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
    UserSearchResultDto user = users.get(position);
    holder.usernameView.setText(user.getUsername());
    holder.onlineView.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);
  }

  @Override
  public int getItemCount() {
    return users.size();
  }

  static class UserViewHolder extends RecyclerView.ViewHolder {
    TextView usernameView;
    View onlineView;

    UserViewHolder(@NonNull View itemView) {
      super(itemView);
      usernameView = itemView.findViewById(R.id.username);
      onlineView = itemView.findViewById(R.id.online_indicator);
    }
  }
}