package com.example.videochat.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.videochat.activity.CrashActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

  private final Context context;
  private final Thread.UncaughtExceptionHandler defaultHandler;

  public CrashHandler(Context context) {
    this.context = context.getApplicationContext();
    this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    String stackTrace = sw.toString();
    String message = e.getMessage() != null ? e.getMessage() : e.toString();

    boolean isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

    if (isDebuggable) {
      Intent intent = new Intent(context, CrashActivity.class);
      intent.putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTrace);
      intent.putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, message);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      context.startActivity(intent);

      android.os.Process.killProcess(android.os.Process.myPid());
      System.exit(1);
    } else {
      new Handler(Looper.getMainLooper()).post(() -> {
        Toast.makeText(context, "Произошла ошибка. Приложение будет закрыто.", Toast.LENGTH_LONG).show();
      });

      if (defaultHandler != null) {
        defaultHandler.uncaughtException(t, e);
      } else {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
      }
    }
  }
}