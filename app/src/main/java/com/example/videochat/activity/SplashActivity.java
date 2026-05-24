package com.example.videochat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videochat.util.PermissionHelper;

public class SplashActivity extends AppCompatActivity {
  private ActivityResultLauncher<String[]> permissionLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
              boolean allGranted = true;
              for (Boolean granted : result.values()) {
                if (!granted) {
                  allGranted = false;
                  break;
                }
              }

              if (allGranted) {
                checkAuth();
              }
            }
    );

    if (handleIncomingCallIntent(getIntent())) return;

    checkAndRequestPermissions();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (handleIncomingCallIntent(intent)) finish();
  }

  private boolean handleIncomingCallIntent(Intent intent) {
    if (intent != null && intent.hasExtra("callId")) {
      Log.d("SPLASH", "Routing incoming call intent directly");
      Intent callIntent = new Intent(this, IncomingCallActivity.class);
      callIntent.putExtras(intent);
      callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(callIntent);
      return true;
    }
    return false;
  }

  private void checkAndRequestPermissions() {
    if (PermissionHelper.hasAllPermissions(this)) {
      checkAuth();
    } else {
      permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS);
    }
  }

  private void checkAuth() {
    String token = getSharedPreferences("app_data", MODE_PRIVATE)
            .getString("auth_token", null);

    Intent intent = new Intent(this, token != null ? MainActivity.class : LoginActivity.class);
    startActivity(intent);
    finish();
  }
}
