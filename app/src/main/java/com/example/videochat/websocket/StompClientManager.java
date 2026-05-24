package com.example.videochat.websocket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.videochat.dto.NotificationDto;
import com.example.videochat.encryption.DhPublicKeyMessage;
import com.example.videochat.encryption.E2eeReadyMessage;
import com.example.videochat.encryption.WrappedGroupKeyMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;

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
  private static final String WS_URL = "wss://vkr-videochat.duckdns.org/api/v1/ws";

  private final Context context;
  private final Gson gson;
  private final MutableLiveData<NotificationDto> notificationEvent = new MutableLiveData<>();
  private final MutableLiveData<Boolean> connectionState = new MutableLiveData<>();

  private StompClient stompClient;
  private final CompositeDisposable disposables = new CompositeDisposable();
  private boolean isConnected = false;
  private String authToken;

  private int reconnectAttempts = 0;
  private static final int MAX_RECONNECT_ATTEMPTS = 5;
  private static final long INITIAL_RECONNECT_DELAY_MS = 2000;

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
                    reconnectAttempts = 0;
                    Log.d(TAG, "WebSocket connected");
                    isConnected = true;
                    connectionState.postValue(true);
                    subscribeToNotifications();
                    break;

                  case ERROR:
                    Log.e(TAG, "WebSocket error", lifecycleEvent.getException());
                    Throwable error = lifecycleEvent.getException();
                    boolean isRecoverable = (error instanceof java.net.SocketException ||
                                             error instanceof java.io.EOFException ||
                                             error instanceof SSLException ||
                                             (error.getCause() != null && (
                                                     error.getCause() instanceof java.net.SocketException ||
                                                     error.getCause() instanceof java.io.EOFException ||
                                                     error.getCause() instanceof java.io.IOException ||
                                                     error.getCause() instanceof javax.net.ssl.SSLException
                                             )));
                    if (isRecoverable) {

                      reconnectAttempts++;
                      if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
                        Log.e(TAG, "Max reconnect attempts (" + MAX_RECONNECT_ATTEMPTS + ") reached, giving up");
                        connectionState.postValue(false);
                        return;
                      }

                      long delay = Math.min(INITIAL_RECONNECT_DELAY_MS * reconnectAttempts, 30000);
                      Log.w(TAG, "Reconnect attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS +
                                 " in " + delay + "ms");

                      isConnected = false;
                      connectionState.postValue(false);
                      if (stompClient != null) {
                        try {
                          stompClient.disconnect();
                        } catch (Exception e) { /* ignore */ }
                        stompClient = null;
                      }

                      new Handler(Looper.getMainLooper())
                              .postDelayed(StompClientManager.this::connect, delay);
                    }
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
      return Disposables.disposed();
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
      return Completable.complete();
    }

    return stompClient.send(destination, message);
  }

  public void handleCallRequest(NotificationDto notification) {
    if ("CALL_REQUEST".equals(notification.getType())) {
      Log.d(TAG, "Incoming call request: " + notification.getData());
    }
  }

  public Completable sendDhPublicKey(String callId, String userId, String publicKeyBase64) {
    DhPublicKeyMessage message = new DhPublicKeyMessage(userId, publicKeyBase64, callId);
    String json = gson.toJson(message);
    String destination = "/app/call/" + callId + "/e2ee/dh-key";
    return send(destination, json)
            .retryWhen((Flowable<Throwable> errors) ->
                    errors.zipWith(
                                    Flowable.range(1, 3),
                                    (Throwable error, Integer attempt) -> {
                                      if (attempt < 3) {
                                        Log.d(TAG, "Retrying sendDhPublicKey, attempt " + attempt);
                                        return attempt;
                                      } else {
                                        throw Exceptions.propagate(error);
                                      }
                                    }
                            )
                            .flatMap((Integer attempt) ->
                                    Flowable.timer(attempt, TimeUnit.SECONDS)
                            )
            );
  }

  public Completable sendWrappedGroupKey(String callId, WrappedGroupKeyMessage message) {
    String json = gson.toJson(message);
    String destination = "/app/call/" + callId + "/e2ee/wrapped-key";
    return send(destination, json)
            .retryWhen((Flowable<Throwable> errors) ->
                    errors.zipWith(
                                    Flowable.range(1, 3),
                                    (Throwable error, Integer attempt) -> {
                                      if (attempt < 3) {
                                        Log.d(TAG, "Retrying sendWrappedGroupKey, attempt " + attempt);
                                        return attempt;
                                      } else {
                                        throw Exceptions.propagate(error);
                                      }
                                    }
                            )
                            .flatMap((Integer attempt) ->
                                    Flowable.timer(attempt, TimeUnit.SECONDS)
                            )
            );
  }

  public Completable sendE2eeReady(String callId, String userId) {
    E2eeReadyMessage message = new E2eeReadyMessage(userId, callId);
    String json = gson.toJson(message);
    String destination = "/app/call/" + callId + "/e2ee/ready";
    return send(destination, json)
            .retryWhen((Flowable<Throwable> errors) ->
                    errors.zipWith(
                                    Flowable.range(1, 3),
                                    (Throwable error, Integer attempt) -> {
                                      if (attempt < 3) {
                                        Log.d(TAG, "Retrying sendE2eeReady, attempt " + attempt);
                                        return attempt;
                                      } else {
                                        throw Exceptions.propagate(error);
                                      }
                                    }
                            )
                            .flatMap((Integer attempt) ->
                                    Flowable.timer(attempt, TimeUnit.SECONDS)
                            )
            );
  }

  public Disposable subscribeToDhKeys(String callId, Consumer<DhPublicKeyMessage> onKeyReceived, Consumer<Throwable> onError) {
    if (stompClient == null || !isConnected) {
      Log.w(TAG, "Cannot subscribe to DH keys: not connected");
      return Disposables.disposed();
    }

    String topic = "/topic/call/" + callId + "/e2ee/dh-key";
    return stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    msg -> {
                      try {
                        DhPublicKeyMessage keyMsg = gson.fromJson(msg.getPayload(), DhPublicKeyMessage.class);
                        Log.d(TAG, "Received DH public key from: " + keyMsg.getSenderId());
                        onKeyReceived.accept(keyMsg);
                      } catch (Exception e) {
                        Log.e(TAG, "Error parsing DH key message", e);
                        onError.accept(e);
                      }
                    },
                    onError
            );
  }

  public Disposable subscribeToWrappedKeys(Consumer<WrappedGroupKeyMessage> onKeyReceived, Consumer<Throwable> onError) {
    if (stompClient == null || !isConnected) {
      Log.w(TAG, "Cannot subscribe to wrapped keys: not connected");
      return Disposables.disposed();
    }

    String topic = "/user/queue/e2ee/wrapped-key";
    return stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    msg -> {
                      try {
                        WrappedGroupKeyMessage keyMsg = gson.fromJson(msg.getPayload(), WrappedGroupKeyMessage.class);
                        Log.d(TAG, "Received wrapped group key from: " + keyMsg.getInviterId());
                        onKeyReceived.accept(keyMsg);
                      } catch (Exception e) {
                        Log.e(TAG, "Error parsing wrapped key message", e);
                        onError.accept(e);
                      }
                    },
                    onError
            );
  }

  public Disposable subscribeToE2eeReady(String callId, Consumer<E2eeReadyMessage> onReady, Consumer<Throwable> onError) {
    if (stompClient == null || !isConnected) return Disposables.disposed();

    String topic = "/topic/call/" + callId + "/e2ee/ready";
    return stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    msg -> {
                      try {
                        E2eeReadyMessage readyMsg = gson.fromJson(msg.getPayload(), E2eeReadyMessage.class);
                        Log.d(TAG, "Received E2EE ready signal from: " + readyMsg.getUserId());
                        onReady.accept(readyMsg);
                      } catch (Exception e) {
                        onError.accept(e);
                      }
                    },
                    onError
            );
  }
}