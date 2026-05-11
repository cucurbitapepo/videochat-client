package com.example.videochat.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videochat.dialog.IncomingCallDialog;
import com.example.videochat.dto.NotificationDto;
import com.example.videochat.dto.UserDto;

public class IncomingCallActivity extends AppCompatActivity {
  public static final String EXTRA_CALL_ID = "callId";
  public static final String EXTRA_CALLER_ID = "callerId";
  public static final String EXTRA_CALLER_NAME = "callerName";
  public static final String EXTRA_INITIATOR_DH_PUBLIC_KEY = "initiatorDhPublicKey";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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

    NotificationDto notification = new NotificationDto();
    notification.setType("CALL_REQUEST");
    notification.setData(callId);
    notification.setCallerId(callerId);
    notification.setCallerName(callerName);
    notification.setTimestamp(System.currentTimeMillis());
    notification.setInitiatorDhPublicKey(initiatorDhPublicKey);
    showIncomingCallDialog(notification);
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
