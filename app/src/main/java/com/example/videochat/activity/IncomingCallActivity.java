package com.example.videochat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videochat.dialog.IncomingCallDialog;
import com.example.videochat.dto.NotificationDto;
import com.example.videochat.dto.UserDto;
import com.example.videochat.websocket.WebSocketManager;

public class IncomingCallActivity extends AppCompatActivity {
  public static final String EXTRA_CALL_ID = "callId";
  public static final String EXTRA_CALLER_ID = "callerId";
  public static final String EXTRA_CALLER_NAME = "callerName";
  public static final String EXTRA_INITIATOR_DH_PUBLIC_KEY = "initiatorDhPublicKey";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if ("DISMISS_CALL".equals(getIntent().getAction())) {
      Log.d("INCOMING", "Dismissing due to cancellation");
      finish();
      return;
    }

    getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    );

    String callId = getIntent().getStringExtra(EXTRA_CALL_ID);
    Long callerId = getIntent().getLongExtra(EXTRA_CALLER_ID, -1L);
    String callerName = getIntent().getStringExtra(EXTRA_CALLER_NAME);
    String initiatorDhPublicKey = getIntent().getStringExtra(EXTRA_INITIATOR_DH_PUBLIC_KEY);

    if (callId == null || callerId == -1L || callerName == null) {
      Log.e("INCOMING", "Invalid intent data: callId=" + callId + ", callerId=" + callerId);
      finish();
      return;
    }

    WebSocketManager.getInstance(this).getNotificationEvent().observe(this, notification -> {
      if (notification == null) return;

      if ("CALL_STATUS".equals(notification.getType()) &&
          "cancelled".equals(notification.getMessage()) &&
          callId != null && callId.equals(notification.getData())) {

        Log.d("INCOMING", "Received cancellation via STOMP, dismissing dialog");
        finish();
      }
    });

    NotificationDto notification = new NotificationDto();
    notification.setType("CALL_REQUEST");
    notification.setData(callId);
    notification.setCallerId(callerId);
    notification.setCallerName(callerName);
    notification.setTimestamp(System.currentTimeMillis());
    notification.setInitiatorDhPublicKey(initiatorDhPublicKey);
    showIncomingCallDialog(notification);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    if ("DISMISS_CALL".equals(intent.getAction())) {
      String callId = intent.getStringExtra("callId");
      String currentCallId = getIntent().getStringExtra("callId");

      if (callId != null && callId.equals(currentCallId)) {
        Log.d("INCOMING", "Dismissing active call dialog");
        finish();
      }
    }
  }

  private void showIncomingCallDialog(NotificationDto notification) {
    IncomingCallDialog dialog = IncomingCallDialog.newInstance(notification);
    dialog.show(getSupportFragmentManager(), "incoming_call");
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (getSupportFragmentManager().findFragmentByTag("incoming_call") == null) {
      finish();
    }
  }
}
