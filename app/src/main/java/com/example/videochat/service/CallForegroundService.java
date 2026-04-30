package com.example.videochat.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.videochat.R;
import com.example.videochat.activity.CallActivity;

public class CallForegroundService extends Service {
  public static final String ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND";
  public static final String ACTION_STOP_FOREGROUND = "ACTION_STOP_FOREGROUND";

  private static final int NOTIFICATION_ID = 1;

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {
      String action = intent.getAction();
      if (ACTION_START_FOREGROUND.equals(action)) {
        startForegroundService();
        return START_STICKY;
      } else if (ACTION_STOP_FOREGROUND.equals(action)) {
        stopForegroundService();
        return START_NOT_STICKY;
      }
    }
    return START_NOT_STICKY;
  }

  private void startForegroundService() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(
              "call_foreground_channel",
              "Call Foreground Service",
              NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("Keeps the call connection alive");
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }

    Intent notificationIntent = new Intent(this, CallActivity.class);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );

    Notification notification = new NotificationCompat.Builder(this, "call_foreground_channel")
            .setContentTitle("Active call")
            .setContentText("You are in a video call")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification,
              FOREGROUND_SERVICE_TYPE_MICROPHONE | FOREGROUND_SERVICE_TYPE_CAMERA);
    } else {
      startForeground(NOTIFICATION_ID, notification);
    }
  }

  private void stopForegroundService() {
    stopForeground(true);
    stopSelf();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
