package com.example.videochat.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserSearchResultDto implements Serializable {
  private Long id;
  private String username;
  private boolean online;
  @SerializedName("contact")
  private boolean isContact;

  public UserSearchResultDto() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public boolean isOnline() { return online; }
  public void setOnline(boolean online) { this.online = online; }

  public boolean isContact() { return isContact; }
  public void setIsContact(boolean isContact) { this.isContact = isContact; }
}
