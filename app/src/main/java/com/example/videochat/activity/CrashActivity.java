package com.example.videochat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.videochat.R;

public class CrashActivity extends AppCompatActivity {

  public static final String EXTRA_STACK_TRACE = "stack_trace";
  public static final String EXTRA_ERROR_MESSAGE = "error_message";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_crash);

    TextView tvMessage = findViewById(R.id.tv_error_message);
    TextView tvStackTrace = findViewById(R.id.tv_stack_trace);
    Button btnRestart = findViewById(R.id.btn_restart);

    String message = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
    String stackTrace = getIntent().getStringExtra(EXTRA_STACK_TRACE);

    tvMessage.setText(message != null ? message : "Unknown Error");
    tvStackTrace.setText(stackTrace != null ? stackTrace : "No stack trace available");

    btnRestart.setOnClickListener(v -> {
      Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
      }
      finish();
      android.os.Process.killProcess(android.os.Process.myPid());
    });
  }
}
