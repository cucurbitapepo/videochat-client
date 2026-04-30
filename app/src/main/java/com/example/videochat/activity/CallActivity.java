package com.example.videochat.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;


import com.example.videochat.R;
import com.example.videochat.api.ApiClient;
import com.example.videochat.dialog.IncomingCallDialog;
import com.example.videochat.dto.RoomTokenResponse;
import com.example.videochat.livekit.LiveKitClient;
import com.example.videochat.livekit.LiveKitManager;
import com.example.videochat.service.CallForegroundService;
import com.example.videochat.util.JwtUtils;
import com.example.videochat.websocket.StompClientManager;
import com.example.videochat.websocket.WebSocketManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.livekit.android.LiveKit;
import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.room.Room;
import io.livekit.android.room.participant.LocalParticipant;
import io.livekit.android.room.participant.Participant;
import io.livekit.android.room.track.LocalVideoTrack;
import io.livekit.android.room.track.Track;
import io.livekit.android.room.track.TrackException;
import io.livekit.android.room.track.TrackPublication;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import livekit.org.webrtc.EglBase;
import livekit.org.webrtc.RendererCommon;
import okhttp3.WebSocket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallActivity extends AppCompatActivity {
  private static final String TAG = "CallActivity";

  private static final int CAMERA_PERMISSION_CODE = 100;
  private static final int AUDIO_PERMISSION_CODE = 101;

  // UI
  private SurfaceViewRenderer remoteVideoView;
  private SurfaceViewRenderer localVideoView;
  private PreviewView cameraXPreviewView;
  private ImageView remoteAvatarView;
  private ImageView localAvatarView;
  private ImageView cameraOffIndicator;
  private TextView connectionStatus;
  private FloatingActionButton endCallButton;
  private FloatingActionButton microphoneButton;
  private FloatingActionButton cameraButton;
  private FrameLayout remoteMutedContainer;
  private FrameLayout localMutedContainer;
  private TextView remoteUserName;
  private String remoteParticipantName;


  // LiveKit
  private LiveKitClient liveKitClient;
  private CompositeDisposable disposables = new CompositeDisposable();
  private Disposable callDisposable;
  private Disposable roomNotificationDisposable;
  private Observer<Room> roomObserver;
  private Observer<Participant> speakerObserver;

  private boolean isInitialized = false;
  private boolean hasConnectedOnce = false;
  private boolean isMicrophoneEnabled = true;
  private boolean isCameraEnabled = true;
  private boolean isServiceRunning = false;


  // CameraX
  private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
  //  private androidx.camera.core.Preview cameraXPreview;
  private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "CallActivity started");

    WebSocketManager.getInstance(this).getStompClientManager().clearPendingNotifications();

    setContentView(R.layout.activity_call);

    handleIntent(getIntent());
    initViews();

    setupClickListeners();

    updateMicrophoneButton();
    updateCameraButton();

    initializeLiveKit();
