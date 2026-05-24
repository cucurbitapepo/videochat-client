package com.example.videochat.util;

public class NotificationUtils {
  private static final int NOTIFICATION_BASE_ID = 2000;

  public static int getNotificationId(String callId) {
    if (callId == null) return NOTIFICATION_BASE_ID;
    return Math.abs(callId.hashCode()) % 10000 + NOTIFICATION_BASE_ID;
  }
}