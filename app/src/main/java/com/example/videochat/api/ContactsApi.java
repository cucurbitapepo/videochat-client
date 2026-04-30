package com.example.videochat.api;

import com.example.videochat.dto.ContactDto;
import com.example.videochat.dto.ContactRequestDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ContactsApi {

  @GET("contacts")
  Call<List<ContactDto>> getContacts();

  @POST("contacts")
  Call<ContactDto> addContact(@Body ContactRequestDto request);

  @DELETE("contacts/{contactUserId}")
  Call<Void> removeContact(@Path("contactUserId") Long contactUserId);
}
