package com.example.videochat.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.videochat.R;
import com.example.videochat.activity.CallActivity;
import com.example.videochat.dto.NotificationDto;
import com.example.videochat.dto.UserDto;
import com.example.videochat.service.CallService;
import com.example.videochat.websocket.WebSocketManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class IncomingCallDialog extends DialogFragment {

  private static final String ARG_CALL_ID = "callId";
  private static final String ARG_CALLER_ID = "callerId";
  public static final String ARG_CALLER_NAME = "callerName";
  public static final String ARG_INITIATOR_DH_PUBLIC_KEY = "initiatorDhPublicKey";

  private String callId;
  private String callerName;
  private Long callerId;
  private String initiatorDhPublicKey;
  private CallService callService;

  public static IncomingCallDialog newInstance(NotificationDto notification) {

    if(notification.getData() == null || notification.getCallerId() == null || notification.getCallerName() == null) {
      throw new IllegalArgumentException("Received null data in notification");
    }

    Bundle args = new Bundle();
    args.putString(ARG_CALL_ID, notification.getData());
    args.putLong(ARG_CALLER_ID, notification.getCallerId());
    args.putString(ARG_CALLER_NAME, notification.getCallerName());
    if (notification.getInitiatorDhPublicKey() != null) {
      Log.d("IncomingCallDialog", "initiator public key is: " + notification.getInitiatorDhPublicKey());
      args.putString(ARG_INITIATOR_DH_PUBLIC_KEY, notification.getInitiatorDhPublicKey());
    } else {
      Log.d("IncomingCallDialog", "initiator public key is NULL");
    }
    IncomingCallDialog fragment = new IncomingCallDialog();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      callId = getArguments().getString(ARG_CALL_ID);
      callerId = getArguments().getLong(ARG_CALLER_ID);
      callerName = getArguments().getString(ARG_CALLER_NAME);
      initiatorDhPublicKey = getArguments().getString(ARG_INITIATOR_DH_PUBLIC_KEY);
    }

    if (callId == null || callerId == -1L) {
      Log.e("DIALOG", "Invalid arguments: callId=" + callId + ", callerId=" + callerId);
      dismiss();
      return;
    }
    callService = new CallService();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_incoming_call, container, false);

    TextView callerNameView = view.findViewById(R.id.caller_name);
    if (callerNameView != null && callerName != null) {
      callerNameView.setText(callerName);
      Log.d("DIALOG", "Displaying caller name: " + callerName);
    } else {
      Log.w("DIALOG", "callerNameView not found or callerName is null");
    }
    MaterialButton acceptButton = view.findViewById(R.id.accept_button);
    acceptButton.setOnClickListener(v -> acceptCall());

    FloatingActionButton declineButton = view.findViewById(R.id.decline_button);
    declineButton.setOnClickListener(v -> declineCall());

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    Dialog dialog = getDialog();
    if (dialog != null) {
      int width = ViewGroup.LayoutParams.MATCH_PARENT;
      int height = ViewGroup.LayoutParams.MATCH_PARENT;
      dialog.getWindow().setLayout(width, height);
    }
  }

  private void acceptCall() {
    if (callId == null || callerId == -1L) {
      Log.e("DIALOG", "Cannot accept call: invalid arguments");
      Toast.makeText(getContext(), "Ошибка: данные звонка повреждены", Toast.LENGTH_SHORT).show();
      dismiss();
      return;
    }

    Log.d("DIALOG", "Accepting call: callId=" + callId + ", callerId=" + callerId);

    callService.acceptCall(callId, success -> {
      if (success) {
        Log.d("DIALOG", "Call accepted via REST, starting CallActivity");
        dismiss();
        startCallActivity();
      } else {
        Log.e("DIALOG", "Failed to accept call via REST");
        Toast.makeText(getContext(), "Не удалось принять звонок", Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void declineCall() {
    if (callId == null) {
      dismiss();
      return;
    }

    callService.rejectCall(callId, success -> {
      if (success) {
        dismiss();
      }
    });
  }

  private void startCallActivity() {
    if (getContext() == null) return;

    Intent intent = new Intent(getContext(), CallActivity.class);
    intent.putExtra("CALL_ID", callId);
    intent.putExtra("IS_CALLER", false);
    intent.putExtra("RECEIVER_ID", callerId);
    if (callerName != null) {
      intent.putExtra(ARG_CALLER_NAME, callerName);
    }
    if (initiatorDhPublicKey != null) {
      intent.putExtra("INITIATOR_DH_PUBLIC_KEY", initiatorDhPublicKey);
      Log.d("DIALOG", "Passing initiator DH public key to CallActivity");
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Log.d("DIALOG", "Starting CallActivity: callId=" + callId + ", receiverId=" + callerId);
    getContext().startActivity(intent);
  }
}