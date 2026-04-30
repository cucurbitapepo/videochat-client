package com.example.videochat.dto;

import java.time.LocalDateTime;

public class FcmTokenDto {
  private String token;
  private LocalDateTime expiry;

  public LocalDateTime getExpiry() {
    return expiry;
  }

  public void setExpiry(LocalDateTime expiry) {
    this.expiry = expiry;
  }

  public FcmTokenDto() {}

  public FcmTokenDto(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}