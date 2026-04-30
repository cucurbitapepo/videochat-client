package com.example.videochat.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.videochat.App;
import com.example.videochat.util.GsonProvider;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

  public static final String BASE_URL = "https://84.54.59.153:8443/api/v1/";
  private static Retrofit retrofit = null;
  private static AuthService authService;
  private static UserApi userApi;
  private static ContactsApi contactsApi;
  private static CallApi callApi;
  private static LiveKitApi liveKitApi;

  private static SharedPreferences getPrefs() {
    return App.getAppContext().getSharedPreferences("app_data", Context.MODE_PRIVATE);
  }

  public static Retrofit getClient() {
    if (retrofit == null) {
      SharedPreferences prefs = getPrefs();

      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(HttpLoggingInterceptor.Level.BODY);

      OkHttpClient client = new OkHttpClient.Builder()
              .addInterceptor(new AuthInterceptor(prefs))
              .addInterceptor(logging)
              .connectTimeout(30, TimeUnit.SECONDS)
              .readTimeout(30, TimeUnit.SECONDS)
              .build();

      retrofit = new Retrofit.Builder()
              .baseUrl(BASE_URL)
              .addConverterFactory(GsonConverterFactory.create(GsonProvider.createGson()))
              .client(client)
              .build();
    }
    return retrofit;
  }

  public static AuthService getAuthService() {
    if (authService == null) {
      authService = getClient().create(AuthService.class);
    }
    return authService;
  }

  public static UserApi getUserApi() {
    if (userApi == null) {
      userApi = getClient().create(UserApi.class);
    }
    return userApi;
  }

  public static ContactsApi getContactsApi() {
    if (contactsApi == null) {
      contactsApi = getClient().create(ContactsApi.class);
    }
    return contactsApi;
  }

  public static CallApi getCallApi() {
    if (callApi == null) {
      callApi = getClient().create(CallApi.class);
    }
    return callApi;
  }

  public static LiveKitApi getLiveKitApi() {
    if (liveKitApi == null) {
      liveKitApi = getClient().create(LiveKitApi.class);
    }
    return liveKitApi;
  }
}
