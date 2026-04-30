package com.example.videochat.api;

import com.example.videochat.dto.CallDto;
import com.example.videochat.dto.RoomTokenResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CallApi {
  @POST("calls")
  Call<CallDto> createCall(@Body CallDto callRequest);

  @POST("calls/{callId}/accept")
  Call<CallDto> acceptCall(@Path("callId") String callId);

  @POST("calls/{callId}/reject")
  Call<CallDto> rejectCall(@Path("callId") String callId);

  @POST("calls/{callId}/end")
  Call<CallDto> endCall(@Path("callId") String callId);

  @GET("calls/active")
  Call<List<CallDto>> getActiveCalls();

  @GET("calls/{callId}/token")
  Call<RoomTokenResponse> getCallToken(@Path("callId") String callId);

}