//    initializeCameraX();

    checkCameraPermission();
    checkAudioPermission();

    setupCallFlow();
  }

  private void handleIntent(Intent intent) {
    if (intent != null && "CALL_NOTIFICATION".equals(intent.getAction())) {
      String callId = intent.getStringExtra("CALL_ID");
      boolean isCaller = intent.getBooleanExtra("IS_CALLER", false);

      getIntent().putExtra("CALL_ID", callId);
      getIntent().putExtra("IS_CALLER", isCaller);
    }
  }

  private void initViews() {
    remoteVideoView = findViewById(R.id.remote_video_view);
    localVideoView = findViewById(R.id.local_video_view);

    remoteMutedContainer = findViewById(R.id.remote_muted_container);
    localMutedContainer = findViewById(R.id.local_muted_container);
    remoteUserName = findViewById(R.id.remote_user_name);
    remoteParticipantName = getIntent().getStringExtra(IncomingCallDialog.ARG_CALLER_NAME);

    if (remoteParticipantName == null || remoteParticipantName.isEmpty()) {
      remoteParticipantName = "Собеседник";
    }

    if (remoteUserName != null) {
      remoteUserName.setText(remoteParticipantName);
    }
//    cameraXPreviewView = findViewById(R.id.camerax_preview_view);
//    if(cameraXPreviewView != null) {
//      cameraXPreviewView.setVisibility(View.GONE);
//    }
    //TODO: this has to be changed to user profile picture in the future
//    remoteAvatarView = findViewById(R.id.remote_avatar_view);
//    localAvatarView = findViewById(R.id.local_avatar_view);
//    localAvatarView = findViewById(R.id.local_avatar_view);
    //
    cameraOffIndicator = findViewById(R.id.camera_off_indicator);
    connectionStatus = findViewById(R.id.connection_status);
    endCallButton = findViewById(R.id.end_call_button);
    microphoneButton = findViewById(R.id.microphone_button);
    cameraButton = findViewById(R.id.camera_button);

    Long receiverId = getIntent().getLongExtra("RECEIVER_ID", -1);
    if (receiverId != -1) {
      remoteUserName.setText("FIX ME");
    }
  }

  private void setupClickListeners() {
    endCallButton.setOnClickListener(v -> endCall());
    microphoneButton.setOnClickListener(v -> toggleMicrophone());
    cameraButton.setOnClickListener(v -> toggleCamera());
  }

  // ==================== CameraX ====================

//  private void initializeCameraX() {
//    cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//  }

