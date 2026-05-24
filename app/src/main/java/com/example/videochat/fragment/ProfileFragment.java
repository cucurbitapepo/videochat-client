package com.example.videochat.fragment;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.videochat.R;
import com.example.videochat.activity.LoginActivity;
import com.example.videochat.activity.SplashActivity;
import com.example.videochat.util.JwtUtils;
import com.example.videochat.websocket.WebSocketManager;

public class ProfileFragment extends Fragment {

  private static final String TAG = "ProfileFragment";

  private TextView usernameText;
  private Button logoutButton;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_profile, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    initViews(view);
    loadUserProfile();
    setupLogoutButton();
  }

  private void initViews(View view) {
    usernameText = view.findViewById(R.id.username_text);
    logoutButton = view.findViewById(R.id.logout_button);
  }

  private void loadUserProfile() {
    String username = JwtUtils.getCurrentUserUsername();

    if (username != null && !username.isEmpty()) {
      usernameText.setText(username);
      Log.d(TAG, "Loaded username: " + username);
    } else {
      usernameText.setText("Неизвестный пользователь");
      Log.w(TAG, "Username not found");
    }
  }

  private void setupLogoutButton() {
    logoutButton.setOnClickListener(v -> showLogoutConfirmation());
  }

  private void showLogoutConfirmation() {
    if (getContext() == null) return;

    new AlertDialog.Builder(getContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти", (dialog, which) -> performLogout())
            .setNegativeButton("Отмена", null)
            .setCancelable(true)
            .show();
  }

  private void performLogout() {
    if (getContext() != null) {
      SharedPreferences prefs = getContext().getSharedPreferences("app_data", Context.MODE_PRIVATE);
      prefs.edit()
              .remove("auth_token")
              .remove("current_user")
              .apply();
      Log.d(TAG, "Auth tokens cleared");
    }

    WebSocketManager.getInstance(requireContext()).disconnect();

    Intent intent = new Intent(getContext(), LoginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    if (getContext() != null) {
      getContext().startActivity(intent);
    }

    if (getActivity() != null) {
      getActivity().finish();
    }

    Log.d(TAG, "Logout completed");
  }
}