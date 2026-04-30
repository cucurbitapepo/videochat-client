package com.example.videochat.api;

import com.example.videochat.dto.AuthRequest;
import com.example.videochat.dto.AuthResponse;
import com.example.videochat.dto.FcmTokenDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {

  @POST("auth/register")
  Call<AuthResponse> register(@Body AuthRequest request);

  @POST("auth/login")
  Call<AuthResponse> login(@Body AuthRequest request);

  @POST("auth/fcm-token")
  Call<Void> sendFcmToken(@Body FcmTokenDto fcmTokenDto);
}
