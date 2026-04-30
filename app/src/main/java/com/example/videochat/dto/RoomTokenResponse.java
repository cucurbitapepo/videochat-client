package com.example.videochat.dto;

public class RoomTokenResponse {
  private String token;
  private String roomName;
  private String serverUrl;

  public String getToken() {
    return token;
  }

  public String getRoomName() {
    return roomName;
  }

  public String getServerUrl() {
    return serverUrl;
  }
}
