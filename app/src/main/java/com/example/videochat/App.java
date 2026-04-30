package com.example.videochat;

import android.app.Application;
import android.content.Context;

import com.google.firebase.FirebaseApp;

public class App extends Application {
  private static App instance;

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;

    FirebaseApp.initializeApp(this);
  }

  public static Context getAppContext() {
    return instance.getApplicationContext();
  }
}