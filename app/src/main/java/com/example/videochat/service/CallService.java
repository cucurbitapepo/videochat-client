package com.example.videochat.service;

import com.example.videochat.api.ApiClient;
import com.example.videochat.dto.CallDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallService {

  public void createCall(CallDto callRequest, CallCallback callback) {
    ApiClient.getCallApi().createCall(callRequest).enqueue(new Callback<CallDto>() {
      @Override
      public void onResponse(Call<CallDto> call, Response<CallDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onResult(true, response.body());
        } else {
          callback.onResult(false, null);
        }
      }

      @Override
      public void onFailure(Call<CallDto> call, Throwable t) {
        callback.onResult(false, null);
      }
    });
  }

  public void acceptCall(String callId, SimpleCallback callback) {
    ApiClient.getCallApi().acceptCall(callId).enqueue(new Callback<CallDto>() {
      @Override
      public void onResponse(Call<CallDto> call, Response<CallDto> response) {
        callback.onResult(response.isSuccessful());
      }

      @Override
      public void onFailure(Call<CallDto> call, Throwable t) {
        callback.onResult(false);
      }
    });
  }

  public void rejectCall(String callId, SimpleCallback callback) {
    ApiClient.getCallApi().rejectCall(callId).enqueue(new Callback<CallDto>() {
      @Override
      public void onResponse(Call<CallDto> call, Response<CallDto> response) {
        callback.onResult(response.isSuccessful());
      }

      @Override
      public void onFailure(Call<CallDto> call, Throwable t) {
        callback.onResult(false);
      }
    });
  }

  public void endCall(String callId, SimpleCallback callback) {
    ApiClient.getCallApi().endCall(callId).enqueue(new Callback<CallDto>() {
      @Override
      public void onResponse(Call<CallDto> call, Response<CallDto> response) {
        callback.onResult(response.isSuccessful());
      }

      @Override
      public void onFailure(Call<CallDto> call, Throwable t) {
        callback.onResult(false);
      }
    });
  }

  public interface CallCallback {
    void onResult(boolean success, CallDto callDto);
  }

  public interface SimpleCallback {
    void onResult(boolean success);
  }
}