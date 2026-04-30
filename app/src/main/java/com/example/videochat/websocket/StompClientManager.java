package com.example.videochat.websocket;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.videochat.dto.NotificationDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class StompClientManager {
  private static final String TAG = "StompClient";
  private static final String WS_URL = "wss://84.54.59.153:8443/api/v1/ws";

  private final Context context;
  private final Gson gson;
  private final MutableLiveData<NotificationDto> notificationEvent = new MutableLiveData<>();
  private final MutableLiveData<Boolean> connectionState = new MutableLiveData<>();

  private StompClient stompClient;
  private final CompositeDisposable disposables = new CompositeDisposable();
  private boolean isConnected = false;
  private String authToken;

  public StompClientManager(Context context) {
    this.context = context;
    this.gson = new GsonBuilder().create();
    loadAuthToken();
  }

  public void clearPendingNotifications() {
    notificationEvent.postValue(null);
    Log.d(TAG, "Cleared pending notifications");
  }

  private void loadAuthToken() {
    authToken = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .getString("auth_token", null);
  }

  public void connect() {
    if (isConnected || stompClient != null) {
      return;
    }

    if (authToken == null) {
      Log.e(TAG, "Cannot connect: authentication token is null");
      connectionState.postValue(false);
      return;
    }

    try {
      OkHttpClient httpClient = new OkHttpClient.Builder()
              .connectTimeout(30, TimeUnit.SECONDS)
              .readTimeout(30, TimeUnit.SECONDS)
              .writeTimeout(30, TimeUnit.SECONDS)
              .build();

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer " + authToken);

      stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL, headers, httpClient);

      disposables.add(stompClient.lifecycle()
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                  case OPENED:
                    Log.d(TAG, "WebSocket connected");
                    isConnected = true;
                    connectionState.postValue(true);
                    subscribeToNotifications();
                    break;

                  case ERROR:
                    Log.e(TAG, "WebSocket error", lifecycleEvent.getException());
                    break;

                  case CLOSED:
                    Log.d(TAG, "WebSocket closed");
                    isConnected = false;
                    connectionState.postValue(false);
                    break;
                }
              }));


      stompClient.connect();

    } catch (Exception e) {
      Log.e(TAG, "WebSocket connection error", e);
      connectionState.postValue(false);
    }
  }

  private void subscribeToNotifications() {
    if (stompClient == null || !isConnected) {
      Log.w(TAG, "Cannot subscribe: not connected");
      return;
    }

    String topicPath = "/user/queue/notifications";
    Log.d(TAG, "Attempting to subscribe to: " + topicPath);


    Disposable messageDisposable = stompClient.topic("/user/queue/notifications")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
              Log.d(TAG, "Received WebSocket message: " + topicMessage.getPayload());
              try {
                NotificationDto notification = gson.fromJson(topicMessage.getPayload(), NotificationDto.class);
                notificationEvent.postValue(notification);
                Log.d(TAG, "Notification posted to LiveData: type=" + notification.getType());
              } catch (JsonSyntaxException e) {
                Log.e(TAG, "Error parsing notification", e);
                Log.e(TAG, "Error parsing notification", e);
              }
            }, throwable -> {
              Log.e(TAG, "Subscription error", throwable);
            });

    disposables.add(messageDisposable);

    Log.d(TAG, "Subscription active");
  }

  public Disposable subscribeToRoomNotifications(String roomId) {
    if (stompClient == null || !isConnected) {
      Log.w(TAG, "Cannot subscribe to room: not connected");
      return null;
    }

    String topicPath = "/topic/room/" + roomId;
    Log.d(TAG, "Subscribing to room notifications: " + topicPath);

    return stompClient.topic(topicPath)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    topicMessage -> {
                      Log.d(TAG, "Room notification: " + topicMessage.getPayload());
                      try {
                        NotificationDto notification = gson.fromJson(
                                topicMessage.getPayload(),
                                NotificationDto.class
                        );
                        notificationEvent.postValue(notification);
                      } catch (JsonSyntaxException e) {
                        Log.e(TAG, "Error parsing room notification", e);
                      }
                    },
                    throwable -> Log.e(TAG, "Room subscription error", throwable)
            );
  }

  public void unsubscribeFromRoomNotifications(String roomId) {
    if (stompClient != null && isConnected) {
      String topicPath = "/topic/room/" + roomId;
      Log.d(TAG, "Unsubscribing from room notifications: " + topicPath);
    }
  }

  public void disconnect() {
    isConnected = false;
    connectionState.postValue(false);

    if (disposables != null && !disposables.isDisposed()) {
      disposables.clear();
    }

    if (stompClient != null) {
      stompClient.disconnect();
      stompClient = null;
    }

    Log.d(TAG, "WebSocket disconnected");
  }

  public MutableLiveData<NotificationDto> getNotificationEvent() {
    return notificationEvent;
  }

  public MutableLiveData<Boolean> getConnectionState() {
    return connectionState;
  }

  public boolean isConnected() {
    return isConnected;
  }

  public Completable send(String destination, String message) {
    if (stompClient == null || !isConnected) {
      Log.w(TAG, "Cannot send message: not connected to WebSocket");
      return Completable.error(new Exception("WebSocket is not connected"));
    }

    return stompClient.send(destination, message);
  }

  public void handleCallRequest(NotificationDto notification) {
    if ("CALL_REQUEST".equals(notification.getType())) {
      Log.d(TAG, "Incoming call request: " + notification.getData());
    }
  }
}