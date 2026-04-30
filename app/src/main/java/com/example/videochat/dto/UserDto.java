package com.example.videochat.dto;

import java.io.Serializable;

public class UserDto implements Serializable {
  private Long id;
  private String username;
  private boolean online;
  private String createdAt;

  public UserDto() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public boolean isOnline() { return online; }
  public void setOnline(boolean online) { this.online = online; }

  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
