package com.example.videochat.websocket;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.videochat.dto.NotificationDto;

public class WebSocketManager {
  private static WebSocketManager instance;
  private final StompClientManager stompClientManager;

  private WebSocketManager(Context context) {
    stompClientManager = new StompClientManager(context.getApplicationContext());
  }

  public static synchronized WebSocketManager getInstance(Context context) {
    if (instance == null) {
      instance = new WebSocketManager(context);
    }
    return instance;
  }

  public StompClientManager getStompClientManager() {
    return this.stompClientManager;
  }

  public void connect() {
    stompClientManager.connect();
  }

  public void disconnect() {
    stompClientManager.disconnect();
  }

  public MutableLiveData<NotificationDto> getNotificationEvent() {
    return stompClientManager.getNotificationEvent();
  }

  public MutableLiveData<Boolean> getConnectionState() {
    return stompClientManager.getConnectionState();
  }

  public boolean isConnected() {
    return stompClientManager.isConnected();
  }

  public void handleCallRequest(NotificationDto notification) {
    stompClientManager.handleCallRequest(notification);
  }
}