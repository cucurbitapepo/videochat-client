package com.example.videochat.dto;

public class ContactRequestDto {
  private Long userId;

  public ContactRequestDto() {}

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
}
