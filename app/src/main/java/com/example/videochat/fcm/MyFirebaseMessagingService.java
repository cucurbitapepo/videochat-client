package com.example.videochat.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.example.videochat.websocket.WebSocketManager;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

  private static final String TAG = "FCMService";
  private static final String CHANNEL_ID = "video_call_channel";
  private static final int NOTIFICATION_ID = 1;
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

    if (remoteMessage.getNotification() != null) {
      Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
      sendNotification(remoteMessage.getNotification().getBody());
    }
  }

  private void handleDataMessage(Map<String, String> data) {
    String type = data.get("type");
    String callId = data.get("callId");

    if ("CALL_REQUEST".equals(type) && callId != null) {
      if (WebSocketManager.getInstance(this).isConnected()) {
        NotificationDto notification = new NotificationDto(
                type,
                "Входящий звонок",
                callId,
                System.currentTimeMillis()
        );
        WebSocketManager.getInstance(this).getNotificationEvent().postValue(notification);
      } else {
        showIncomingCallNotification(callId);
      }
    } else if ("CALL_STATUS".equals(type) && callId != null) {
      String status = data.get("message");

      if (WebSocketManager.getInstance(this).isConnected()) {
        NotificationDto notification = new NotificationDto(
                type,
                status,
                callId,
                System.currentTimeMillis()
        );
        WebSocketManager.getInstance(this).getNotificationEvent().postValue(notification);
      }
    }
  }

  private void sendRegistrationToServer(String token) {
    ApiClient.getAuthService().sendFcmToken(new FcmTokenDto(token))
            .enqueue(new Callback<Void>() {
              @Override
              public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "FCM token sent to server");
              }

              @Override
              public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to send FCM token", t);
              }
            });
  }

//  private void showCallNotification(String callId) {
//    Intent intent = new Intent(this, CallActivity.class);
//    intent.putExtra("CALL_ID", callId);
//    intent.putExtra("IS_CALLER", false);
//    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//    PendingIntent pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            intent,
//            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
//    );
//
//    createNotificationChannel();
//
//    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("Входящий звонок")
//            .setContentText("Нажмите, чтобы ответить")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setFullScreenIntent(pendingIntent, true)
//            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//      builder.setFullScreenIntent(pendingIntent, true);
//    }
//
//    NotificationManager notificationManager =
//            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//    notificationManager.notify(NOTIFICATION_ID, builder.build());
//  }

  private void showIncomingCallNotification(String callId) {
    Intent acceptIntent = new Intent(this, IncomingCallReceiver.class);
    acceptIntent.setAction("ACCEPT_CALL");
    acceptIntent.putExtra("callId", callId);
    PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            acceptIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );

    Intent declineIntent = new Intent(this, IncomingCallReceiver.class);
    declineIntent.setAction("DECLINE_CALL");
    declineIntent.putExtra("callId", callId);
    PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            declineIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );

    createNotificationChannel();

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Входящий звонок")
            .setContentText("Нажмите, чтобы ответить")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_call, "Принять", acceptPendingIntent)
            .addAction(R.drawable.ic_call_end, "Отклонить", declinePendingIntent)
            .setAutoCancel(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
      fullScreenIntent.putExtra("callId", callId);
      fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
              this,
              0,
              fullScreenIntent,
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
      );

      builder.setFullScreenIntent(fullScreenPendingIntent, true);
    }

    NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(NOTIFICATION_ID, builder.build());
  }
  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = "Video Call Channel";
      String description = "Channel for video call notifications";
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setDescription(description);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      channel.setBypassDnd(true);

      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }

  private void sendNotification(String messageBody) {
    Intent intent = new Intent(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

    String channelId = "default_channel";
    NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Video Chat")
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(
              channelId,
              "Default Channel",
              NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
    }

    notificationManager.notify(0, notificationBuilder.build());
  }
}