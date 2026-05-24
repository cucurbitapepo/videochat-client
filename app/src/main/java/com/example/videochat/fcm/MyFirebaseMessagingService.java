package com.example.videochat.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.videochat.R;
import com.example.videochat.activity.IncomingCallActivity;
import com.example.videochat.activity.MainActivity;
import com.example.videochat.api.ApiClient;
import com.example.videochat.dto.FcmTokenDto;
import com.example.videochat.dto.NotificationDto;
import com.example.videochat.receiver.IncomingCallReceiver;
import com.example.videochat.util.NotificationUtils;
import com.example.videochat.websocket.WebSocketManager;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

  private static final String TAG = "FCMService";
  private static final String CHANNEL_ID = "video_call_channel";
//  private static final int NOTIFICATION_BASE_ID = 2000;
  private static final String INCOMING_CALL_ACTION = "INCOMING_CALL";

  @Override
  public void onNewToken(String token) {
    Log.d(TAG, "Refreshed token: " + token);
    sendRegistrationToServer(token);
  }

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Log.d(TAG, "From: " + remoteMessage.getFrom());

    if (remoteMessage.getData().size() > 0) {
      Log.d(TAG, "Message data payload: " + remoteMessage.getData());
      handleDataMessage(remoteMessage.getData());
    }
  }

  private void handleDataMessage(Map<String, String> data) {
    String type = data.get("type");
    String callId = data.get("callId");

    if ("CALL_REQUEST".equals(type) && callId != null) {
      String callerName = data.get("callerName");
      String initiatorDhPublicKey = data.get("initiatorDhPublicKey");
      String title = data.get("title");
      String body = data.get("body");

      if (WebSocketManager.getInstance(this).isConnected()) {
        NotificationDto notification = new NotificationDto(
                type, body, callId, System.currentTimeMillis()
        );
        notification.setCallerName(callerName);
        notification.setInitiatorDhPublicKey(initiatorDhPublicKey);
        WebSocketManager.getInstance(this).getNotificationEvent().postValue(notification);
      } else {
        showIncomingCallNotification(callId, callerName, initiatorDhPublicKey, title, body);
      }
    } else if ("CALL_STATUS".equals(type) && callId != null) {
      String status = data.get("body");
      if ("cancelled".equals(status)) {
        dismissIncomingCallUi(callId);
      }
    }
  }

  private void dismissIncomingCallUi(String callId) {
    int notifId = NotificationUtils.getNotificationId(callId);
    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    nm.cancel(notifId);
    Log.d(TAG, "Notification cancelled: ID=" + notifId);
  }

  private void sendRegistrationToServer(String token) {
    String authToken = getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .getString("auth_token", null);

    if (authToken == null) {
      Log.d(TAG, "User not authenticated, skipping FCM token registration");
      return;
    }

    ApiClient.getAuthService().sendFcmToken(new FcmTokenDto(token))
            .enqueue(new Callback<Void>() {
              @Override
              public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                  Log.d(TAG, "FCM token sent to server");
                } else {
                  Log.e(TAG, "Failed to send FCM token: " + response.code());
                }
              }

              @Override
              public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to send FCM token", t);
              }
            });
  }

  private int getPendingIntentFlags() {
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return flags;
  }

  private void showIncomingCallNotification(String callId, String callerName,
                                            String initiatorDhPublicKey,
                                            String title, String body) {
    if (callId == null) return;

    int notifId = NotificationUtils.getNotificationId(callId);
    int pendingFlags = getPendingIntentFlags();

    Intent acceptIntent = new Intent(this, IncomingCallReceiver.class);
    acceptIntent.setAction("ACCEPT_CALL");
    acceptIntent.putExtra("callId", callId);
    PendingIntent acceptPI = PendingIntent.getBroadcast(this, notifId + 1, acceptIntent, pendingFlags);

    Intent declineIntent = new Intent(this, IncomingCallReceiver.class);
    declineIntent.setAction("DECLINE_CALL");
    declineIntent.putExtra("callId", callId);
    PendingIntent declinePI = PendingIntent.getBroadcast(this, notifId + 2, declineIntent, pendingFlags);

    Intent incomingCallIntent = new Intent(this, IncomingCallActivity.class);
    incomingCallIntent.putExtra("callId", callId);
    incomingCallIntent.putExtra("callerName", callerName);
    if (initiatorDhPublicKey != null) {
      incomingCallIntent.putExtra("initiatorDhPublicKey", initiatorDhPublicKey);
    }

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addNextIntentWithParentStack(incomingCallIntent);

    PendingIntent incomingCallPendingIntent = stackBuilder.getPendingIntent(
            notifId + 3,
            PendingIntent.FLAG_UPDATE_CURRENT | pendingFlags
    );

    createNotificationChannel();

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title != null ? title : "Входящий звонок")
            .setContentText(body != null ? body : "от собеседника")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(R.drawable.ic_call, "Принять", acceptPI)
            .addAction(R.drawable.ic_call_end, "Отклонить", declinePI)
            .setContentIntent(incomingCallPendingIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setFullScreenIntent(incomingCallPendingIntent, true);
    }

    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(notifId, builder.build());
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = getSystemService(NotificationManager.class);
      NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);

      if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_HIGH) {
        nm.deleteNotificationChannel(CHANNEL_ID);
        channel = null;
      }

      if (channel == null) {
        channel = new NotificationChannel(CHANNEL_ID, "Video Call Channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Incoming video calls");
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build());
        channel.enableVibration(true);
        channel.setBypassDnd(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
      }
    }
  }
}