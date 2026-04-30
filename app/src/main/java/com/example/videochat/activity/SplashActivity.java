package com.example.videochat.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    checkAuth();
  }

  private void checkAuth() {
    String token = getSharedPreferences("app_data", MODE_PRIVATE)
            .getString("auth_token", null);

    Intent intent = new Intent(this, token != null ? MainActivity.class : LoginActivity.class);
    startActivity(intent);
    finish();
  }
}
