package com.example.videochat;

import android.app.Application;
import android.content.Context;

import com.google.firebase.FirebaseApp;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class App extends Application {
  private static App instance;

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;

    FirebaseApp.initializeApp(this);
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
  }

  public static Context getAppContext() {
    return instance.getApplicationContext();
  }
}