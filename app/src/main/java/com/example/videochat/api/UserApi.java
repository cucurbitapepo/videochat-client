package com.example.videochat.api;

import com.example.videochat.dto.PageResponse;
import com.example.videochat.dto.UserSearchResultDto;

import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface UserApi {
  @GET("users/search")
  Call<PageResponse<UserSearchResultDto>> searchUsers(@Query("q") String query);

  @POST("users/names")
  Call<Map<Long, String>> getUsernamesByIds(@Body Set<Long> userIds);
}
