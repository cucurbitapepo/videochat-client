package com.example.videochat.dto;

import java.io.Serializable;

public class NotificationDto implements Serializable {
  private String type;
  private String message;
  private String data;
  private long timestamp;
  private Long callerId;
  private String callerName;
  private String initiatorDhPublicKey;

  public NotificationDto() {}

  public NotificationDto(String type, String message, String data, long timestamp, String initiatorDhPublicKey) {
    this.type = type;
    this.message = message;
    this.data = data;
    this.timestamp = timestamp;
    this.initiatorDhPublicKey = initiatorDhPublicKey;
  }

  public NotificationDto(String type, String message, String data, long timestamp) {
    this(type, message, data, timestamp, null);
  }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public String getData() { return data; }
  public void setData(String data) { this.data = data; }

  public long getTimestamp() { return timestamp; }
  public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

  public Long getCallerId() {
    return callerId;
  }

  public void setCallerId(Long callerId) {
    this.callerId = callerId;
  }

  public String getCallerName() {
    return callerName;
  }

  public void setCallerName(String callerName) {
    this.callerName = callerName;
  }
  public String getInitiatorDhPublicKey() {
    return initiatorDhPublicKey;
  }

  public void setInitiatorDhPublicKey(String initiatorDhPublicKey) {
    this.initiatorDhPublicKey = initiatorDhPublicKey;
  }
}