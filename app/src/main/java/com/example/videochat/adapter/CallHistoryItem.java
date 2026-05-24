package com.example.videochat.adapter;

import com.example.videochat.R;
import com.example.videochat.dto.CallStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CallHistoryItem {
  private String callId;
  private String counterpartName;
  private LocalDateTime startTime;
  private Duration duration;
  private CallStatus status;
  private boolean isOutgoing;

  public CallHistoryItem(String callId, String counterpartName, LocalDateTime startTime, Duration duration, CallStatus status, boolean isOutgoing) {
    this.callId = callId;
    this.counterpartName = counterpartName;
    this.startTime = startTime;
    this.duration = duration;
    this.status = status;
    this.isOutgoing = isOutgoing;
  }

  public String getFormattedStartTime() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault());
    return startTime.format(formatter);
  }

  public String getFormattedDuration() {
    if (duration == null || status == CallStatus.WAITING || status == CallStatus.ACTIVE) {
      return "—";
    }
    long seconds = duration.getSeconds();
    if (seconds < 60) return seconds + " сек";
    long minutes = seconds / 60;
    long secs = seconds % 60;
    return String.format("%d:%02d", minutes, secs);
  }

  public int getStatusIconRes() {
    if (isOutgoing) {
      switch (status) {
        case ENDED: return R.drawable.ic_call_outgoing_accepted;
        case REJECTED: return R.drawable.ic_call_outgoing_declined;
        case CANCELLED: return R.drawable.ic_call_missed_outgoing;
        default: return R.drawable.ic_call_outgoing;
      }
    } else {
      switch (status) {
        case ENDED: return R.drawable.ic_call_incoming_accepted;
        case REJECTED: return R.drawable.ic_call_incoming_declined;
        case CANCELLED: return R.drawable.ic_call_missed_incoming;
        default: return R.drawable.ic_call_incoming;
      }
    }
  }

  public String getCallId() {
    return callId;
  }

  public void setCallId(String callId) {
    this.callId = callId;
  }

  public String getCounterpartName() {
    return counterpartName;
  }

  public void setCounterpartName(String counterpartName) {
    this.counterpartName = counterpartName;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public CallStatus getStatus() {
    return status;
  }

  public void setStatus(CallStatus status) {
    this.status = status;
  }

  public boolean isOutgoing() {
    return isOutgoing;
  }

  public void setOutgoing(boolean outgoing) {
    isOutgoing = outgoing;
  }
}