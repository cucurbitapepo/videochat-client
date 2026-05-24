package com.example.videochat.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videochat.R;
import com.example.videochat.api.ApiClient;
import com.example.videochat.api.AuthService;
import com.example.videochat.dto.AuthRequest;
import com.example.videochat.dto.AuthResponse;
import com.example.videochat.dto.FcmTokenDto;
import com.example.videochat.util.JwtUtils;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;

import java.time.LocalDateTime;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

  private EditText usernameInput, passwordInput;
  private Button loginButton, registerButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    initViews();
    setupClickListeners();
  }

  private void initViews() {
    usernameInput = findViewById(R.id.usernameInput);
    passwordInput = findViewById(R.id.passwordInput);
    loginButton = findViewById(R.id.loginButton);
    registerButton = findViewById(R.id.registerButton);
  }

  private void setupClickListeners() {
    loginButton.setOnClickListener(v -> loginUser());
    registerButton.setOnClickListener(v -> registerUser());
  }

  private void registerUser() {
    String username = usernameInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();
    AuthRequest request = new AuthRequest(username, password);
    AuthService authService = ApiClient.getAuthService();

    authService.register(request).enqueue(new Callback<AuthResponse>() {
      @Override
      public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String token = response.body().getAccessToken();
          saveToken(token);
          FirebaseMessaging.getInstance().getToken()
                  .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                      sendFcmTokenToServer(task.getResult(), () -> proceedToMainActivity());
                    } else {
                      proceedToMainActivity();
                    }
                  });
        } else {
          showError("Ошибка регистрации");
        }
      }

      @Override
      public void onFailure(Call<AuthResponse> call, Throwable t) {
        showError("Ошибка сети: " + t.getMessage());
      }
    });
  }

  private void loginUser() {
    String username = usernameInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();

    if (username.isEmpty() || password.isEmpty()) {
      showError("Заполните все поля");
      return;
    }

    AuthRequest request = new AuthRequest(username, password);
    AuthService authService = ApiClient.getAuthService();

    authService.login(request).enqueue(new Callback<>() {
      @Override
      public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          saveToken(response.body().getAccessToken());
          FirebaseMessaging.getInstance().getToken()
                  .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                      String fcmToken = task.getResult();
                      Log.d("LOGIN", "FCM Token: " + fcmToken);
                      sendFcmTokenToServer(fcmToken, () -> {
                        Log.d("LOGIN", "FCM token sent, proceeding to MainActivity");
                        proceedToMainActivity();
                      });
                    } else {
                      Log.w("LOGIN", "Failed to get FCM token", task.getException());
                      proceedToMainActivity();
                    }
                  });
        } else {
          showError("Ошибка входа. Проверьте логин и пароль.");
        }
      }

      @Override
      public void onFailure(Call<AuthResponse> call, Throwable t) {
        showError("Ошибка сети: " + t.getMessage());
      }
    });
  }

  private void proceedToMainActivity() {
    startActivity(new Intent(LoginActivity.this, MainActivity.class));
    finish();
  }

  private void saveToken(String token) {
    //TODO: Android KeyStore для шифрования токена
    SharedPreferences prefs = getSharedPreferences("app_data", MODE_PRIVATE);
    prefs.edit().putString("auth_token", token).apply();

    String currentUserUsername;
    try {
      currentUserUsername = JwtUtils.getUsernameFromToken(token);
    } catch (JSONException e) {
      Log.w("LoginActivity", "Не удалось получить имя пользователя из токена аутентификации.");
      currentUserUsername = "Ошибка";
    }
    prefs.edit().putString("current_user", currentUserUsername).apply();
  }

  private void sendFcmTokenToServer(String token, Runnable onSuccess) {
    String authToken = getSharedPreferences("app_data", MODE_PRIVATE)
            .getString("auth_token", null);

    if (authToken == null) return;

    FcmTokenDto request = new FcmTokenDto();
    request.setToken(token);
    request.setExpiry(LocalDateTime.now().plusDays(30));

    ApiClient.getAuthService().sendFcmToken(request)
            .enqueue(new Callback<Void>() {
              @Override
              public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                  Log.d("FCM", "Token sent successfully");
                  if (onSuccess != null) onSuccess.run();
                } else {
                  Log.e("FCM", "Failed to send token: " + response.code());
                  if (onSuccess != null) onSuccess.run();
                }
              }

              @Override
              public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FCM", "Network error: " + t.getMessage());
                if (onSuccess != null) onSuccess.run();
              }
            });
  }

  private void showError(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }
}
