package com.example.videochat.api;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
  private final SharedPreferences prefs;

  public AuthInterceptor(SharedPreferences prefs) {
    this.prefs = prefs;
  }


  @NonNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();

    String token = prefs.getString("auth_token", null);

    Request.Builder builder = originalRequest.newBuilder();
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }

    return chain.proceed(builder.build());
  }
}