//  private void startCameraXPreview() {
//    cameraProviderFuture.addListener(() -> {
//      try {
//        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//
//        cameraXPreview = new Preview.Builder()
//                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
//                .build();
//
//        cameraXPreview.setSurfaceProvider(cameraXPreviewView.getSurfaceProvider());
//
//        cameraProvider.unbindAll();
//        cameraProvider.bindToLifecycle(
//                this,
//                cameraSelector,
//                cameraXPreview
//        );
//
//        Log.d(TAG, "CameraX Preview started");
//
//        runOnUiThread(() -> {
//          cameraXPreviewView.setVisibility(View.VISIBLE);
//          localVideoView.setVisibility(View.GONE);
//        });
//
//      } catch (ExecutionException | InterruptedException e) {
//        Log.e(TAG, "Failed to start CameraX", e);
//      }
//    }, ContextCompat.getMainExecutor(this));
//  }
//
//  private void stopCameraXPreview() {
//    if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
//      try {
//        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//        cameraProvider.unbindAll();
//        Log.d(TAG, "CameraX Preview stopped");
//      } catch (Exception e) {
//        Log.e(TAG, "Failed to stop CameraX", e);
//      }
//    }
//    runOnUiThread(() -> {
//      cameraXPreviewView.setVisibility(View.GONE);
//      localVideoView.setVisibility(View.VISIBLE);
//    });
//  }

  // ==================== LiveKit ====================

  private void initializeLiveKit() {
    boolean isCaller = getIntent().getBooleanExtra("IS_CALLER", false);
    Log.d(TAG, "initializeLiveKit() called, isCaller=" + isCaller);
    liveKitClient = LiveKitClient.getInstance(this);
    liveKitClient.setRemoteVideoView(remoteVideoView);
    liveKitClient.setLocalVideoView(localVideoView);

    liveKitClient.setAvatarCallback(new LiveKitManager.AvatarCallback() {
      @Override
      public void onAvatarStateChanged(String participantIdentity, boolean show) {
        runOnUiThread(() -> {
          if (show) {
            remoteAvatarView.setVisibility(View.VISIBLE);
//          remoteAvatarView.setImageResource(getAvatarResForUser(participantIdentity));
          } else {
            remoteAvatarView.setVisibility(View.GONE);
          }
        });
      }


      @Override
      public void onLocalAvatarStateChanged(boolean show) {
        runOnUiThread(() -> {
          if (localAvatarView == null) {
            Log.w(TAG, "localAvatarView is null, skipping avatar state change");
            return;
          }
          if (show) {
            localAvatarView.setVisibility(View.VISIBLE);
//          localAvatarView.setImageResource(getAvatarResForUser(participantIdentity));
          } else {
            localAvatarView.setVisibility(View.GONE);
          }
        });
      }
    });

    liveKitClient.setCameraStateCallback((participantIdentity, muted) -> {
      Log.d(TAG, "Remote camera state changed: " + participantIdentity + " muted=" + muted);
      updateRemoteCameraUI(participantIdentity, !muted);
    });

    roomObserver = room -> {
      Log.d(TAG, "Observer works");

      if (room != null) {
        if (!hasConnectedOnce) {
          Log.d(TAG, "First successful connection: " + room.getName());
          hasConnectedOnce = true;
        }
        Log.d(TAG, "Room connected: " + room.getName());
        connectionStatus.setText("Соединение установлено");
//        connectionStatus.setVisibility(View.GONE);

        remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        room.initVideoRenderer(remoteVideoView);
        //
        localVideoView.setMirror(true);
        localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localVideoView.setVisibility(View.VISIBLE);
        localVideoView.setZOrderMediaOverlay(true);
        localVideoView.setEnableHardwareScaler(true);

        room.initVideoRenderer(localVideoView);

        updateLocalCameraUI(isCameraEnabled);
        enableLocalTracks();
        isInitialized = true;
      } else {
        if (!hasConnectedOnce) {
          Log.d(TAG, "Ignoring initial null emission (hasConnectedOnce=false)");
          return;
        }

        Log.d(TAG, "Room is null");
        connectionStatus.setText("Отключено от сервера");
        connectionStatus.setVisibility(View.VISIBLE);

        isInitialized = false;
        showErrorNotification("Соединение с сервером разорвано. Проверьте подключение к интернету.");
      }
    };


    speakerObserver = participant -> {
      if (participant != null) {
        // Можно добавить индикацию активного спикера
      }
    };

    // Подписываемся на изменения состояния комнаты
    liveKitClient.getRoomLiveData().observe(this, roomObserver);

    // Подписываемся на изменения активного спикера
    liveKitClient.getActiveSpeakerLiveData().observe(this, speakerObserver);
  }

  private void enableLocalTracks() {
    if (liveKitClient == null) return;

    // Получаем локального участника
    LocalParticipant localParticipant = liveKitClient.getLocalParticipant();
    if (localParticipant == null) {
      Log.w(TAG, "Local participant not available yet");
      return;
    }

    // Включаем микрофон и камеру
    BuildersKt.launch(
            CoroutineScopeKt.MainScope(),
            Dispatchers.getMain(),
            CoroutineStart.DEFAULT,
            (scope, continuation) -> {
              boolean audioEnabled = false;

              // Пробуем включить аудио, но не крашимся при ошибке
              try {
                localParticipant.setMicrophoneEnabled(true, EMPTY_CONTINUATION);
                audioEnabled = true;
                Log.d(TAG, "Microphone enabled");
              } catch (Exception e) {
                Log.w(TAG, "Microphone unavailable, continuing without audio", e);
              }

              // Всегда пытаемся включить видео
              try {
                Log.d(TAG, "Trying to enable camera, isCameraEnabled=" + isCameraEnabled);
                localParticipant.setCameraEnabled(isCameraEnabled, EMPTY_CONTINUATION);
                Log.d(TAG, "Camera enabled");

                if (isCameraEnabled) {
                  new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    LocalVideoTrack localTrack = liveKitClient.getLocalVideoTrack();
                    if (localTrack != null) {
                      Log.d(TAG, "Local track: enabled=" + localTrack.getEnabled());
                    } else {
                      Log.w(TAG, "localTrack is still NULL after 1s");
                    }
                  }, 1000);
                }
              } catch (TrackException.PublishException e) {
                Log.e(TAG, "Camera failed", e);
                if (isCameraEnabled) {
//                  runOnUiThread(this::startCameraXPreview);
                }
              }

              return kotlin.Unit.INSTANCE;
            });
  }

  // ==================== Call Flow ====================

  private void setupCallFlow() {
    String callId = getIntent().getStringExtra("CALL_ID");
    boolean isCaller = getIntent().getBooleanExtra("IS_CALLER", false);
    Long receiverId = getIntent().getLongExtra("RECEIVER_ID", -1);

    Log.d(TAG, "callId=" + callId + ", isCaller=" + isCaller);

    WebSocketManager.getInstance(this).getNotificationEvent().observe(this, notification -> {
      if (notification == null) return;

      String notifCallId = notification.getData();
      String currentCallId = getIntent().getStringExtra("CALL_ID");

      if (!currentCallId.equals(notifCallId)) {
        Log.d(TAG, "Ignoring notification for different call");
        return;
      }

      String type = notification.getType();
      String status = notification.getMessage();

      Log.d(TAG, "Notification: type=" + type + ", status=" + status);

      if ("CALL_STATUS".equals(type)) {
        if ("rejected".equals(status)) {
          finishCall("Вызов отклонён");
        } else if ("ended".equals(status)) {
          finishCall("Звонок завершён");
        }
      }
    });

    Log.d(TAG, "Connecting to LiveKit: isCaller=" + isCaller);
    connectToLiveKit(callId);

    WebSocketManager.getInstance(this).getConnectionState().observe(this, isConnected -> {
      if (isConnected && isCaller && receiverId != -1) {
        sendCallRequest(callId, receiverId);
      }
    });
  }

  private void connectToLiveKit(String callId) {
    Log.d(TAG, "connectToLiveKit() called");
    ApiClient.getCallApi().getCallToken(callId)
            .enqueue(new Callback<>() {
              @Override
              public void onResponse(Call<RoomTokenResponse> call, Response<RoomTokenResponse> response) {
                Log.d(TAG, "Token response: code=" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                  RoomTokenResponse tokenResponse = response.body();
                  Log.d(TAG, "Connecting to LiveKit: url=" + tokenResponse.getServerUrl());

                  String callId = getIntent().getStringExtra("CALL_ID");
                  if (callId != null) {
                    roomNotificationDisposable = WebSocketManager.getInstance(CallActivity.this)
                            .getStompClientManager()
                            .subscribeToRoomNotifications(callId);
                    Log.d(TAG, "Subscribed to room notifications: " + callId);
                  }

                  liveKitClient.connectToRoom(
                          tokenResponse.getServerUrl(),
                          tokenResponse.getToken()
                  );
                } else {
                  String error = "unknown";
                  try {
                    if (response.errorBody() != null) {
                      error = response.errorBody().string();
                    }
                  } catch (IOException e) { /* ignore */ }
                  Log.e(TAG, "Failed to get token: " + response.code() + " - " + error);
                  finishCall("Не удалось получить токен: " + response.code());
                }
              }

              @Override
              public void onFailure(Call<RoomTokenResponse> call, Throwable t) {
                Log.e(TAG, "Token request failed", t);
                finishCall("Ошибка сети: " + t.getMessage());
              }
            });
  }

  private void sendCallRequest(String callId, Long receiverId) {
    Log.d(TAG, "sendCallRequest() called");
    StompClientManager stompManager = WebSocketManager.getInstance(this).getStompClientManager();

    if (!stompManager.isConnected()) {
      Log.w(TAG, "WebSocket not connected, cannot send call request");
      Toast.makeText(this, "Соединение с сервером не установлено", Toast.LENGTH_SHORT).show();
      return;
    }
    Map<String, Object> callRequest = new HashMap<>();
    callRequest.put("callerId", getCurrentUserId());
    callRequest.put("receiverId", receiverId);
    callRequest.put("callId", callId);
    callRequest.put("status", "request");

    String jsonRequest = new Gson().toJson(callRequest);

    callDisposable = WebSocketManager.getInstance(this).getStompClientManager()
            .send("/app/call/status", jsonRequest)
            .subscribe(
                    () -> Log.d(TAG, "Call request sent successfully"),
                    throwable -> {
                      Log.e(TAG, "Failed to send call request", throwable);
                      Toast.makeText(this,
                              "Не удалось отправить запрос на звонок. Проверьте соединение.",
                              Toast.LENGTH_SHORT).show();
                    }
            );
  }

  // ==================== UI Controls ====================

  private void toggleMicrophone() {
    isMicrophoneEnabled = !isMicrophoneEnabled;
    if (liveKitClient != null) {
      try {
        liveKitClient.toggleMicrophone();
        updateMicrophoneButton();
      } catch (TrackException.PublishException e) {
        Toast.makeText(this, "Ошибка микрофона", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void toggleCamera() {
    isCameraEnabled = !isCameraEnabled;

    if (liveKitClient != null) {
      try {
        liveKitClient.toggleCamera();
        updateCameraButton();
        updateLocalCameraUI(isCameraEnabled);

        if (localAvatarView != null) {
          localAvatarView.setVisibility(isCameraEnabled ? View.GONE : View.VISIBLE);
        }
      } catch (TrackException.PublishException e) {
        Toast.makeText(this, "Ошибка камеры", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void updateMicrophoneButton() {
    if (liveKitClient == null) return;
    int iconRes = isMicrophoneEnabled ? R.drawable.ic_mic_on : R.drawable.ic_mic_off;
    microphoneButton.setImageResource(iconRes);
  }

  private void updateCameraButton() {
    if (liveKitClient == null) return;
    int iconRes = isCameraEnabled ? R.drawable.ic_videocam_on : R.drawable.ic_camera_off;
    cameraButton.setImageResource(iconRes);
    cameraOffIndicator.setVisibility(
            liveKitClient.isCameraEnabled() ? View.GONE : View.VISIBLE);
  }

  private void updateLocalCameraUI(boolean cameraEnabled) {
    runOnUiThread(() -> {
      if (cameraEnabled) {
        localVideoView.setVisibility(View.VISIBLE);
        localVideoView.setZOrderMediaOverlay(true);
        localVideoView.bringToFront();
        localMutedContainer.setVisibility(View.GONE);
      } else {
        localVideoView.setVisibility(View.GONE);
        localMutedContainer.setVisibility(View.VISIBLE);
      }
      Log.d(TAG, "Local camera UI updated: enabled=" + cameraEnabled);
    });
  }

  private void updateRemoteCameraUI(String participantIdentity, boolean cameraEnabled) {
    runOnUiThread(() -> {
      if (cameraEnabled) {
        remoteVideoView.setVisibility(View.VISIBLE);
        if (localVideoView.getVisibility() == View.VISIBLE) {
          localVideoView.setZOrderMediaOverlay(true);
          localVideoView.bringToFront();
        } else {
          localMutedContainer.setVisibility(View.VISIBLE);
        }
        remoteMutedContainer.setVisibility(View.GONE);
      } else {
        remoteVideoView.setVisibility(View.GONE);
        remoteMutedContainer.setVisibility(View.VISIBLE);

        if (participantIdentity != null && !participantIdentity.isEmpty()) {
          remoteUserName.setText(participantIdentity);
        }
      }
      Log.d(TAG, "Remote camera UI updated for " + participantIdentity + ": enabled=" + cameraEnabled);
    });
  }

  // ==================== Permissions ====================

  private void checkCameraPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }
  }

  private void checkAudioPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.RECORD_AUDIO},
              AUDIO_PERMISSION_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_PERMISSION_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Camera permission granted");
      } else {
        Toast.makeText(this, "Разрешение камеры отклонено", Toast.LENGTH_SHORT).show();
      }
    } else if (requestCode == AUDIO_PERMISSION_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Audio permission granted");
        // Можно повторно попробовать включить микрофон
        if (liveKitClient != null) {
          try {
            liveKitClient.toggleMicrophone();
          } catch (Exception e) {
            Log.e(TAG, "Failed to enable microphone", e);
          }
        }
      } else {
        Toast.makeText(this, "Разрешение на запись аудио отклонено", Toast.LENGTH_SHORT).show();
        // Можно отключить микрофон и продолжить только с видео
        isMicrophoneEnabled = false;
      }
    }
  }

  // ==================== Lifecycle ====================

  @Override
  protected void onResume() {
    super.onResume();
    startForegroundService();
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Не останавливаем сервис при переходе в фон, только при завершении звонка
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    // Обрабатываем новый интент, если активность уже запущена
    handleIntent(intent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy() called");

//    stopCameraXPreview();

    if (roomNotificationDisposable != null && !roomNotificationDisposable.isDisposed()) {
      roomNotificationDisposable.dispose();
//      roomNotificationDisposable = null;
      Log.d(TAG, "Unsubscribed from room notifications");
    }

    if (callDisposable != null && !callDisposable.isDisposed()) {
      callDisposable.dispose();
      Log.d(TAG, "callDisposable disposed");
    }

    if (liveKitClient != null) {
      liveKitClient.clearRemoteVideoView();
      liveKitClient.clearLocalVideoView();
      liveKitClient.disconnect();
      Log.d(TAG, "liveKitClient disconnected");
    }


    if (roomObserver != null && liveKitClient != null) {
      liveKitClient.getRoomLiveData().removeObserver(roomObserver);
      Log.d(TAG, "roomObserver removed");
    }
    if (speakerObserver != null && liveKitClient != null) {
      liveKitClient.getActiveSpeakerLiveData().removeObserver(speakerObserver);
      Log.d(TAG, "speakerObserver removed");
    }

    disposables.clear();
    hasConnectedOnce = false;
    LiveKitClient.resetInstance();
    stopForegroundService();
    Log.d(TAG, "onDestroy() completed");
  }

  // ==================== Helpers ====================

  private void startForegroundService() {
    Intent serviceIntent = new Intent(this, CallForegroundService.class);
    serviceIntent.setAction(CallForegroundService.ACTION_START_FOREGROUND);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent);
    } else {
      startService(serviceIntent);
    }

    isServiceRunning = true;
  }

  private void stopForegroundService() {
    if (isServiceRunning) {
      Intent serviceIntent = new Intent(this, CallForegroundService.class);
      serviceIntent.setAction(CallForegroundService.ACTION_STOP_FOREGROUND);
      stopService(serviceIntent);
      isServiceRunning = false;
    }
  }

  private void endCall() {
    if (liveKitClient != null) {
      liveKitClient.disconnect();
    }

    sendCallEndedNotification();

    finish();
  }

  private void sendCallEndedNotification() {
    Map<String, Object> callStatus = new HashMap<>();
    callStatus.put("callId", getIntent().getStringExtra("CALL_ID"));
    callStatus.put("status", "ended");

    String jsonRequest = new Gson().toJson(callStatus);

    WebSocketManager.getInstance(this).getStompClientManager()
            .send("/app/call/status", jsonRequest)
            .subscribe();
  }

  private Long getCurrentUserId() {
    String token = getSharedPreferences("app_data", MODE_PRIVATE)
            .getString("auth_token", null);

    if (token != null) {
      try {
        return JwtUtils.getUserIdFromToken(token);
      } catch (Exception e) {
        Log.e("USER", "Error parsing user ID", e);
      }
    }
    return null;
  }

  private void finishCall(String reason) {
    Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();

    if (liveKitClient != null) {
      liveKitClient.disconnect();
    }

    if (callDisposable != null && !callDisposable.isDisposed()) {
      callDisposable.dispose();
    }

    finish();
  }

  private void showErrorNotification(String message) {
    if (isFinishing()) return;

    runOnUiThread(() -> {
      new AlertDialog.Builder(this)
              .setTitle("Ошибка соединения")
              .setMessage(message)
              .setPositiveButton("Завершить звонок", (dialog, which) -> {
                finishCall("Соединение разорвано");
              })
              .setNegativeButton("Попробовать снова", (dialog, which) -> {
                String callId = getIntent().getStringExtra("CALL_ID");
                if (callId != null) {
                  connectToLiveKit(callId);
                }
              })
              .setCancelable(false)
              .show();
    });
  }

  private static final kotlin.coroutines.Continuation<Object> EMPTY_CONTINUATION =
          new kotlin.coroutines.Continuation<>() {
            @Override
            public kotlin.coroutines.CoroutineContext getContext() {
              return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object o) {
              // Ничего не делаем
            }
          };
}
