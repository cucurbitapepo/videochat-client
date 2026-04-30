package com.example.videochat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videochat.R;
import com.example.videochat.dto.UserSearchResultDto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_HEADER = 0;
  private static final int VIEW_TYPE_ITEM = 1;

  private List<Object> items = new ArrayList<>();

  private final Function<UserSearchResultDto, Void> itemClickListener;

  public ContactAdapter(Function<UserSearchResultDto, Void> itemClickListener) {
    this.itemClickListener = itemClickListener;
  }

  @Override
  public int getItemViewType(int position) {
    return (items.get(position) instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());

    if (viewType == VIEW_TYPE_HEADER) {
      View view = inflater.inflate(R.layout.item_contact_header, parent, false);
      return new HeaderViewHolder(view);
    } else {
      View view = inflater.inflate(R.layout.item_contact, parent, false);
      return new ContactViewHolder(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof HeaderViewHolder) {
      ((HeaderViewHolder) holder).bind((String) items.get(position));
    } else if (holder instanceof ContactViewHolder) {
      ((ContactViewHolder) holder).bind((UserSearchResultDto) items.get(position), itemClickListener);
    }
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public void setContacts(List<UserSearchResultDto> contacts) {
    items.clear();

    if (!contacts.isEmpty()) {
      items.add("Ваши контакты");
      items.addAll(contacts);
    }

    notifyDataSetChanged();
  }

  public void setSearchResults(List<UserSearchResultDto> contacts, List<UserSearchResultDto> others) {
    items.clear();

    if (!contacts.isEmpty()) {
      items.add("Ваши контакты");
      items.addAll(contacts);
    }

    if (!others.isEmpty()) {
      if (!items.isEmpty()) items.add(" ");
      items.add("Другие пользователи");
      items.addAll(others);
    }

    notifyDataSetChanged();
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    TextView headerText;

    HeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerText = itemView.findViewById(R.id.header_text);
    }

    void bind(String header) {
      headerText.setText(header);
    }
  }

  static class ContactViewHolder extends RecyclerView.ViewHolder {
    ImageView avatar;
    TextView username;
    View onlineIndicator;

    ContactViewHolder(@NonNull View itemView) {
      super(itemView);
      avatar = itemView.findViewById(R.id.avatar);
      username = itemView.findViewById(R.id.username);
      onlineIndicator = itemView.findViewById(R.id.online_indicator);
    }

    void bind(UserSearchResultDto user, Function<UserSearchResultDto, Void> clickListener) {
      username.setText(user.getUsername());
      onlineIndicator.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

      avatar.setImageResource(user.isOnline() ?
//              R.drawable.ic_avatar_online : R.drawable.ic_avatar_offline);
              R.drawable.ic_avatar_default : R.drawable.ic_avatar_default);

      itemView.setTag(user);
      itemView.setOnClickListener(v -> {
        if (clickListener != null) {
          clickListener.apply(user);
        }
      });
    }
  }
}
