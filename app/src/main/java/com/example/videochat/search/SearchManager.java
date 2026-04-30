package com.example.videochat.search;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.example.videochat.api.ApiClient;
import com.example.videochat.api.UserApi;
import com.example.videochat.dto.PageResponse;
import com.example.videochat.dto.UserSearchResultDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class SearchManager {
  private final UserApi userApi;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable searchRunnable = this::performSearch;
  private String lastQuery;
  private SearchCallback callback;
  private Call<PageResponse<UserSearchResultDto>> currentCall;

  public SearchManager() {
    this.userApi = ApiClient.getUserApi();
  }

  public void setSearchCallback(SearchCallback callback) {
    this.callback = callback;
  }

  public void query(String text) {
    if (currentCall != null) {
      currentCall.cancel();
    }

    if (callback != null) {
      callback.onLoading(false);
    }

    if (text == null || text.trim().length() < 3) {
      if (callback != null) {
        callback.onResults(null);
        callback.onEmptyQuery();
      }
      return;
    }

    lastQuery = text.trim();
    handler.removeCallbacks(searchRunnable);
    handler.postDelayed(searchRunnable, 500);

    if (callback != null) {
      callback.onLoading(true);
    }
  }

  private void performSearch() {
    if (lastQuery == null || callback == null) return;

    currentCall = userApi.searchUsers(lastQuery);
    currentCall.enqueue(new Callback<>() {
      @Override
      public void onResponse(@NonNull Call<PageResponse<UserSearchResultDto>> call,
                             @NonNull Response<PageResponse<UserSearchResultDto>> response) {
        if (response.isSuccessful() && response.body() != null && !response.body().getContent().isEmpty()) {
          callback.onResults(response.body().getContent());
        } else {
          callback.onResults(null);
        }
        callback.onLoading(false);
      }

      @Override
      public void onFailure(@NonNull Call<PageResponse<UserSearchResultDto>> call,
                            @NonNull Throwable t) {
        if (call.isCanceled()) {
          return;
        }
        callback.onError("Не удалось выполнить поиск: " + t.getMessage());
        callback.onLoading(false);
      }
    });
  }
  public interface SearchCallback {
    void onResults(List<UserSearchResultDto> results);
    void onLoading(boolean isLoading);
    void onError(String message);
    void onEmptyQuery();
  }
}
