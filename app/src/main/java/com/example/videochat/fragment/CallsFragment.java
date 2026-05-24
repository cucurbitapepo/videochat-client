package com.example.videochat.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.videochat.R;
import com.example.videochat.adapter.CallHistoryAdapter;
import com.example.videochat.adapter.CallHistoryItem;
import com.example.videochat.api.ApiClient;
import com.example.videochat.dto.CallDto;
import com.example.videochat.util.JwtUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallsFragment extends Fragment {

  private static final String TAG = "CallsFragment";
  private static final int HISTORY_LIMIT = 20;

  private RecyclerView callsRecycler;
  private CallHistoryAdapter adapter;
  private TextView emptyStateView;
  private TextView titleView;
  private ProgressBar loadingIndicator;

  private final List<CallHistoryItem> historyItems = new ArrayList<>();

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_calls, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    initViews(view);
    setupRecyclerView();
    loadCallHistory();
  }

  private void initViews(View view) {
    callsRecycler = view.findViewById(R.id.calls_recycler);
    emptyStateView = view.findViewById(R.id.empty_state);
    titleView = view.findViewById(R.id.history_title);
    loadingIndicator = view.findViewById(R.id.loading_indicator);
  }

  private void setupRecyclerView() {
    adapter = new CallHistoryAdapter(item -> {
      Log.d(TAG, "Clicked call: " + item.getCallId());
    });

    callsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    callsRecycler.setAdapter(adapter);
  }

  private void loadCallHistory() {
    setLoading(true);

    ApiClient.getCallApi().getCallHistory(HISTORY_LIMIT)
            .enqueue(new Callback<List<CallDto>>() {
              @Override
              public void onResponse(Call<List<CallDto>> call, Response<List<CallDto>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                  processCallHistory(response.body());
                } else {
                  showError("Не удалось загрузить историю звонков");
                }
              }

              @Override
              public void onFailure(Call<List<CallDto>> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Failed to load call history", t);
                showError("Ошибка сети: " + t.getMessage());
              }
            });
  }

  private void processCallHistory(List<CallDto> calls) {
    if (calls.isEmpty()) {
      updateUI(Collections.emptyList());
      return;
    }

    Long currentUserId = JwtUtils.getCurrentUserId();

    Set<Long> counterpartIds = new HashSet<>();
    for (CallDto call : calls) {
      for (Long participantId : call.getParticipants()) {
        if (!participantId.equals(currentUserId)) {
          counterpartIds.add(participantId);
        }
      }
    }

    if (!counterpartIds.isEmpty()) {
      ApiClient.getUserApi().getUsernamesByIds(counterpartIds)
              .enqueue(new Callback<Map<Long, String>>() {
                @Override
                public void onResponse(Call<Map<Long, String>> call, Response<Map<Long, String>> response) {
                  Map<Long, String> usernames = new HashMap<>();

                  if (response.isSuccessful() && response.body() != null) {
                    usernames.putAll(response.body());
                  }

                  buildHistoryItems(calls, currentUserId, usernames);
                }

                @Override
                public void onFailure(Call<Map<Long, String>> call, Throwable t) {
                  Log.w(TAG, "Failed to load usernames, using placeholders", t);
                  buildHistoryItems(calls, currentUserId, Collections.emptyMap());
                }
              });
    } else {
      updateUI(Collections.emptyList());
    }
  }

  private void buildHistoryItems(List<CallDto> calls, Long currentUserId, Map<Long, String> usernames) {
    historyItems.clear();

    for (CallDto call : calls) {
      Long counterpartId = findCounterpartId(call, currentUserId);
      String counterpartName = usernames.getOrDefault(counterpartId, "Собеседник");

      boolean isOutgoing = call.getCallerId().equals(currentUserId);

      Duration duration = null;
      if (call.getEndedAt() != null && call.getCreatedAt() != null) {
        duration = Duration.between(call.getCreatedAt(), call.getEndedAt());
      }

      CallHistoryItem item = new CallHistoryItem(
              call.getCallId(),
              counterpartName,
              call.getCreatedAt(),
              duration,
              call.getStatus(),
              isOutgoing
      );
      historyItems.add(item);
    }

    updateUI(historyItems);
  }

  private Long findCounterpartId(CallDto call, Long currentUserId) {
    for (Long participantId : call.getParticipants()) {
      if (!participantId.equals(currentUserId)) {
        return participantId;
      }
    }
    return null;
  }

  private void updateUI(List<CallHistoryItem> items) {
    if (items.isEmpty()) {
      titleView.setVisibility(View.GONE);
      emptyStateView.setVisibility(View.VISIBLE);
      callsRecycler.setVisibility(View.GONE);
    } else {
      titleView.setVisibility(View.VISIBLE);
      emptyStateView.setVisibility(View.GONE);
      callsRecycler.setVisibility(View.VISIBLE);
      adapter.updateItems(items);
    }
  }

  private void setLoading(boolean loading) {
    if (loading) {
      loadingIndicator.setVisibility(View.VISIBLE);
      callsRecycler.setVisibility(View.GONE);
      emptyStateView.setVisibility(View.GONE);
    } else {
      loadingIndicator.setVisibility(View.GONE);
    }
  }

  private void showError(String message) {
    if (getContext() != null) {
      Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    updateUI(Collections.emptyList());
  }
}