package com.example.videochat.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class CallDto {
  private String callId;
  private Long callerId;
  private CallStatus status;
  private Set<Long> participants;
  private LocalDateTime createdAt;
  private LocalDateTime endedAt;

  public String getInitiatorDhPublicKey() {
    return initiatorDhPublicKey;
  }

  public void setInitiatorDhPublicKey(String initiatorDhPublicKey) {
    this.initiatorDhPublicKey = initiatorDhPublicKey;
  }

  private String initiatorDhPublicKey;

  public String getCallId() {
    return callId;
  }

  public void setCallId(String callId) {
    this.callId = callId;
  }

  public Long getCallerId() {
    return callerId;
  }

  public void setCallerId(Long callerId) {
    this.callerId = callerId;
  }

  public CallStatus getStatus() {
    return status;
  }

  public void setStatus(CallStatus status) {
    this.status = status;
  }

  public Set<Long> getParticipants() {
    return participants;
  }

  public void setParticipants(Set<Long> participants) {
    this.participants = participants;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(LocalDateTime endedAt) {
    this.endedAt = endedAt;
  }
}
