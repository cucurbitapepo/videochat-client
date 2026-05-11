package com.example.videochat.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.videochat.R;
import com.example.videochat.activity.CallActivity;
import com.example.videochat.api.ApiClient;
import com.example.videochat.dto.CallDto;
import com.example.videochat.dto.ContactDto;
import com.example.videochat.dto.ContactRequestDto;
import com.example.videochat.dto.UserSearchResultDto;
import com.example.videochat.encryption.E2eeKeyManager;
import com.example.videochat.service.CallService;
import com.example.videochat.util.JwtUtils;
import com.example.videochat.websocket.StompClientManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Collections;

import io.reactivex.disposables.CompositeDisposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileDialog extends BottomSheetDialogFragment {

  private UserSearchResultDto user;
  private TextView usernameView;
  private ImageView avatarView;
  private View onlineIndicator;
  private Button actionButton;
  private Button callButton;

  private StompClientManager stompClientManager;
  private CompositeDisposable disposables;

  public static UserProfileDialog newInstance(UserSearchResultDto user) {
    UserProfileDialog dialog = new UserProfileDialog();
    Bundle args = new Bundle();
    args.putSerializable("user", user);
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      user = (UserSearchResultDto) getArguments().getSerializable("user");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_user_profile, container, false);

    usernameView = view.findViewById(R.id.username);
    avatarView = view.findViewById(R.id.avatar);
    onlineIndicator = view.findViewById(R.id.online_indicator);
    actionButton = view.findViewById(R.id.action_button);
    callButton = view.findViewById(R.id.call_button);
    ImageButton closeButton = view.findViewById(R.id.close_button);

    usernameView.setText(user.getUsername());
    onlineIndicator.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

    avatarView.setImageResource(user.isOnline() ?
//            R.drawable.ic_avatar_online : R.drawable.ic_avatar_offline);
            R.drawable.ic_avatar_default : R.drawable.ic_avatar_default);

    updateActionButton();

    setupCallButton();

    actionButton.setOnClickListener(v -> toggleContact());
    closeButton.setOnClickListener(v -> dismiss());

    return view;
  }

  private void setupCallButton() {
    callButton.setOnClickListener(v -> initiateCall());
  }

  private void initiateCall() {
    String currentUserUsername = JwtUtils.getCurrentUserUsername();
    String callId = "call-" + System.currentTimeMillis();

    CallDto callRequest = new CallDto();
    callRequest.setParticipants(Collections.singleton(user.getId()));
    callRequest.setCallId(callId);

    if (currentUserUsername != null) {
      try {
        String initiatorPublicKey = E2eeKeyManager.generateInitiatorPublicKeyForInvite(
                callId,
                currentUserUsername
        );
        callRequest.setInitiatorDhPublicKey(initiatorPublicKey);
        Log.d("CALL", "Added initiator DH public key to CallDto: " +
                      (initiatorPublicKey != null ? initiatorPublicKey : "null"));
      } catch (Exception e) {
        Log.e("CALL", "Failed to generate DH key pair for invite", e);
      }
    } else {
      Log.w("CALL", "Current user username not found, cannot generate E2EE key");
    }

    CallService callService = new CallService();
    callService.createCall(callRequest, new CallService.CallCallback() {
      @Override
      public void onResult(boolean success, CallDto result) {
        if (success && result != null) {
          Log.d("CALL", "Call created: callId=" + result.getCallId());

          if(!callId.equals(result.getCallId())) {
            Log.w("CALL", "Server returned different callId: expected= " + callId + ", got=" + result.getCallId());
          }
          Intent intent = new Intent(requireContext(), CallActivity.class);
          intent.putExtra("CALL_ID", result.getCallId());
          intent.putExtra("IS_CALLER", true);
          intent.putExtra("RECEIVER_ID", user.getId());
          intent.putExtra(IncomingCallDialog.ARG_CALLER_NAME, user.getUsername());

          Log.d("CALL", "Starting CallActivity");
          requireContext().startActivity(intent);
          dismiss();
        } else {
          Log.e("CALL", "Failed to create call: success=" + success + ", result=" + result);
          Toast.makeText(requireContext(), "Не удалось начать звонок", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  private void updateActionButton() {
    if (user.isContact()) {
      actionButton.setText("Удалить из контактов");
      actionButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red));
    } else {
      actionButton.setText("Добавить в контакты");
      actionButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
    }
  }

  private void toggleContact() {
    if (user.isContact()) {
      removeContact();
    } else {
      addContact();
    }
  }

  private void addContact() {
    ContactRequestDto request = new ContactRequestDto();
    request.setUserId(user.getId());

    ApiClient.getContactsApi().addContact(request).enqueue(new Callback<ContactDto>() {
      @Override
      public void onResponse(Call<ContactDto> call, Response<ContactDto> response) {
        if (response.isSuccessful()) {
          user.setIsContact(true);
          updateActionButton();
          Toast.makeText(requireContext(), "Пользователь добавлен в контакты", Toast.LENGTH_SHORT).show();
        } else {
          showError("Не удалось добавить контакт");
        }
      }

      @Override
      public void onFailure(Call<ContactDto> call, Throwable t) {
        showError("Ошибка сети: " + t.getMessage());
      }
    });
  }

  private void removeContact() {
    ApiClient.getContactsApi().removeContact(user.getId()).enqueue(new Callback<Void>() {
      @Override
      public void onResponse(Call<Void> call, Response<Void> response) {
        if (response.isSuccessful()) {
          user.setIsContact(false);
          updateActionButton();
          Toast.makeText(requireContext(), "Пользователь удален из контактов", Toast.LENGTH_SHORT).show();
        } else {
          showError("Не удалось удалить контакт");
        }
      }

      @Override
      public void onFailure(Call<Void> call, Throwable t) {
        showError("Ошибка сети: " + t.getMessage());
      }
    });
  }

  private void showError(String message) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onStart() {
    super.onStart();
    View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setPeekHeight(0);
      behavior.setSkipCollapsed(true);
    }
  }
}
