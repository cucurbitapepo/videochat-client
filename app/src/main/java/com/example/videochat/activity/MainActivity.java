package com.example.videochat.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.videochat.R;
import com.example.videochat.dialog.IncomingCallDialog;
import com.example.videochat.dto.NotificationDto;
import com.example.videochat.fragment.CallsFragment;
import com.example.videochat.fragment.ContactsFragment;
import com.example.videochat.fragment.ProfileFragment;
import com.example.videochat.fragment.SettingsFragment;
import com.example.videochat.util.CrashHandler;
import com.example.videochat.websocket.WebSocketManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

  private BottomNavigationView bottomNav;
  private WebSocketManager webSocketManager;
  private CompositeDisposable disposables = new CompositeDisposable();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

    setContentView(R.layout.activity_main);

    webSocketManager = WebSocketManager.getInstance(this);

    connectWebSocket();

    subscribeToNotifications();

    bottomNav = findViewById(R.id.bottom_navigation);

    loadFragment(new CallsFragment());

    bottomNav.setOnItemSelectedListener(item -> {
      Fragment fragment = null;

      if (item.getItemId() == R.id.nav_calls) {
        fragment = new CallsFragment();
      } else if (item.getItemId() == R.id.nav_contacts) {
        fragment = new ContactsFragment();
      } else if (item.getItemId() == R.id.nav_settings) {
        fragment = new SettingsFragment();
      } else if (item.getItemId() == R.id.nav_profile) {
        fragment = new ProfileFragment();
      }

      if (fragment != null) {
        loadFragment(fragment);
        return true;
      }
      return false;
    });
  }

  private void connectWebSocket() {
    String token = getSharedPreferences("app_data", MODE_PRIVATE)
            .getString("auth_token", null);

    if (token != null && !webSocketManager.isConnected()) {
      webSocketManager.connect();
    }
  }

  private void subscribeToNotifications() {
    webSocketManager.getNotificationEvent().observe(this, notification -> {
      if (notification != null) {
        if ("CALL_REQUEST".equals(notification.getType())) {
          showIncomingCallDialog(notification);
        } else if ("CALL_STATUS".equals(notification.getType())) {
          handleCallStatus(notification);
        }

      }
    });
  }

  private void showIncomingCallDialog(NotificationDto notification) {
    Intent intent = new Intent(this, IncomingCallActivity.class);
    intent.putExtra(IncomingCallActivity.EXTRA_CALL_ID, notification.getData());
    intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, notification.getCallerId());
    intent.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, notification.getCallerName());
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    Log.d("MAIN", "Starting IncomingCallActivity: callId=" + notification.getData() +
                  ", callerId=" + notification.getCallerId());

    startActivity(intent);
  }

  private void handleCallStatus(NotificationDto notification) {
    String status = notification.getMessage();
    String callId = notification.getData();

    if ("accepted".equals(status)) {
    } else if ("rejected".equals(status)) {
      Toast.makeText(this, "Звонок отклонен", Toast.LENGTH_SHORT).show();
    } else if ("ended".equals(status)) {
      if (isCallActivityVisible()) {
        finishCallActivity();
      }
    }
  }

  private boolean isCallActivityVisible() {
    return false;
  }

  private void finishCallActivity() {
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!webSocketManager.isConnected()) {
      connectWebSocket();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    disposables.clear();
    webSocketManager.disconnect();
  }

  private void loadFragment(Fragment fragment) {
    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
  }
}
