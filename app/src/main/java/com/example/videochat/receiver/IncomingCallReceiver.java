package com.example.videochat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.videochat.activity.CallActivity;
import com.example.videochat.service.CallService;

public class IncomingCallReceiver extends BroadcastReceiver {
  private static final String TAG = "IncomingCallReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    String callId = intent.getStringExtra("callId");

    if (callId == null) {
      Log.e(TAG, "Call ID is null");
      return;
    }

    if ("ACCEPT_CALL".equals(action)) {
      handleAcceptCall(context, callId);
    } else if ("DECLINE_CALL".equals(action)) {
      handleDeclineCall(context, callId);
    }
  }

  private void handleAcceptCall(Context context, String callId) {
    CallService callService = new CallService();
    callService.acceptCall(callId, success -> {
      if (success) {
        Intent callIntent = new Intent(context, CallActivity.class);
        callIntent.putExtra("CALL_ID", callId);
        callIntent.putExtra("IS_CALLER", false);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
      }
    });
  }

  private void handleDeclineCall(Context context, String callId) {
    CallService callService = new CallService();
    callService.rejectCall(callId, success -> {
      if (!success) {
        Log.e(TAG, "Failed to reject call: " + callId);
      }
    });
  }
}