package com.example.videochat.dto;

public class AuthRequest {
  private String username;
  private String password;

  public AuthRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
