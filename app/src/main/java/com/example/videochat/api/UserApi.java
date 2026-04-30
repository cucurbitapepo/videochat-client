package com.example.videochat.api;

import com.example.videochat.dto.PageResponse;
import com.example.videochat.dto.UserSearchResultDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserApi {
  @GET("users/search")
  Call<PageResponse<UserSearchResultDto>> searchUsers(@Query("q") String query);
}
