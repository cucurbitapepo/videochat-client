package com.example.videochat.dto;

public class AuthResponse {
  private String accessToken;
  private String tokenType;
  private Long expiresIn;

  public String getAccessToken() { return accessToken; }
  public String getTokenType() { return tokenType; }
  public Long getExpiresIn() { return expiresIn; }
}
