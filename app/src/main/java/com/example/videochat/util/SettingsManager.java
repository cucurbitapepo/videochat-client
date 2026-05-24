package com.example.videochat.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

  private static final String PREF_NAME = "app_settings";
  private static final String KEY_MUTE_MIC_ON_JOIN = "mute_mic_on_join";
  private static final String KEY_MUTE_CAM_ON_JOIN = "mute_cam_on_join";

  private final SharedPreferences prefs;

  private static SettingsManager instance;

  private SettingsManager(Context context) {
    prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
  }

  public static synchronized SettingsManager getInstance(Context context) {
    if (instance == null) {
      instance = new SettingsManager(context.getApplicationContext());
    }
    return instance;
  }

  public boolean shouldMuteMicOnJoin() {
    return prefs.getBoolean(KEY_MUTE_MIC_ON_JOIN, false);
  }

  public void setMuteMicOnJoin(boolean value) {
    prefs.edit().putBoolean(KEY_MUTE_MIC_ON_JOIN, value).apply();
  }

  public boolean shouldMuteCamOnJoin() {
    return prefs.getBoolean(KEY_MUTE_CAM_ON_JOIN, false);
  }

  public void setMuteCamOnJoin(boolean value) {
    prefs.edit().putBoolean(KEY_MUTE_CAM_ON_JOIN, value).apply();
  }
}
