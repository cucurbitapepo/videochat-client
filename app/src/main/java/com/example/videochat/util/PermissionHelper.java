package com.example.videochat.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionHelper {
  public static final String[] REQUIRED_PERMISSIONS = {
          Manifest.permission.CAMERA,
          Manifest.permission.RECORD_AUDIO
  };

  public static boolean hasAllPermissions(Context context) {
    for (String perm : REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }
}
